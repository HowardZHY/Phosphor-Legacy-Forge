package me.jellysquid.mods.phosphor.mixins.common;

import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnumFacing.class)
public interface DirectionAccessor {
    @Accessor("VALUES")
    static EnumFacing[] getAll() {
        throw new AssertionError();
    }

    @Accessor("HORIZONTALS")
    static EnumFacing[] getHorizontal() {
        throw new AssertionError();
    }
}
