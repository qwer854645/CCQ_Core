package ccq.core.compat.storage;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class StorageBridgeBlock extends BaseEntityBlock {
    public static final MapCodec<StorageBridgeBlock> CODEC = simpleCodec(StorageBridgeBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public StorageBridgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Front faces the player, same convention as furnaces / FS controllers.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageBridgeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, StorageBridgeBlocks.STORAGE_BRIDGE_BE.get(), StorageBridgeBlockEntity::serverTick);
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
        if (!(level.getBlockEntity(pos) instanceof StorageBridgeBlockEntity bridge)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() instanceof LinkingToolItem) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            return handleLinkingTool(stack, level, pos, player, bridge)
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.FAIL;
        }

        if (stack.isEmpty() && !level.isClientSide) {
            player.displayClientMessage(bridge.describeLinks(), true);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean handleLinkingTool(
            ItemStack stack,
            Level level,
            BlockPos bridgePos,
            Player player,
            StorageBridgeBlockEntity bridge
    ) {
        BlockPos controllerPos = FSAttachments.CONTROLLER.get(stack);
        if (controllerPos == null || controllerPos.equals(BlockPos.ZERO)) {
            player.displayClientMessage(Component.translatable("message.ccq_core.storage_bridge.no_controller"), true);
            return false;
        }

        if (!(level.getBlockEntity(controllerPos) instanceof StorageControllerTile<?> controller)) {
            player.displayClientMessage(Component.translatable("message.ccq_core.storage_bridge.bad_controller"), true);
            return false;
        }

        LinkingToolItem.ActionMode mode = LinkingToolItem.getActionMode(stack);
        double range = controller.getStorageMultiplier();
        AABB area = new AABB(controllerPos).inflate(range);
        if (!area.contains(Vec3.atCenterOf(bridgePos))) {
            player.displayClientMessage(Component.translatable("message.ccq_core.storage_bridge.out_of_range"), true);
            return false;
        }

        var connected = controller.getConnectedDrawers().getConnectedDrawers();
        long packed = bridgePos.asLong();

        if (mode == LinkingToolItem.ActionMode.REMOVE) {
            connected.removeIf(value -> value == packed);
            bridge.clearFsController();
            controller.getConnectedDrawers().rebuild();
            controller.markForUpdate();
            player.displayClientMessage(Component.translatable("message.ccq_core.storage_bridge.unlinked"), true);
            return true;
        }

        bridge.setFsController(controllerPos);
        if (!connected.contains(packed)) {
            connected.add(packed);
        }
        controller.getConnectedDrawers().rebuild();
        if (controller.inventoryHandler != null) {
            controller.inventoryHandler.invalidateSlots();
        }
        controller.markForUpdate();
        player.displayClientMessage(Component.translatable("message.ccq_core.storage_bridge.linked"), true);
        return true;
    }
}
