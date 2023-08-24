package me.jellysquid.mods.phosphor.mixins.common;

import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkProviderServer.class)
public interface ServerChunkProviderAccessor {
    @Accessor("id2ChunkMap")
    LongHashMap<Chunk> getChunkStorage();
}