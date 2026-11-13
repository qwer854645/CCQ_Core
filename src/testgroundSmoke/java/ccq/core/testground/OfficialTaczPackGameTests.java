package ccq.core.testground;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import net.minecraft.resources.ResourceLocation;
@GameTestHolder("ccq_official_tacz") @PrefixGameTestTemplate(false)
public class OfficialTaczPackGameTests {
 @GameTest(template="empty",timeoutTicks=100)
 public static void officialLoaderReadsPatchedPack(GameTestHelper h)throws Exception {
  var api=Class.forName("com.tacz.guns.api.TimelessAPI");
  for(var pair:new String[][]{{"getCommonAmmoIndex","tacz:12g"}}) {
   var result=(java.util.Optional<?>)api.getMethod(pair[0],ResourceLocation.class).invoke(null,ResourceLocation.parse(pair[1]));
   h.assertTrue(result.isPresent(),"Official loader exposes patched "+pair[0]+" "+pair[1]);
  }
  h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("tacz:ammo/12g")).isPresent(),"Official vanilla recipe manager exposes 12g recipe");
  h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("applied_armorer:gun/moritz_rifle_ar77")).isPresent(),"Applied Armorer rifle recipe is registered");
  for(var ns:new String[]{"applied_armorer","create_armorer","ars_armorer"}) {
   long count=h.getLevel().getRecipeManager().getRecipes().stream().filter(v->v.id().getNamespace().equals(ns)).count();
   h.assertTrue(count>0,"Actual registered recipes for "+ns+" count="+count);
   org.slf4j.LoggerFactory.getLogger("ccq_tacz").info("Official gunpack registered recipes {}={}",ns,count);
  }
  var removed=(java.util.Optional<?>)api.getMethod("getCommonGunIndex",ResourceLocation.class).invoke(null,ResourceLocation.parse("tacz:ak47"));
  h.assertTrue(removed.isEmpty(),"Default guns stay removed after official loading");
  var blocks=(java.util.Set<?>)api.getMethod("getAllCommonBlockIndex").invoke(null);
  h.assertTrue(blocks.stream().anyMatch(v->((java.util.Map.Entry<?,?>)v).getKey().toString().contains("applied_armorer")),"Applied Armorer block index loads");
  var block=((java.util.Map.Entry<?,?>)blocks.stream().filter(v->((java.util.Map.Entry<?,?>)v).getKey().toString().contains("applied_armorer")).findFirst().orElseThrow()).getValue();
  var data=block.getClass().getMethod("getData").invoke(block);var tabs=(java.util.List<?>)data.getClass().getMethod("getTabs").invoke(data);
  h.assertTrue(tabs.size()==13,"All thirteen patched workbench tabs load");
  for(var tab:tabs){var icon=(net.minecraft.world.item.ItemStack)tab.getClass().getMethod("icon").invoke(tab);h.assertTrue(!icon.isEmpty()&&icon.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA),"Workbench icon loads with custom_data");}
  for(var ns:new String[]{"applied_armorer","create_armorer","ars_armorer"}) {
   var entry=(java.util.Map.Entry<?,?>)blocks.stream().filter(v->((java.util.Map.Entry<?,?>)v).getKey().toString().startsWith(ns+":")).findFirst().orElseThrow();
   var terminal=entry.getValue();var filter=terminal.getClass().getMethod("getFilter").invoke(terminal);
   int accepted=0;
   for(var recipe:h.getLevel().getRecipeManager().getRecipes())if(recipe.id().getNamespace().equals(ns)&&
      (Boolean)filter.getClass().getMethod("contains",ResourceLocation.class).invoke(filter,recipe.id()))accepted++;
   h.assertTrue(accepted>0,"Workbench actually accepts registered recipes: "+ns);
   org.slf4j.LoggerFactory.getLogger("ccq_tacz").info("Workbench filter accepted recipes {}={}",ns,accepted);
  }
  org.slf4j.LoggerFactory.getLogger("ccq_tacz").info("Official TACZ 1.2 patched-pack loader check passed: 12g ammo, recipe and Applied Armorer block");h.succeed();
 }
}
