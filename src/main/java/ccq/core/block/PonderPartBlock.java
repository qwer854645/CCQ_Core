package ccq.core.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class PonderPartBlock extends PonderCableBlock {
    public static final DirectionProperty PART_FACE = DirectionProperty.create("part_face", Direction.values());

    public PonderPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART_FACE, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART_FACE);
    }
}
