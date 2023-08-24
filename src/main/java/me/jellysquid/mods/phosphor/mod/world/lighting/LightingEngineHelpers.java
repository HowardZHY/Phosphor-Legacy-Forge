package me.jellysquid.mods.phosphor.mod.world.lighting;

import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

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
            int key = section.getData()[(y & 15) << 8 | (z & 15) << 4 | x & 15];

            if (key != 0) {
                IBlockState state = Block.BLOCK_STATE_IDS.getByValue(key);

                if (state != null) {
                    return state;
                }
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
}
