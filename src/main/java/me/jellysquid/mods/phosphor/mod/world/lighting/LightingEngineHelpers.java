package me.jellysquid.mods.phosphor.mod.world.lighting;

import me.jellysquid.mods.phosphor.mixins.client.ClientChunkProviderAccessor;
import me.jellysquid.mods.phosphor.mixins.common.ServerChunkProviderAccessor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;

public class LightingEngineHelpers {

    private static final IBlockState DEFAULT_BLOCK_STATE = Blocks.air.getDefaultState();

    // Avoids some additional logic in Chunk#getBlockState... 0 is always air
    static IBlockState posToState(final BlockPos pos, final Chunk chunk) {
        return posToState(pos, chunk.getBlockStorageArray()[pos.getY() >> 4]);
    }

    static IBlockState posToState(final BlockPos pos, final ExtendedBlockStorage section) {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        if (section != null)
        {
            IBlockState state = section.getBlockByExtId((x & 15), (y & 15), (z & 15)).getDefaultState();
            if (state != null) {
                return state;
            }
        }

        return DEFAULT_BLOCK_STATE;
    }

    static int getLightValueForState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        if (LightingEngine.isDynamicLightsLoaded) {
            /* Use the Dynamic Lights implementation */
            return atomicstryker.dynamiclights.client.DynamicLights.getLightValue(state.getBlock(), world, pos);
        } else {
            /* Use the vanilla implementation */
            return state.getBlock().getLightValue(world, pos);
        }
    }

    public static Chunk getLoadedChunk(IChunkProvider chunkProvider, int x, int z) {
        if (chunkProvider instanceof ChunkProviderServer) {
            LongHashMap<Chunk> chunkStorage = ((ServerChunkProviderAccessor) chunkProvider).getChunkStorage();
            return chunkStorage.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
        }
        if (chunkProvider instanceof ChunkProviderClient) {
            LongHashMap<Chunk> chunkStorage = ((ClientChunkProviderAccessor) chunkProvider).getChunkStorage();
            return chunkStorage.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
        }

        // Fallback for other providers, hopefully this doesn't break...
        return chunkProvider.provideChunk(x, z);
    }
}
