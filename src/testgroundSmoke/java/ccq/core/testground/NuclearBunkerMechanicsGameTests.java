package ccq.core.testground;

import java.util.*;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_nuclear_bunker")
@PrefixGameTestTemplate(false)
public class NuclearBunkerMechanicsGameTests {
    @GameTest(template="empty",batch="bunker_loot",timeoutTicks=100)
    public static void observationLootUsesValidComponentsAndVariableGunChance(GameTestHelper helper) {
        var level=helper.getLevel();
        var key=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("ccq_testground:chests/engineering/bunker_observation"));
        var table=level.getServer().reloadableRegistries().getLootTable(key);
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.ZERO).create(LootContextParamSets.CHEST);
        int gunRolls=0;
        var seeds=net.minecraft.util.RandomSource.create(92361);
        for(int seed=1;seed<=400;seed++) {
            var items=table.getRandomItems(params,seeds.nextLong());int guns=0,ammo=0;
            for(var item:items) {
                var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
                var data=item.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                if(id.equals("tacz:modern_kinetic_gun")) {
                    guns++;helper.assertTrue(Set.of("create_armorer:sniper_semi_clockwork","create_armorer:rifle_assult_crane","create_armorer:pistol_auto_stress").contains(data.getString("GunId")),"Valid Create Armorer gun id");
                }
                if(id.equals("tacz:ammo")){ammo++;helper.assertTrue(Set.of("create_armorer:gas_pistol_ammo","create_armorer:slap","create_armorer:rbapb").contains(data.getString("AmmoId")),"Valid matching ammo id survives loot serialization");}
            }
            helper.assertTrue(guns<=1&&ammo==3,"At most one gun and matching ammunition families per station");
            gunRolls+=guns;
        }
        helper.assertTrue(gunRolls>130&&gunRolls<230,"Seeded loot remains probabilistic near 45%: "+gunRolls+"/400");
        org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Bunker gun loot: {} / 400 observation stations",gunRolls);helper.succeed();
    }
}
