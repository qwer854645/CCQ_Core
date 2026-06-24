package ccq.core.compat.cmr;

import com.simibubi.create.foundation.utility.CreateLang;
import fr.iglee42.cmr.blockspout.BlockSpoutBlockEntity;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlock;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class CmrGoggleTooltips {
    private CmrGoggleTooltips() {
    }

    public static boolean appendSnowmanCooler(
            SnowmanCoolerBlockEntity cooler,
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        if (!CmrFixes.isEnabled()) {
            return false;
        }

        SnowmanCoolerBlock.HeatLevel heat = cooler.getHeatLevelFromBlock();
        int burnTime = cooler.getRemainingBurnTime();
        boolean creative = cooler.isCreative();

        if (heat == SnowmanCoolerBlock.HeatLevel.IDLE && burnTime <= 0 && !creative) {
            CreateLang.translate("ccq_core.goggles.snowman_cooler.idle")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            return true;
        }

        CreateLang.translate("ccq_core.goggles.snowman_cooler.status")
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        CreateLang.translate("ccq_core.goggles.snowman_cooler.heat." + heat.getSerializedName())
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);

        if (creative) {
            CreateLang.translate("ccq_core.goggles.snowman_cooler.creative")
                    .style(ChatFormatting.LIGHT_PURPLE)
                    .forGoggles(tooltip, 1);
        } else if (burnTime > 0) {
            CreateLang.number(burnTime)
                    .add(CreateLang.translate("generic.unit.ticks"))
                    .style(ChatFormatting.GOLD)
                    .forGoggles(tooltip, 1);
        }

        SnowmanCoolerBlockEntity.FuelType fuel = cooler.getActiveFuel();
        if (fuel != SnowmanCoolerBlockEntity.FuelType.NONE) {
            CreateLang.translate("ccq_core.goggles.snowman_cooler.fuel." + fuel.name().toLowerCase())
                    .style(ChatFormatting.GREEN)
                    .forGoggles(tooltip, 1);
        }

        return true;
    }

    public static boolean appendBlockSpout(BlockSpoutBlockEntity spout, List<Component> tooltip) {
        if (!CmrFixes.isEnabled()) {
            return false;
        }
        if (spout.processingTicks <= 0 && spout.recipe == null) {
            return false;
        }

        CreateLang.translate("ccq_core.goggles.block_spout.processing")
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        if (spout.processingTicks > 0) {
            CreateLang.number(spout.processingTicks)
                    .add(CreateLang.text(" / "))
                    .add(CreateLang.number(BlockSpoutBlockEntity.TIME))
                    .add(CreateLang.translate("generic.unit.ticks"))
                    .style(ChatFormatting.GOLD)
                    .forGoggles(tooltip, 1);
        }

        return true;
    }
}
