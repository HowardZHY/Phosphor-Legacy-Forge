package me.jellysquid.mods.phosphor.mixins.common;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;
;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkProviderClient.class)
public interface ClientChunkProviderAccessor {
    @Accessor("chunkMapping")
    LongHashMap<Chunk> getChunkStorage();
}