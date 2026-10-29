package ccq.core.compat.cmr;

import ccq.core.CcqCoreMod;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * CreateMoreRecipes 用 mixin 往 Create 的 {@link HeatCondition} 枚举里追加 COOLED / FROZEN 两个常量，
 * 追加顺序由 {@code Map.of(...)} 的遍历顺序决定，而该顺序在每个 JVM 里是随机化的。
 *
 * <p>Create 的配方网络同步走的是 {@code HeatCondition.STREAM_CODEC}
 * （{@code CatnipStreamCodecBuilders.ofEnum}，按 ordinal 编码），
 * 于是服务端和客户端各自掷骰子：两边顺序不同时，热量条件在客户端会被解析成另一个常量。
 * 表现就是配方执行正常（服务端用自己的实例），但 JEI 里「冷却」和「冷冻」显示互换；
 * 单人游戏客户端与服务端同一个 JVM，所以永远不会错位——问题只在多人服务器上偶发。
 *
 * <p>这里把该字段换成按名字编码的 codec，使线格式不再依赖 ordinal。
 * 同时用 {@link #logHeatConditionOrder()} 在启动时打印一次实际解析顺序，便于排查。
 */
public final class CmrHeatCodecs {
    private CmrHeatCodecs() {
    }

    /**
     * 与 Create 原 codec 同类型（{@code StreamCodec<ByteBuf, HeatCondition>}），
     * 但以 {@link HeatCondition#getSerializedName()} 作为线格式，跨 JVM 稳定。
     */
    public static StreamCodec<ByteBuf, HeatCondition> nameBasedHeatConditionCodec() {
        return new StreamCodec<>() {
            @Override
            public HeatCondition decode(ByteBuf buffer) {
                String name = ByteBufCodecs.STRING_UTF8.decode(buffer);
                for (HeatCondition condition : HeatCondition.values()) {
                    if (condition.getSerializedName().equals(name)) {
                        return condition;
                    }
                }
                // 未知名字不要抛异常：退回 NONE，避免整包配方同步失败。
                CcqCoreMod.LOGGER.warn("Unknown heat requirement '{}' received; falling back to NONE", name);
                return HeatCondition.NONE;
            }

            @Override
            public void encode(ByteBuf buffer, HeatCondition value) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, value.getSerializedName());
            }
        };
    }
}
