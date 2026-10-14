package com.rinkynooble.superbwarfareperformancefix;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(SuperbWarfarePerformanceFix.MODID)
public class SuperbWarfarePerformanceFix {
    public static final String MODID = "superbwarfareperformancefix";

    public SuperbWarfarePerformanceFix() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
