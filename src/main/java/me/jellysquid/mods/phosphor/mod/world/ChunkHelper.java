package me.jellysquid.mods.phosphor.mod.world;

import me.jellysquid.mods.phosphor.mixins.common.ClientChunkProviderAccessor;
import me.jellysquid.mods.phosphor.mixins.common.ServerChunkProviderAccessor;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

public class ChunkHelper {
    public static Chunk getLoadedChunk(IChunkProvider IChunkProvider, int x, int z) {
        if (IChunkProvider instanceof ChunkProviderServer) {
            LongHashMap<Chunk> chunkStorage = ((ServerChunkProviderAccessor) IChunkProvider).getChunkStorage();
            return chunkStorage.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
        }
        if (IChunkProvider instanceof ChunkProviderClient) {
            LongHashMap<Chunk> chunkStorage = ((ClientChunkProviderAccessor) IChunkProvider).getChunkStorage();
            return chunkStorage.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
        }

        // Fallback for other providers, hopefully this doesn't break...
        return IChunkProvider.provideChunk(x, z);
    }
}