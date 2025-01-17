package me.jellysquid.mods.phosphor.mixins.common;

import me.jellysquid.mods.phosphor.api.IChunkLighting;
import me.jellysquid.mods.phosphor.api.IChunkLightingData;
import me.jellysquid.mods.phosphor.api.ILightingEngine;
import me.jellysquid.mods.phosphor.api.ILightingEngineProvider;
import me.jellysquid.mods.phosphor.mod.PhosphorMod;
import me.jellysquid.mods.phosphor.mod.world.WorldChunkSlice;
import me.jellysquid.mods.phosphor.mod.world.lighting.LightingHooks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("all")
@Mixin(Chunk.class)
public abstract class MixinChunk implements IChunkLighting, IChunkLightingData, ILightingEngineProvider {

    // === SHADOW START ===

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
    private int queuedLightChecks;

    @Shadow
    private boolean isChunkLoaded;

    @Shadow
    public abstract TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType type);

    @Shadow
    public abstract IBlockState getBlockState(BlockPos pos);

    @Shadow
    public abstract int getHeightValue(int i, int j);

    @Shadow
    public abstract boolean canSeeSky(BlockPos pos);

    // === SHADOW END ===

    // === FIELD START ===

    private static final EnumFacing[] HORIZONTAL = EnumFacing.Plane.HORIZONTAL.facings();

    // === FIELD END ===

    // === MIXINS START ===

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
        this.getLightingEngine().processLightUpdates();
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

    // === REPLACEMENTS START ===

    /**
     * @reason Replaces the call in setLightFor(Chunk, EnumSkyBlock, BlockPos) with our hook.
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
     * @reason Forge: Can sometimes be called before we are added to the global world list. So use the less accurate one during that. It'll be recalculated later.
     * @author MinecraftForge 1.9.4, 1.11, 1.12.2
     */
    @Overwrite
    public int getBlockLightOpacity(int x, int y, int z) {
        IBlockState state = this.getBlockState(new BlockPos (x, y, z));
        return !this.isChunkLoaded ? state.getBlock().getLightOpacity() : state.getBlock().getLightOpacity(this.worldObj, new BlockPos(this.xPosition << 4 | x & 15, y, this.zPosition << 4 | z & 15));
    }

    /**
     * @reason Overwrites relightBlock with a more efficient implementation.
     * @author Angeline
     */
    @Overwrite
    public void relightBlock(int x, int y, int z) {
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

            if (!this.worldObj.provider.getHasNoSky()) {
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
     * @author Angeline
     */
    @Overwrite
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        this.getLightingEngine().processLightUpdatesForType(type);

        return this.getCachedLightFor(type, pos);
    }

    /**
     * @reason Hooks into checkLight() to check chunk lighting and returns immediately after, voiding the rest of the function.
     * @author Angeline
     */
    @Overwrite
    public void func_150809_p() {
        this.isTerrainPopulated = true;

        LightingHooks.checkChunkLighting((Chunk) (Object) this, this.worldObj);
    }

    /**
     * @reason Optimized version of recheckGaps. Avoids chunk fetches as much as possible.
     * @author Angeline
     */
    @Overwrite
    public void recheckGaps(boolean onlyOne) {
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

    /**
     * @author embeddedt
     * @reason optimize random light checks so they complete faster
     */
    @Overwrite
    public void enqueueRelightChecks() {
        if (this.queuedLightChecks >= 4096)
        {
            return;
        }

        boolean isActiveChunk = worldObj.activeChunkSet.contains(new ChunkCoordIntPair(this.xPosition, this.zPosition));
        int lightRecheckSpeed;

        if (worldObj.isRemote && isActiveChunk) {
            lightRecheckSpeed = 256;
        } else if (worldObj.isRemote) {
            lightRecheckSpeed = 64;
        } else {
            lightRecheckSpeed = 32;
        }

        BlockPos blockpos = new BlockPos(this.xPosition << 4, 0, this.zPosition << 4);
        for (int i = 0; i < lightRecheckSpeed; ++i) {

            int j = this.queuedLightChecks % 16;
            int k = this.queuedLightChecks / 16 % 16;
            int l = this.queuedLightChecks / 256;
            ++this.queuedLightChecks;

            for (int y = 0; y < 16; ++y)
            {
                ExtendedBlockStorage storage = this.storageArrays[j];
                BlockPos blockpos1 = blockpos.add(k, (j << 4) + y, l);
                boolean flag = y == 0 || y == 15 || k == 0 || k == 15 || l == 0 || l == 15;

                if (storage == null && flag || storage != null && storage.getBlockByExtId(k, y, l).getMaterial() == Material.air)
                {
                    for (EnumFacing enumfacing : EnumFacing.values())
                    {
                        BlockPos blockpos2 = blockpos1.offset(enumfacing);

                        if (this.worldObj.getBlockState(blockpos2).getBlock().getLightValue() > 0)
                        {
                            this.worldObj.checkLight(blockpos2);
                        }
                    }

                    this.worldObj.checkLight(blockpos1);
                }
            }
        }
    }

    // === REPLACEMENTS END ===

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

    private int recheckGapsGetLowestHeight(WorldChunkSlice slice, int x, int z) {
        int max = Integer.MAX_VALUE;

        for (EnumFacing facing : HORIZONTAL) {
            int j = x + facing.getFrontOffsetX();
            int k = z + facing.getFrontOffsetZ();
            Chunk chunk = slice.getChunkFromWorldCoords(j, k);
            if (chunk != null) {
                max = Math.min(max, chunk.getLowestHeight());
            }

        }

        return max;
    }

    private void recheckGapsSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int height, int max) {
        this.checkSkylightNeighborHeight(slice, x, z, max);

        for (EnumFacing facing : HORIZONTAL) {
            int j = x + facing.getFrontOffsetX();
            int k = z + facing.getFrontOffsetZ();

            this.checkSkylightNeighborHeight(slice, j, k, height);
        }
    }

    private void checkSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int maxValue) {

        Chunk chunk = slice.getChunkFromWorldCoords(x, z);
        if (chunk != null) {
            int i = chunk.getHeightValue(x & 15, z & 15);

            if (i > maxValue) {
                this.updateSkylightNeighborHeight(slice, x, z, maxValue, i + 1);
            } else if (i < maxValue) {
                this.updateSkylightNeighborHeight(slice, x, z, i, maxValue + 1);
            }

        } else {
            PhosphorMod.LOGGER.warn("Chunk is null! x: " + x + " z: " + z + " maxValue: " + maxValue);
        }
    }

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

    // === INTERFACE IMPL ===

    private short[] neighborLightChecks;

    private boolean isLightInitialized;

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
        if (this.lightingEngine == null) {
            this.lightingEngine = ((ILightingEngineProvider)this.worldObj).getLightingEngine();
            if (this.lightingEngine == null) {
                throw new IllegalStateException("Cannot get Lighting Engine from instance");
            }
        }
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

        ExtendedBlockStorage storage = this.storageArrays[j >> 4];

        if (storage == null) {
            if (this.canSeeSky(pos)) {
                return type.defaultLightValue;
            }
            else {
                return 0;
            }
        }
        else if (type == EnumSkyBlock.SKY) {
            if (this.worldObj.provider.getHasNoSky()) {
                return 0;
            }
            else {
                return storage.getExtSkylightValue(i, j & 15, k);
            }
        }
        else {
            if (type == EnumSkyBlock.BLOCK) {
                return storage.getExtBlocklightValue(i, j & 15, k);
            }
            else {
                return type.defaultLightValue;
            }
        }
    }

    // === END OF INTERFACE IMPL ===

    private ExtendedBlockStorage initSection(int y, boolean storeSkylight) {

        ExtendedBlockStorage storage = new ExtendedBlockStorage(y, storeSkylight);

        LightingHooks.initSkylightForSection(this.worldObj, (Chunk) (Object) this, storage);

        return storage;
    }

    /**
     * Redirects the construction of the ExtendedBlockStorage in setBlockState(BlockPos, IBlockState). We need to initialize
     * the skylight data for the constructed section as soon as possible.
     *
     * @author Angeline
     */
    @Redirect(
            method = "setBlockState",
            at = @At(
                    value = "NEW",
                    args = "class=net/minecraft/world/chunk/storage/ExtendedBlockStorage"
            ),
            expect = 0
    )
    private ExtendedBlockStorage setBlockStateCreateSectionVanilla(int y, boolean storeSkylight) {
        return this.initSection(y, storeSkylight);
    }
}
