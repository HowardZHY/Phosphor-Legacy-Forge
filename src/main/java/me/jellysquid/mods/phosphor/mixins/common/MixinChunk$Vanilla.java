package me.jellysquid.mods.phosphor.mixins.common;

import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Chunk.class)
public class MixinChunk$Vanilla {

    /**
     * Modifies the flag variable of setBlockState(BlockPos, IBlockState) to always be false after it is set.
     *
     * @author Angeline
     */
    @ModifyVariable(
            method = "setBlockState",
            at = @At(
                    value = "STORE",
                    ordinal = 1
            ),
            index = 13,
            name = "flag",
            allow = 1
    )
    private boolean setBlockStateInjectGenerateSkylightMapVanilla(boolean generateSkylight) {
        return false;
    }

    /**
     * Prevent propagateSkylightOcclusion from being called.
     * @author embeddedt
     */
    @Redirect(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;propagateSkylightOcclusion(II)V"))
    private void doPropagateSkylight(Chunk chunk, int i1, int i2) {
        /* No-op, we don't want skylight propagated */
    }

    /**
     * Prevent getLightFor from being called.
     * @author embeddedt
     */
    @Redirect(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getLightFor(Lnet/minecraft/world/EnumSkyBlock;Lnet/minecraft/util/BlockPos;)I"))
    private int getFakeLightFor(Chunk chunk, EnumSkyBlock skyBlock, BlockPos blockPos) {
        return 0;
    }
}
