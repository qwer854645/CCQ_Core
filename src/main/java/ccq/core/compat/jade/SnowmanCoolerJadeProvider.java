package ccq.core.compat.jade;

import ccq.core.compat.cmr.LiquidSnowmanCoolerBlock;
import ccq.core.compat.cmr.LiquidSnowmanCoolerBlockEntity;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlock;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IElementHelper;

public enum SnowmanCoolerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ccq_core", "snowman_cooler");

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    @Override
    public void appendServerData(net.minecraft.nbt.CompoundTag tag, BlockAccessor accessor) {
        if (isLiquidCooler(accessor) || !(accessor.getBlockEntity() instanceof SnowmanCoolerBlockEntity cooler)) {
            return;
        }

        tag.putInt("BurnTime", cooler.getRemainingBurnTime());
        tag.putBoolean("Creative", cooler.isCreative());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (isLiquidCooler(accessor) || !(accessor.getBlockEntity() instanceof SnowmanCoolerBlockEntity cooler)) {
            return;
        }

        var serverData = accessor.getServerData();
        int burnTime = serverData.contains("BurnTime") ? serverData.getInt("BurnTime") : cooler.getRemainingBurnTime();
        boolean creative = serverData.getBoolean("Creative") || cooler.isCreative();
        if (burnTime <= 0 && !creative) {
            return;
        }

        SnowmanCoolerBlock.HeatLevel heat = cooler.getHeatLevelFromBlock();
        if (heat != SnowmanCoolerBlock.HeatLevel.IDLE) {
            tooltip.add(Component.translatable("ccq_core.jade.snowman_cooler.heat." + heat.getSerializedName()));
        }

        if (burnTime > 0) {
            tooltip.add(Component.translatable("ccq_core.jade.snowman_cooler.burn_time", burnTime));
        }
    }

    private static boolean isLiquidCooler(BlockAccessor accessor) {
        return accessor.getBlock() instanceof LiquidSnowmanCoolerBlock
                || accessor.getBlockEntity() instanceof LiquidSnowmanCoolerBlockEntity;
    }
}
