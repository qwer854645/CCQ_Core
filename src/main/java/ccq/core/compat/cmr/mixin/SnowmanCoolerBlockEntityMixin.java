package ccq.core.compat.cmr.mixin;

import ccq.core.compat.cmr.CmrGoggleTooltips;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(SnowmanCoolerBlockEntity.class)
public abstract class SnowmanCoolerBlockEntityMixin implements IHaveGoggleInformation {
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return CmrGoggleTooltips.appendSnowmanCooler(
                (SnowmanCoolerBlockEntity) (Object) this,
                tooltip,
                isPlayerSneaking
        );
    }
}
