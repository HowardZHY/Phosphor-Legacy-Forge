package me.jellysquid.mods.phosphor.core;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
//import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.List;
import java.util.Map;

//@IFMLLoadingPlugin.MCVersion("1.8.9")
public class PhosphorFMLPlugin implements IFMLLoadingPlugin {

    public PhosphorFMLPlugin() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.phosphor.json");
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.UNKNOWN);
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    /*@Override
    public List<String> getMixinConfigs() {
        return ImmutableList.of("mixins.phosphor.json");
    }*/
}
