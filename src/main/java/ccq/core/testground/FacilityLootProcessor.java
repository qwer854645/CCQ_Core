package ccq.core.testground;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Roll real loot tables into mod containers' native serialized inventories. */
public final class FacilityLootProcessor extends StructureProcessor {
    static final FacilityLootProcessor INSTANCE=new FacilityLootProcessor();
    public static final MapCodec<FacilityLootProcessor> CODEC=MapCodec.unit(()->INSTANCE);
    private FacilityLootProcessor(){}
    @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader world,BlockPos origin,BlockPos pivot,
            StructureTemplate.StructureBlockInfo original,StructureTemplate.StructureBlockInfo current,StructurePlaceSettings settings){
        if(current.nbt()==null||!(world instanceof WorldGenLevel generation))return current;
        var tag=current.nbt().copy();
        String table=tag.getString("ccq_facility_loot");
        boolean fuel=tag.contains("ccq_facility_fuel");
        if(table.isEmpty()&&!fuel)return current;
        var random=RandomSource.create(generation.getSeed()^current.pos().asLong()^0x4343514c4f4f54L);
        if(!table.isEmpty()){
            String inventoryKey=tag.getString("id").equals("fxntstorage:storage_box_entity")?"Items":"Inventory";
            var inventory=tag.getCompound(inventoryKey);
            int capacity=inventory.getInt("Size");
            if(capacity>0&&inventory.getList("Items",Tag.TAG_COMPOUND).isEmpty()){
                var serverLevel=generation.getLevel();
                var params=new LootParams.Builder(serverLevel).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(current.pos()))
                        .create(LootContextParamSets.CHEST);
                var key=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse(table));
                var loot=serverLevel.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params,random);
                var items=new ListTag();
                var slots=new java.util.ArrayList<Integer>();
                for(int i=0;i<capacity;i++)slots.add(i);
                int stored=0;
                for(var stack:loot){
                    int remaining=stack.getCount();
                    while(remaining>0&&!slots.isEmpty()){
                        int count=Math.min(remaining,stack.getMaxStackSize());
                        var item=(CompoundTag)stack.copyWithCount(count).save(serverLevel.registryAccess());
                        item.putInt("Slot",slots.remove(random.nextInt(slots.size())));
                        items.add(item);stored+=count;remaining-=count;
                    }
                }
                inventory.put("Items",items);tag.put(inventoryKey,inventory);
                if(inventoryKey.equals("Items")){
                    tag.putInt("StoredAmount",stored);
                    tag.putFloat("PercentageUsed",100F*items.size()/capacity);
                }
            }
            tag.remove("ccq_facility_loot");
        }
        if(fuel){
            var tanks=tag.getList("Tanks",Tag.TAG_COMPOUND);
            if(!tanks.isEmpty()&&tanks.getCompound(0).getCompound("TankContent").isEmpty()){
                boolean salvage=tag.getString("ccq_facility_fuel").equals("salvage");
                var content=new CompoundTag();
                content.putString("id",random.nextInt(4)==0?"createdieselgenerators:gasoline":"createdieselgenerators:diesel");
                content.putInt("amount",salvage?(random.nextBoolean()?250:500):(random.nextBoolean()?500:1000));
                tanks.getCompound(0).put("TankContent",content);
            }
            tag.remove("ccq_facility_fuel");
        }
        return new StructureTemplate.StructureBlockInfo(current.pos(),current.state(),tag);
    }
    @Override protected StructureProcessorType<?> getType(){return EngineeringStructures.FACILITY_LOOT_PROCESSOR.get();}
}
