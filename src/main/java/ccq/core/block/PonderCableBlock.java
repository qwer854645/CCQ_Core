package ccq.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PonderCableBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape NORTH_ARM = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SOUTH_ARM = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape EAST_ARM = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape WEST_ARM = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape UP_ARM = Block.box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape DOWN_ARM = Block.box(6, 0, 6, 10, 6, 10);

    public PonderCableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, NORTH_ARM);
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, SOUTH_ARM);
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, EAST_ARM);
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, WEST_ARM);
        }
        if (state.getValue(UP)) {
            shape = Shapes.or(shape, UP_ARM);
        }
        if (state.getValue(DOWN)) {
            shape = Shapes.or(shape, DOWN_ARM);
        }
        return shape;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    public static BlockState withConnections(BlockState state, Iterable<Direction> connections) {
        BlockState result = state
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false);
        for (Direction direction : connections) {
            result = switch (direction) {
                case NORTH -> result.setValue(NORTH, true);
                case SOUTH -> result.setValue(SOUTH, true);
                case EAST -> result.setValue(EAST, true);
                case WEST -> result.setValue(WEST, true);
                case UP -> result.setValue(UP, true);
                case DOWN -> result.setValue(DOWN, true);
            };
        }
        return result;
    }
}
