package ccq.core.compat.cmr.mixin;

import ccq.core.compat.cmr.CmrHeatCodecs;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把 Create 的 {@code HeatCondition.STREAM_CODEC} 从「按 ordinal 编码」换成「按名字编码」。
 *
 * <p>CreateMoreRecipes 在该枚举 {@code <clinit>} 的 TAIL 追加 COOLED / FROZEN，
 * 顺序由 {@code Map.of(...)} 的随机遍历顺序决定。Create 的配方同步用这个 codec，
 * 因此客户端与服务端 ordinal 顺序不同时，JEI 里的冷却/冷冻会互换。
 *
 * <p>注入点同为 {@code <clinit>} 的 TAIL：无论与 CMR 的注入谁先谁后都不影响正确性，
 * 因为新 codec 在编解码时才按名字查找，不依赖枚举顺序。
 */
@Mixin(value = HeatCondition.class, remap = false)
public abstract class HeatConditionStreamCodecMixin {
    @Shadow
    @Final
    @Mutable
    public static StreamCodec<ByteBuf, HeatCondition> STREAM_CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void ccq$useNameBasedHeatCodec(CallbackInfo ci) {
        HeatConditionStreamCodecMixin.STREAM_CODEC = CmrHeatCodecs.nameBasedHeatConditionCodec();
    }
}
