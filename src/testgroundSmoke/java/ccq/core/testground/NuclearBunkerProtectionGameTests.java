package ccq.core.testground;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.*;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_bunker_mechanics")
@PrefixGameTestTemplate(false)
public class NuclearBunkerProtectionGameTests {
    @GameTest(template="empty",batch="bunker_vault",timeoutTicks=600)
    public static void vaultDoorOperatesWithoutMovingEntranceGates(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput();
        var cases=new ArrayList<NuclearBunkerTwinDoorGameTests.Door>();var forced=new HashSet<net.minecraft.world.level.ChunkPos>();
        for(var rotation:Rotation.values()) {
            var requested=new BlockPos(65000+rotation.ordinal()*1024,180,65000);var origin=requested.below(8);
            EngineeringBaseCommands.placeBunker(source,requested,rotation,Mirror.NONE,1,0);
            var bounds=net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(origin,origin.offset(new BlockPos(95,31,95).rotate(rotation)));
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++) {
                level.setChunkForced(x,z,true);forced.add(new net.minecraft.world.level.ChunkPos(x,z));
            }
            cases.add(new NuclearBunkerTwinDoorGameTests.Door(origin,rotation,51,2,
                (MechanicalPistonBlockEntity)level.getBlockEntity(origin.offset(new BlockPos(52,6,51).rotate(rotation))),
                (HandCrankBlockEntity)level.getBlockEntity(origin.offset(new BlockPos(51,3,53).rotate(rotation))),
                (HandCrankBlockEntity)level.getBlockEntity(origin.offset(new BlockPos(51,3,49).rotate(rotation)))));
        }
        NuclearBunkerTwinDoorGameTests.drive(helper,cases,5,205,true,true);
        helper.runAfterDelay(210,()->{
            NuclearBunkerTwinDoorGameTests.assertDoor(helper,cases,true);
            for(var d:cases)for(int z:new int[]{79,89}) {
                var entrance=(MechanicalPistonBlockEntity)level.getBlockEntity(d.at(48,18,z));
                helper.assertTrue(entrance.offset<.01f,"Vault drive cannot move either entrance gate");
            }
        });
        NuclearBunkerTwinDoorGameTests.drive(helper,cases,220,420,false,false);
        helper.runAfterDelay(430,()->{
            try {NuclearBunkerTwinDoorGameTests.assertDoor(helper,cases,false);helper.succeed();}
            finally {forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }

}
