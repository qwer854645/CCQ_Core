package ccq.core.testground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

record PortalFrame(BlockPos origin, Direction.Axis axis, int width, int height) {
    static final int MAX_SIZE = 21;
    private static final ResourceLocation FRAME = ResourceLocation.parse("create:industrial_iron_block");

    static boolean isFrame(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(FRAME);
    }

    static boolean isInterior(BlockState state) {
        return state.isAir() || state.is(TestGroundModule.PORTAL.get());
    }

    Direction right() { return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH; }
    BlockPos at(int x, int y) { return origin.relative(right(), x).above(y); }

    static PortalFrame find(LevelAccessor level, BlockPos inside, Direction.Axis axis) {
        if (!isInterior(level.getBlockState(inside))) return null;
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos bottom = inside;
        for (int i = 0; i < MAX_SIZE && isInterior(level.getBlockState(bottom.below())); i++) bottom = bottom.below();
        if (!isFrame(level.getBlockState(bottom.below()))) return null;
        for (int i = 0; i < MAX_SIZE && isInterior(level.getBlockState(bottom.relative(right.getOpposite()))); i++)
            bottom = bottom.relative(right.getOpposite());
        if (!isFrame(level.getBlockState(bottom.relative(right.getOpposite())))) return null;
        int width = 0;
        while (width <= MAX_SIZE && isInterior(level.getBlockState(bottom.relative(right, width)))) width++;
        if (width < 2 || width > MAX_SIZE || !isFrame(level.getBlockState(bottom.relative(right, width)))) return null;
        for (int height = 0; height <= MAX_SIZE; height++) {
            boolean top = isFrame(level.getBlockState(bottom.above(height)));
            for (int x = -1; x <= width; x++) {
                BlockState state = level.getBlockState(bottom.relative(right, x).above(height));
                if (x == -1 || x == width || top) {
                    if (!isFrame(state)) return null;
                } else if (!isInterior(state)) return null;
            }
            if (top) {
                if (height < 3) return null;
                PortalFrame frame = new PortalFrame(bottom.immutable(), axis, width, height);
                for (int x = -1; x <= width; x++) if (!isFrame(level.getBlockState(frame.at(x, -1)))) return null;
                return frame;
            }
        }
        return null;
    }

    void fill(LevelAccessor level) {
        BlockState state = TestGroundModule.PORTAL.get().defaultBlockState().setValue(EngineeringPortalBlock.AXIS, axis);
        // Publish the whole surface before validating it. Partial surfaces are invalid once lit.
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) level.setBlock(at(x, y), state, 18);
    }

    boolean isComplete(LevelAccessor level) {
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            BlockState state = level.getBlockState(at(x, y));
            if (!state.is(TestGroundModule.PORTAL.get()) || state.getValue(EngineeringPortalBlock.AXIS) != axis) return false;
        }
        return true;
    }
}
