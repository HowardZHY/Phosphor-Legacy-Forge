package me.jellysquid.mods.phosphor.mixins.common;

import me.jellysquid.mods.phosphor.api.ILightingEngineProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServer {
    @Final
    @Shadow
    public WorldServer world;

    @Final
    @Shadow
    private Set<Long> droppedChunksSet;

    /**
     * Injects a callback into the start of saveChunks(boolean) to force all light updates to be processed before saving.
     *
     * @author Angeline
     */
    @Inject(method = "saveChunks", at = @At("HEAD"))
    private void onSaveChunks(boolean p_saveChunks_1_, CallbackInfoReturnable<Boolean> cir) {
        ((ILightingEngineProvider) this.world).getLightingEngine().processLightUpdates();
    }

    /**
     * Injects a callback into the start of the onTick() method to process all pending light updates. This is not necessarily
     * required, but we don't want our work queues getting too large.
     *
     * @author Angeline
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfoReturnable<Boolean> cir) {
        if (!this.world.disableLevelSaving) {
            if (!this.droppedChunksSet.isEmpty()) {
                ((ILightingEngineProvider) this.world).getLightingEngine().processLightUpdates();
            }
        }
    }
}
