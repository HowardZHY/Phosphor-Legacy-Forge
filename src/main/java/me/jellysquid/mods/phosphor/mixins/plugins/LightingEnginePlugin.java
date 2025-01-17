package me.jellysquid.mods.phosphor.mixins.plugins;

import me.jellysquid.mods.phosphor.mod.PhosphorConfig;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

@SuppressWarnings("all")
public class LightingEnginePlugin implements IMixinConfigPlugin {
    private static final Logger logger = LogManager.getLogger("Phosphor Plugin");

    public static boolean ENABLE_ILLEGAL_THREAD_ACCESS_WARNINGS = false;

    private PhosphorConfig config;

    public boolean spongePresent;

    public boolean legacy = false;

    @Override
    public void onLoad(String mixinPackage) {
        logger.debug("Loading configuration");

        this.config = PhosphorConfig.loadConfig();

        if (!this.config.enablePhosphor) {
            logger.warn("Phosphor has been disabled through mod configuration! No patches will be applied...");
        }

        ENABLE_ILLEGAL_THREAD_ACCESS_WARNINGS = this.config.enableIllegalThreadAccessWarnings;

        String mcVersion = Loader.instance().getMCVersionString();

        logger.info("Detected MC Version : " + mcVersion);

        if (!mcVersion.contains("8.9")) {
            logger.warn("You're using legacy 1.8.8 instead of 1.8.9");
            this.legacy = true;
        }

        try {
            // This class will always be loaded by Forge prior to us (due to the tweak class ordering) and should have
            // no effect. On the off chance it isn't, early class loading shouldn't cause any issues as nobody seems to
            // transform core-mods themselves, or at least I hope they don't...
            Class.forName("org.spongepowered.mod.SpongeCoremod");

            this.spongePresent = true;
        } catch (Exception e) {
            this.spongePresent = false;
        }

        if (this.spongePresent) {
            logger.error("Sponge (Forge) has been detected on the classpath, you will run into issues. ");
            logger.error("Due to SpongeForge 1.8.9 's codebase was outdated and uses an extremely outdated Mixin (0.5.11), This mod can't be compatible with it! ");
            FMLCommonHandler.instance().exitJava(-1, false);
        }
    }

    @Override
    public String getRefMapperConfig() {
        if (Launch.blackboard.get("fml.deobfuscatedEnvironment") == Boolean.TRUE) {
            return null;
        }

        return "mixins.phosphor.refmap.json";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!this.config.enablePhosphor) {
            return false;
        }

        // Do not apply client transformations if we are not in a client environment!
        if (targetClassName.startsWith("net.minecraft.client") && MixinEnvironment.getCurrentEnvironment().getSide() != MixinEnvironment.Side.CLIENT) {
            logger.debug("Disabled patch '{}' because it targets an client-side class unavailable in the current environment", mixinClassName);

            return false;
        }

        if (mixinClassName.endsWith("$Vanilla")) {
            if (this.legacy) {
                logger.debug("Disable Chunk Patch For 1.8.8");
                return false;
            }
        }

        if (mixinClassName.endsWith("$Legacy")) {
            if (!this.legacy) {
                logger.debug("Disable Chunk Patch For 1.8.9");
                return false;
            }
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
