package com.rinkynooble.superbwarfareperformancefix;

import com.rinkynooble.superbwarfareperformancefix.client.PlayerAnimatorCompat;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SuperbWarfarePerformanceFix.MODID)
public class SuperbWarfarePerformanceFix {
    public static final String MODID = "superbwarfareperformancefix";
    public static final String PLAYER_ANIMATOR = "playeranimator";

    public SuperbWarfarePerformanceFix() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // Client setup never fires on a dedicated server.
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded(PLAYER_ANIMATOR)) {
            // On the main thread, so loading playerAnimator's classes never races with its parallel setup.
            event.enqueueWork(PlayerAnimatorCompat::enable);
        }
    }
}
