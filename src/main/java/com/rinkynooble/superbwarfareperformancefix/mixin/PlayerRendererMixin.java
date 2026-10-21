package com.rinkynooble.superbwarfareperformancefix.mixin;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rinkynooble.superbwarfareperformancefix.client.PlayerAnimatorCompat;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * With thermal imaging on, Superb Warfare draws the first-person arm a second time, white, from a callback at the
 * end of renderHand that sets the player model up again. playerAnimator keeps third-person animations off
 * first-person arms by marking the model just before vanilla's setup in renderHand, and that setup uses the mark up.
 * Superb Warfare's second setup is unmarked, so an animation that is playing (TaCZ's gun poses, once started) lands
 * on the white arm and stretches it across the view.
 *
 * <p>This marks the model again, the same way playerAnimator does, just before Superb Warfare's setup, so the white
 * arm is drawn where the normal arm is, as it is without playerAnimator. Only while thermal is on, the same flag
 * Superb Warfare's callback runs on, so the mark is always used up by that setup. Applied only with playerAnimator
 * installed and Superb Warfare's callback matching the one this was written for (see ThermalArmShape).
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    // Right after the sleeve, renderHand's last call. Code there always comes before every callback at its return,
    // Superb Warfare's included, whatever order the mixins are applied in.
    @Inject(method = "renderHand",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
                    ordinal = 1, shift = At.Shift.AFTER))
    private void superbwarfareperformancefix$markThermalArmFirstPerson(PoseStack poseStack, MultiBufferSource buffer, int light,
                                                                     AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                                                     CallbackInfo ci) {
        if (ClientEventHandler.activeThermalImaging && PlayerAnimatorCompat.isReady()) {
            PlayerAnimatorCompat.prepForFirstPersonRender(this.getModel(), player);
        }
    }
}
