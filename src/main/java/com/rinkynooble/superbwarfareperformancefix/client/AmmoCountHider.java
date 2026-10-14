package com.rinkynooble.superbwarfareperformancefix.client;

import com.atsuishio.superbwarfare.client.overlay.AmmoCountOverlay;
import com.rinkynooble.superbwarfareperformancefix.ClientConfig;
import com.rinkynooble.superbwarfareperformancefix.SuperbWarfarePerformanceFix;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Skips Superb Warfare's ammo count overlay when the client config hides it. */
@Mod.EventBusSubscriber(modid = SuperbWarfarePerformanceFix.MODID, value = Dist.CLIENT)
public final class AmmoCountHider {
    private AmmoCountHider() {
    }

    // Matched by the overlay itself rather than its id, which isn't superbwarfare:ammo_count.
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().overlay() == AmmoCountOverlay.INSTANCE && ClientConfig.HIDE_AMMO_COUNT.get()) {
            event.setCanceled(true);
        }
    }
}
