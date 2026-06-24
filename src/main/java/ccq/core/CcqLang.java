package ccq.core;

import net.createmod.catnip.lang.LangBuilder;

public final class CcqLang {
    private CcqLang() {
    }

    public static LangBuilder builder() {
        return new LangBuilder(CcqCoreMod.MOD_ID);
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }
}
