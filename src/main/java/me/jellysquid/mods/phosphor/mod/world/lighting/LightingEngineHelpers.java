package me.jellysquid.mods.phosphor.mod.world.lighting;

import atomicstryker.dynamiclights.client.DynamicLights;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class LightingEngineHelpers {
    private static final IBlockState DEFAULT_BLOCK_STATE = Blocks.AIR.getDefaultState();

    // Avoids some additional logic in Chunk#getBlockState... 0 is always air
    static IBlockState posToState(final BlockPos pos, final Chunk chunk) {
        return posToState(pos, chunk.getBlockStorageArray()[pos.getY() >> 4]);
    }

    static IBlockState posToState(final BlockPos pos, final ExtendedBlockStorage section) {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        if (section != Chunk.NULL_BLOCK_STORAGE)
        {

            return section.getData().get((x & 15), (y & 15), (z & 15));

        }

        return DEFAULT_BLOCK_STATE;
    }

    static int getLightValueForState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        if(LightingEngine.isDynamicLightsLoaded) {
            /* Use the Dynamic Lights implementation */
            return DynamicLights.getLightValue(state.getBlock(), state, world, pos);
        } else {
            /* Use the vanilla implementation */
            return state.getLightValue(world, pos);
        }
    }
}
