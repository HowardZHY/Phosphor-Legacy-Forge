package me.jellysquid.mods.phosphor.mixins.lighting.common;

import me.jellysquid.mods.phosphor.api.ILightingEngineProvider;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(S21PacketChunkData.class)
public abstract class MixinS21PacketChunkData {
    /**
     * @author Angeline
     * Injects a callback into S21PacketChunkData#calculateChunkSize(Chunk, booolean, int) to force light updates to be
     * processed before creating the client payload. We use this method rather than the constructor as it is not valid
     * to inject elsewhere other than the RETURN of a ctor, which is too late for our needs.
     */
    @Inject(method = "getExtractedData", at = @At("HEAD"))
    private static void onCalculateChunkSize(Chunk chunkIn, boolean hasSkyLight, boolean p_getExtractedData_2_, int changedSectionFilter, CallbackInfoReturnable<Integer> cir) {
        ((ILightingEngineProvider) chunkIn).getLightingEngine().processLightUpdates();
    }
}
