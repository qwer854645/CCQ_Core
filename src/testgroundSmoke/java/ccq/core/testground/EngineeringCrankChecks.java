package ccq.core.testground;

import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class EngineeringCrankChecks {
    static void check(GameTestHelper helper,ServerLevel level,BlockPos pos,BlockState state) {
        if(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("create:hand_crank"))return;
        var direction=state.getValue(BlockStateProperties.FACING).getOpposite();
        var target=pos.relative(direction);var neighbor=level.getBlockState(target);
        helper.assertTrue(neighbor.getBlock() instanceof IRotate rotate&&rotate.hasShaftTowards(level,target,neighbor,direction.getOpposite()),"Crank uses a real machine shaft face at "+pos);
        if(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(neighbor.getBlock()).toString().equals("create:shaft")) {
            var further=target.relative(direction);var machine=level.getBlockState(further);
            boolean nextShaft=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(machine.getBlock()).toString().equals("create:shaft");
            helper.assertTrue(nextShaft||!(machine.getBlock() instanceof IRotate rotate)||!rotate.hasShaftTowards(level,further,machine,direction.getOpposite()),"No single redundant shaft adapter remains at "+pos);
        }
    }
}
