package ccq.core.testground;

import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_ser_breech")
@PrefixGameTestTemplate(false)
public class SerBreechReloadGameTests {
    @GameTest(template="empty",timeoutTicks=700)
    public static void savedSerDockingCircuitAfterWorldReload(GameTestHelper h) {
        var level=h.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        for(int cx=-4361>>4;cx<=-4266>>4;cx++)for(int cz=283>>4;cz<=378>>4;cz++)level.setChunkForced(cx,cz,true);
        var lever=new BlockPos(-4283,135,354);var lift=new BlockPos(-4291,145,356);
        var points=new BlockPos[]{lever,new BlockPos(-4283,135,356),new BlockPos(-4292,145,355),new BlockPos(-4291,145,355),lift};
        for(int delay:new int[]{75,110,350,400,615})h.runAfterDelay(delay,()->{
            for(var p:points){var b=level.getBlockEntity(p);org.slf4j.LoggerFactory.getLogger("ccq_ser_breech").info("SERSNAPSHOT tick={} pos={} state={} nbt={}",delay,p,level.getBlockState(p),b==null?null:b.saveWithFullMetadata(level.registryAccess()));}
        });
        h.runAfterDelay(80,()->{
            var p=(MechanicalPistonBlockEntity)level.getBlockEntity(lift);
            h.assertTrue(p!=null,"Saved SER piston loads from copied region");
            var state=level.getBlockState(lever);if(!state.getValue(BlockStateProperties.POWERED))((LeverBlock)state.getBlock()).pull(state,level,lever,null);
        });
        h.runAfterDelay(360,()->{
            var p=(MechanicalPistonBlockEntity)level.getBlockEntity(lift);
            h.assertTrue(p.offset>2.99,"Saved SER lift docks: offset="+p.offset+" speed="+p.getSpeed()+" error="+p.getLastAssemblyException());
            var state=level.getBlockState(lever);((LeverBlock)state.getBlock()).pull(state,level,lever,null);
        });
        h.runAfterDelay(620,()->{
            var p=(MechanicalPistonBlockEntity)level.getBlockEntity(lift);
            h.assertTrue(p.offset<.01,"Saved SER lift retracts: offset="+p.offset+" speed="+p.getSpeed());
            org.slf4j.LoggerFactory.getLogger("ccq_ser_breech").info("SER saved circuit validation: actual saved docking lever reverses and retracts actual saved bridge after reload");
            for(int cx=-4361>>4;cx<=-4266>>4;cx++)for(int cz=283>>4;cz<=378>>4;cz++)level.setChunkForced(cx,cz,false);
            h.succeed();
        });
    }
}
