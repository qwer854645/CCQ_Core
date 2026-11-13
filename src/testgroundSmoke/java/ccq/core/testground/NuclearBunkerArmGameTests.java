package ccq.core.testground;

import java.util.*;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_bunker_mechanics")
@PrefixGameTestTemplate(false)
public class NuclearBunkerArmGameTests {
    @GameTest(template="empty",batch="bunker_vault_automation",timeoutTicks=200)
    public static void basementArmsRemainOffWithValidCogwheelPowerInterfaces(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput();
        var cases=new ArrayList<Map.Entry<BlockPos,Rotation>>();
        for(var rotation:Rotation.values()) {
            var requested=new BlockPos(86000+rotation.ordinal()*1024,180,86000);var origin=requested.below(8);
            EngineeringBaseCommands.placeBunker(source,requested,rotation,Mirror.NONE,1,0);cases.add(Map.entry(origin,rotation));
        }
        helper.runAfterDelay(40,()->{
            for(var fixture:cases) {
                var origin=fixture.getKey();var rotation=fixture.getValue();
                var clutch=level.getBlockState(origin.offset(new BlockPos(64,6,19).rotate(rotation)));
                helper.assertTrue(clutch.getValue(BlockStateProperties.POWERED),"Basement clutch is authored off independently of upper machines");
                for(int x:new int[]{36,41,54,59}) {
                    var pos=origin.offset(new BlockPos(x,2,36).rotate(rotation));
                    var arm=(ArmBlockEntity)level.getBlockEntity(pos);
                    helper.assertTrue(arm!=null&&arm.getSpeed()==0,"Arm remains off by default; no activation performed");
                    var cogPos=origin.offset(new BlockPos(x,2,35).rotate(rotation));var cog=level.getBlockState(cogPos);
                    helper.assertTrue(ICogWheel.isSmallCog(cog)&&ICogWheel.isSmallCog(level.getBlockState(pos)),"Real same-axis cog meshes with the arm base");
                    var gearboxPos=origin.offset(new BlockPos(x,1,35).rotate(rotation));var gearbox=level.getBlockState(gearboxPos);
                    helper.assertTrue(cog.getBlock() instanceof IRotate c&&c.hasShaftTowards(level,cogPos,cog,Direction.DOWN)&&gearbox.getBlock() instanceof IRotate g&&g.hasShaftTowards(level,gearboxPos,gearbox,Direction.UP),"Cog receives power from a real upward gearbox port");
                }
            }
            helper.succeed();
        });
    }
}
