package ccq.core.compat.cmr;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlock;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LiquidSnowmanCoolerBlock extends SnowmanCoolerBlock implements IWrenchable {
    private static final ResourceLocation GOGGLES = ResourceLocation.fromNamespaceAndPath("create", "goggles");

    public static final MapCodec<LiquidSnowmanCoolerBlock> CODEC = simpleCodec(LiquidSnowmanCoolerBlock::new);

    public LiquidSnowmanCoolerBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BlockEntityType<? extends SnowmanCoolerBlockEntity> getBlockEntityType() {
        return (BlockEntityType<? extends SnowmanCoolerBlockEntity>) CcqCompatBlocks.LIQUID_SNOWMAN_COOLER_BE.get();
    }

    @Override
    public Class<SnowmanCoolerBlockEntity> getBlockEntityClass() {
        return SnowmanCoolerBlockEntity.class;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (isGoggles(stack)) {
            return onBlockEntityUseItemOn(level, pos, cooler -> {
                if (cooler.goggles) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                cooler.goggles = true;
                cooler.updateBlockState();
                return ItemInteractionResult.SUCCESS;
            });
        }

        SnowmanCoolerBlockEntity cooler = getBlockEntity(level, pos);
        if (cooler != null && cooler.stockKeeper) {
            StockTickerBlockEntity stockTicker = SnowmanCoolerBlockEntity.getStockTicker(level, pos);
            if (stockTicker != null) {
                StockTickerInteractionHandler.interactWithLogisticsManagerAt(player, level, stockTicker.getBlockPos());
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty()) {
            return onBlockEntityUseItemOn(level, pos, activeCooler -> {
                if (!activeCooler.goggles) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                activeCooler.goggles = false;
                activeCooler.updateBlockState();
                return ItemInteractionResult.SUCCESS;
            });
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos.above());
        if (blockEntity instanceof BasinBlockEntity basin) {
            basin.notifyChangeOfContents();
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AllShapes.HEATER_BLOCK_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context == CollisionContext.empty()) {
            return AllShapes.HEATER_BLOCK_SPECIAL_COLLISION_SHAPE;
        }
        return getShape(state, level, pos, context);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected MapCodec<? extends LiquidSnowmanCoolerBlock> codec() {
        return CODEC;
    }

    private static boolean isGoggles(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getOptional(GOGGLES).map(stack::is).orElse(false);
    }
}
