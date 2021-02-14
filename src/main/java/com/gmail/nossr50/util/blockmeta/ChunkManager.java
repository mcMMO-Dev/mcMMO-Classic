package com.gmail.nossr50.util.blockmeta;

import org.bukkit.World;

public interface ChunkManager extends UserBlockTracker {
    void closeAll();
    void chunkUnloaded(int cx, int cz, World world);
    void unloadWorld(World world);
}
