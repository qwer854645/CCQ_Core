package ccq.core.tacz.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.nio.file.Path;
import java.util.List;
@Mixin(targets="com.tacz.guns.resource.GunPackLoader",remap=false)
public class OfficialPackScanMixin {
 @Inject(method="scanExtensions",at=@At("HEAD"),remap=false)
 private static void ccq$beforeOpenGunpacks(Path path,CallbackInfoReturnable<List<?>> cir){
  ccq.core.tacz.CcqGunPackBootstrap.beforeOfficialPackScan();
 }
}
