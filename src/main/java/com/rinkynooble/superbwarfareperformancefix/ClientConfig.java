package com.rinkynooble.superbwarfareperformancefix;

import net.minecraftforge.common.ForgeConfigSpec;

/** config/superbwarfareperformancefix-client.toml */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue HIDE_AMMO_COUNT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        HIDE_AMMO_COUNT = builder
                .comment("Hide Superb Warfare's ammo count overlay, shown while holding ammo or an ammo box.")
                .define("hideAmmoCountOverlay", false);
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
