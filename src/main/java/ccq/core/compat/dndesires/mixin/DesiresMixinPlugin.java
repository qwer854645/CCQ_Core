package ccq.core.compat.dndesires.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public final class DesiresMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }

    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var mods = LoadingModList.get();
        if (mods == null) return false;
        var desires = mods.getMods().stream().filter(m -> m.getModId().equals("dndesires")).findFirst();
        var create = mods.getMods().stream().filter(m -> m.getModId().equals("create")).findFirst();
        return desires.isPresent() && create.isPresent()
                && desires.get().getVersion().toString().equals("2.3a-BETA")
                && create.get().getVersion().toString().equals("6.0.10");
    }

    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
