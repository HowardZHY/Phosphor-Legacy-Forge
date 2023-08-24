package me.jellysquid.mods.phosphor.mixins.lighting.common;

import me.jellysquid.mods.phosphor.api.IChunkLighting;
import me.jellysquid.mods.phosphor.api.IChunkLightingData;
import me.jellysquid.mods.phosphor.api.ILightingEngine;
import me.jellysquid.mods.phosphor.api.ILightingEngineProvider;
import me.jellysquid.mods.phosphor.mod.world.WorldChunkSlice;
import me.jellysquid.mods.phosphor.mod.world.lighting.LightingHooks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("all")
@Mixin(value = Chunk.class)
public abstract class MixinChunk implements IChunkLighting, IChunkLightingData, ILightingEngineProvider {

    private static final EnumFacing[] HORIZONTAL = EnumFacing.Plane.HORIZONTAL.facings();

    @Shadow
    @Final
    private ExtendedBlockStorage[] storageArrays;

    @Shadow
    private boolean isModified;

    @Shadow
    @Final
    private int[] heightMap;

    @Shadow
    private int heightMapMinimum;

    @Shadow
    @Final
    private int[] precipitationHeightMap;

    @Shadow
    @Final
    private World worldObj;

    @Shadow
    private boolean isTerrainPopulated;

    @Final
    @Shadow
    private boolean[] updateSkylightColumns;

    @Final
    @Shadow
    public int xPosition;

    @Final
    @Shadow
    public int zPosition;

    @Shadow
    private boolean isGapLightingUpdated;

    @Shadow
    public abstract TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType type);

    @Shadow
    public abstract IBlockState getBlockState(BlockPos pos);

    @Shadow
    protected abstract int getBlockLightOpacity(int x, int y, int z);

    @Shadow
    public abstract boolean canSeeSky(BlockPos pos);

    /**
     * Callback injected into the Chunk ctor to cache a reference to the lighting engine from the world.
     *
     * @author Angeline
     */
    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At("RETURN"))
    private void onConstructed(CallbackInfo ci) {
        this.lightingEngine = ((ILightingEngineProvider) this.worldObj).getLightingEngine();
    }

    /**
     * Callback injected to the head of getLightSubtracted(BlockPos, int) to force deferred light updates to be processed.
     *
     * @author Angeline
     */
    @Inject(method = "getLightSubtracted", at = @At("HEAD"))
    private void onGetLightSubtracted(BlockPos pos, int amount, CallbackInfoReturnable<Integer> cir) {
        this.lightingEngine.processLightUpdates();
    }

    /**
     * Callback injected at the end of onLoad() to have previously scheduled light updates scheduled again.
     *
     * @author Angeline
     */
    @Inject(method = "onChunkLoad", at = @At("RETURN"))
    private void onLoad(CallbackInfo ci) {
        LightingHooks.scheduleRelightChecksForChunkBoundaries(this.worldObj, (Chunk) (Object) this);
    }

    // === REPLACEMENTS ===

    /**
     * Replaces the call in setLightFor(Chunk, EnumSkyBlock, BlockPos) with our hook.
     *
     * @author Angeline
     */
    @Redirect(
            method = "setLightFor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/Chunk;generateSkylightMap()V"
            ),
            expect = 0
    )
    private void setLightForRedirectGenerateSkylightMap(Chunk chunk, EnumSkyBlock type, BlockPos pos, int value) {
        LightingHooks.initSkylightForSection(this.worldObj, (Chunk) (Object) this, this.storageArrays[pos.getY() >> 4]);
    }

    /**
     * @reason Overwrites relightBlock with a more efficient implementation.
     * @author Angeline
     */
    @Overwrite
    private void relightBlock(int x, int y, int z) {
        int i = this.heightMap[z << 4 | x] & 255;
        int j = i;

        if (y > i) {
            j = y;
        }

        while (j > 0 && this.getBlockLightOpacity(x, j - 1, z) == 0) {
            --j;
        }

        if (j != i) {
            this.heightMap[z << 4 | x] = j;

            if (this.worldObj.provider.hasNoSky) {
                LightingHooks.relightSkylightColumn(this.worldObj, (Chunk) (Object) this, x, z, i, j);
            }

            int l1 = this.heightMap[z << 4 | x];

            if (l1 < this.heightMapMinimum) {
                this.heightMapMinimum = l1;
            }
        }
    }

    /**
     * @reason Hook for calculating light updates only as needed. {@link MixinChunk#getCachedLightFor(EnumSkyBlock, BlockPos)} does not
     * call this hook.
     *
     * @author Angeline
     */
    @Overwrite
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        this.lightingEngine.processLightUpdatesForType(type);

        return this.getCachedLightFor(type, pos);
    }

    /**
     * @reason Hooks into checkLight() to check chunk lighting and returns immediately after, voiding the rest of the function.
     *
     * @author Angeline
     */
    @Overwrite
    public void func_150809_p() {
        this.isTerrainPopulated = true;

        LightingHooks.checkChunkLighting((Chunk) (Object) this, this.worldObj);
    }

    /**
     * @reason Optimized version of recheckGaps. Avoids chunk fetches as much as possible.
     *
     * @author Angeline
     */
    @Overwrite
    private void recheckGaps(boolean onlyOne) {
        this.worldObj.theProfiler.startSection("recheckGaps");

        WorldChunkSlice slice = new WorldChunkSlice(this.worldObj, this.xPosition, this.zPosition);

        if (this.worldObj.isAreaLoaded(new BlockPos(this.xPosition * 16 + 8, 0, this.zPosition * 16 + 8), 16)) {
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    if (this.recheckGapsForColumn(slice, x, z)) {
                        if (onlyOne) {
                            this.worldObj.theProfiler.endSection();

                            return;
                        }
                    }
                }
            }

            this.isGapLightingUpdated = false;
        }

        this.worldObj.theProfiler.endSection();
    }

    @Unique
    private boolean recheckGapsForColumn(WorldChunkSlice slice, int x, int z) {
        int i = x + z * 16;

        if (this.updateSkylightColumns[i]) {
            this.updateSkylightColumns[i] = false;

            int height = this.getHeightValue(x, z);

            int x1 = this.xPosition * 16 + x;
            int z1 = this.zPosition * 16 + z;

            int max = this.recheckGapsGetLowestHeight(slice, x1, z1);

            this.recheckGapsSkylightNeighborHeight(slice, x1, z1, height, max);

            return true;
        }

        return false;
    }

    @Unique
    private int recheckGapsGetLowestHeight(WorldChunkSlice slice, int x, int z) {
        int max = Integer.MAX_VALUE;

        for (EnumFacing facing : HORIZONTAL) {
            int j = x + facing.getFrontOffsetX();
            int k = z + facing.getFrontOffsetZ();
            Chunk chunk = slice.getChunkFromWorldCoords(j, k);
            if(chunk != null) {
                max = Math.min(max, slice.getChunkFromWorldCoords(j, k).getLowestHeight());
            }

        }

        return max;
    }

    @Unique
    private void recheckGapsSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int height, int max) {
        this.checkSkylightNeighborHeight(slice, x, z, max);

        for (EnumFacing facing : HORIZONTAL) {
            int j = x + facing.getFrontOffsetX();
            int k = z + facing.getFrontOffsetZ();

            this.checkSkylightNeighborHeight(slice, j, k, height);
        }
    }

    @Unique
    private void checkSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int maxValue) {
        if(slice.getChunkFromWorldCoords(x, z) == null) {
            return;
        }
        int i = slice.getChunkFromWorldCoords(x, z).getHeightValue(x & 15, z & 15);

        if (i > maxValue) {
            this.updateSkylightNeighborHeight(slice, x, z, maxValue, i + 1);
        } else if (i < maxValue) {
            this.updateSkylightNeighborHeight(slice, x, z, i, maxValue + 1);
        }
    }

    @Unique
    private void updateSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int startY, int endY) {
        if (endY > startY) {
            if (!slice.isLoaded(x, z, 16)) {
                return;
            }

            for (int i = startY; i < endY; ++i) {
                this.worldObj.checkLightFor(EnumSkyBlock.SKY, new BlockPos(x, i, z));
            }

            this.isModified = true;
        }
    }

    @Shadow
    public abstract int getHeightValue(int i, int j);

    // === INTERFACE IMPL ===

    @Unique
    private short[] neighborLightChecks;

    @Unique
    private boolean isLightInitialized;

    @Unique
    private ILightingEngine lightingEngine;

    @Override
    public short[] getNeighborLightChecks() {
        return this.neighborLightChecks;
    }

    @Override
    public void setNeighborLightChecks(short[] data) {
        this.neighborLightChecks = data;
    }

    @Override
    public ILightingEngine getLightingEngine() {
        return this.lightingEngine;
    }

    @Override
    public boolean isLightInitialized() {
        return this.isLightInitialized;
    }

    @Override
    public void setLightInitialized(boolean lightInitialized) {
        this.isLightInitialized = lightInitialized;
    }

    @Shadow
    protected abstract void func_177441_y();

    @Override
    public void setSkylightUpdatedPublic() {
        this.func_177441_y();
    }

    @Override
    public int getCachedLightFor(EnumSkyBlock type, BlockPos pos) {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;

        ExtendedBlockStorage extendedblockstorage = this.storageArrays[j >> 4];

        if (extendedblockstorage == null) {
            if (this.canSeeSky(pos)) {
                return type.defaultLightValue;
            }
            else {
                return 0;
            }
        }
        else if (type == EnumSkyBlock.SKY) {
            // In Minecraft there is no dimension.hasSkyLight() function, I guess it was added by forge
            // Because this checks if the dimension can see the sky, I guess it looks for the nether and end?
            //
            // 1.8.9 Note: isOverworld() doesn't exist here so i'm using !isNether()
            // It should be noted that isNether() is named wrong it should be hasNoSky (MCP) or so
            //
            //if (!this.world.dimension.hasSkyLight()) {
            if (this.worldObj.provider.getHasNoSky()) {
                return 0;
            }
            else {
                return extendedblockstorage.getExtSkylightValue(i, j & 15, k);
            }
        }
        else {
            if (type == EnumSkyBlock.BLOCK) {
                return extendedblockstorage.getExtBlocklightValue(i, j & 15, k);
            }
            else {
                return type.defaultLightValue;
            }
        }
    }


    // === END OF INTERFACE IMPL ===
}
