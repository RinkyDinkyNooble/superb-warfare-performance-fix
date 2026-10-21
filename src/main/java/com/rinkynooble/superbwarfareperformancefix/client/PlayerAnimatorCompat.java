package com.rinkynooble.superbwarfareperformancefix.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.IPlayerModel;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The one step of playerAnimator's that the thermal arm fix repeats (see PlayerRendererMixin). Only loaded with
 * playerAnimator installed, and only used once {@link #enable} has found everything it calls, so a different
 * playerAnimator version leaves the fix off instead of failing on every frame.
 */
public final class PlayerAnimatorCompat {
    private static final Logger LOGGER = LogManager.getLogger("Superb Warfare Performance Fix");

    /** Set once at client setup. While false the thermal arm is drawn as before. */
    private static volatile boolean ready;

    private PlayerAnimatorCompat() {
    }

    public static boolean isReady() {
        return ready;
    }

    /** Turns the fix on if this playerAnimator version has everything {@link #prepForFirstPersonRender} uses. */
    public static void enable() {
        try {
            IPlayerModel.class.getMethod("playerAnimator_prepForFirstPersonRender");
            IAnimatedPlayer.class.getMethod("playerAnimator_getAnimation");
            AnimationApplier.class.getMethod("getFirstPersonMode");
            FirstPersonMode.class.getMethod("isEnabled");
            ready = true;
        } catch (LinkageError | ReflectiveOperationException e) {
            LOGGER.warn("Not fixing the thermal arm: this playerAnimator version is different ({}), so it's drawn as before.",
                    e.toString());
        }
    }

    /** Same as playerAnimator's own hook just before vanilla's setupAnim in renderHand. */
    public static void prepForFirstPersonRender(PlayerModel<?> model, AbstractClientPlayer player) {
        if (model instanceof IPlayerModel playerModel && player instanceof IAnimatedPlayer animatedPlayer
                && !animatedPlayer.playerAnimator_getAnimation().getFirstPersonMode().isEnabled()) {
            playerModel.playerAnimator_prepForFirstPersonRender();
        }
    }
}
