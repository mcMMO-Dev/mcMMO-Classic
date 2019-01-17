package com.gmail.nossr50.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

public abstract class VersionedConfigLoader extends ConfigLoader {
    public VersionedConfigLoader(String fileName) {
        super(fileName);
    }

    @Override
    protected void loadFile() {
        super.loadFile();
        FileConfiguration internalConfig = YamlConfiguration.loadConfiguration(plugin.getResourceAsReader(fileName));

        int version = config.getInt("Version");

        if (version == -1)
            return;

        if (version >= internalConfig.getInt("Version"))
            return;

        plugin.getLogger().warning("You are using an old version of the " + fileName + " file.");
        plugin.getLogger().warning("Your old file has been renamed to " + fileName + "." + version + ".old and has been replaced by an updated default version.");

        getFile().renameTo(new File(getFile().getPath() + "." + version + ".old"));

        if (plugin.getResource(fileName) != null) {
            plugin.saveResource(fileName, true);
        }

        plugin.getLogger().warning("Reloading " + fileName + " with new values...");
        super.loadFile();
    }
}
