package ccq.core.compat.structurify.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class StructurifyMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() {return null;}
    public boolean shouldApplyMixin(String target,String mixin) {
        var mods=LoadingModList.get();return mods!=null&&mods.getModFileById("structurify")!=null;
    }
    public void acceptTargets(Set<String> mine,Set<String> others) {}
    public List<String> getMixins() {return null;}
    public void preApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
    public void postApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
}
