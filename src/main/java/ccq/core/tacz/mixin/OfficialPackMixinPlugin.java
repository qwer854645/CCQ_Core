package ccq.core.tacz.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public final class OfficialPackMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }

    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var mods = LoadingModList.get();
        if (mods == null) return false;
        return mods.getMods().stream().anyMatch(m->m.getModId().equals("tacz")&&m.getVersion().getMajorVersion()==1&&m.getVersion().getMinorVersion()==2);
    }

    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
