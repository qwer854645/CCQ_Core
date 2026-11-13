package ccq.core.testground;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class EngineeringPortalBlock extends Block implements net.minecraft.world.level.block.Portal {
    public static final MapCodec<EngineeringPortalBlock> CODEC = simpleCodec(EngineeringPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z = Block.box(6, 0, 0, 10, 16, 16);
    public EngineeringPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
    }
    @Override protected BlockState updateShape(BlockState state, Direction side, BlockState other,
            LevelAccessor level, BlockPos pos, BlockPos neighbor) {
        level.scheduleTick(pos, this, 1);
        return state;
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PortalFrame frame = PortalFrame.find(level, pos, state.getValue(AXIS));
        if (frame == null || !frame.isComplete(level)) {
            collapse(level, pos, state.getValue(AXIS));
        } else if (pos.equals(frame.origin())) {
            // Corners are diagonal to the surface and do not deliver direct neighbor updates.
            level.scheduleTick(pos, this, 20);
        }
    }
    private void collapse(ServerLevel level, BlockPos start, Direction.Axis axis) {
        var pending = new java.util.ArrayDeque<BlockPos>();
        var seen = new java.util.HashSet<BlockPos>();
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        pending.add(start);
        while (!pending.isEmpty() && seen.size() < 4 * PortalFrame.MAX_SIZE * PortalFrame.MAX_SIZE) {
            BlockPos pos = pending.removeFirst();
            if (!seen.add(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.is(this) || state.getValue(AXIS) != axis) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            for (Direction dir : new Direction[]{Direction.UP, Direction.DOWN, along, along.getOpposite()}) {
                BlockPos next = pos.relative(dir);
                if (!seen.contains(next)) pending.add(next);
            }
        }
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.END_ROD,
                pos.getX()+random.nextDouble(), pos.getY()+random.nextDouble(), pos.getZ()+random.nextDouble(), 0, .015, 0);
    }

    void contact(Entity entity, BlockPos pos) {
        if (entity.canUsePortal(false) && !entity.isVehicle()
                && !(entity instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity))
            entity.setAsInsidePortal(this, pos);
    }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        contact(entity, pos);
    }

    @Override public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return ((net.minecraft.world.level.block.Portal)Blocks.NETHER_PORTAL).getPortalTransitionTime(level, entity);
    }

    @Override public net.minecraft.world.level.portal.DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return PortalTravel.destination(level, pos, entity);
    }

    @Override public net.minecraft.world.level.block.Portal.Transition getLocalTransition() {
        return net.minecraft.world.level.block.Portal.Transition.CONFUSION;
    }
}
