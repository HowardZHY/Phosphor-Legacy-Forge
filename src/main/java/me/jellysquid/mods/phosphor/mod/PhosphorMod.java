package me.jellysquid.mods.phosphor.mod;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        name = PhosphorConstants.MOD_NAME,
        modid = PhosphorConstants.MOD_ID,
        version = "0.2.9.3",
        acceptedMinecraftVersions = "[1.8,)",
        acceptableRemoteVersions = "*"
)
public class PhosphorMod {

    public static final Logger LOGGER = LogManager.getLogger("Phosphor");

}