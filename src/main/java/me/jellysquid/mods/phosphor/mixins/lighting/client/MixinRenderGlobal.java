package me.jellysquid.mods.phosphor.mixins.lighting.client;

import me.jellysquid.mods.phosphor.mod.PhosphorMod;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderGlobal.class})
public class MixinRenderGlobal {

    @Inject(at = @At("HEAD"), method = "notifyLightSet")
    public void notifyLightSet(BlockPos pos, CallbackInfo ci) {
        //PhosphorMod.LOGGER.debug(pos);
    }
}
