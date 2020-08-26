package com.gmail.nossr50.util.blockmeta;

import com.gmail.nossr50.mcMMO;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.io.*;
import java.util.*;

public class HashChunkManager implements ChunkManager {
    private final HashMap<UUID, HashMap<Long, McMMOSimpleRegionFile>> regionFiles = new HashMap<>();
    private final HashMap<ChunkKey, ChunkStore> store = new HashMap<>();

    @Override
    public synchronized void closeAll() {
        for (UUID uid : regionFiles.keySet()) {
            HashMap<Long, McMMOSimpleRegionFile> worldRegions = regionFiles.get(uid);
            for (Iterator<McMMOSimpleRegionFile> worldRegionIterator = worldRegions.values().iterator(); worldRegionIterator.hasNext(); ) {
                McMMOSimpleRegionFile rf = worldRegionIterator.next();
                if (rf != null) {
                    rf.close();
                    worldRegionIterator.remove();
                }
            }
        }
        regionFiles.clear();
    }

    @Override
    public synchronized ChunkStore readChunkStore(World world, int x, int z) throws IOException {
        McMMOSimpleRegionFile rf = getSimpleRegionFile(world, x, z, false);
        if (rf == null)
            return null;
        try (InputStream in = rf.getInputStream(x, z)) {
            if (in == null)
                return null;
            try (ObjectInputStream objectStream = new RefactorObjectInputStream(in)) {
                Object o = objectStream.readObject();
                if (o instanceof ChunkStore) {
                    return (ChunkStore) o;
                }

                throw new RuntimeException("Wrong class type read for chunk meta data for " + x + ", " + z);
            } catch (IOException | ClassNotFoundException e) {
                return null;
            }
        }
    }

    @Override
    public synchronized void writeChunkStore(World world, int x, int z, ChunkStore data) {
        if (!data.isDirty())
            return;
        try {
            McMMOSimpleRegionFile rf = getSimpleRegionFile(world, x, z, true);
            try (ObjectOutputStream objectStream = new ObjectOutputStream(rf.getOutputStream(x, z))) {
                objectStream.writeObject(data);
                objectStream.flush();
            }
            data.setDirty(false);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to write chunk meta data for " + x + ", " + z, e);
        }
    }

    @Override
    public synchronized void closeChunkStore(World world, int x, int z) {
        McMMOSimpleRegionFile rf = getSimpleRegionFile(world, x, z, false);
        if (rf == null)
            return;
        rf.close();
    }

    private synchronized McMMOSimpleRegionFile getSimpleRegionFile(World world, int x, int z, boolean createIfAbsent) {
        // Get map for this world.
        HashMap<Long, McMMOSimpleRegionFile> worldRegions = regionFiles.computeIfAbsent(world.getUID(), k -> new HashMap<>());

        // Compute region index (32x32 chunk regions)
        int rx = x >> 5;
        int rz = z >> 5;

        // Key  is just a 64bit number, upper 32 bits are rx, lower 32 bits are rz
        long key2 = (((long) rx) << 32) | ((rz) & 0xFFFFFFFFL);

        return worldRegions.computeIfAbsent(key2, k -> {
            File worldRegionsDirectory = new File(world.getWorldFolder(), "mcmmo_regions");
            if (!createIfAbsent && !worldRegionsDirectory.isDirectory())
                return null; // Don't create the directory on read-only operations
            worldRegionsDirectory.mkdirs(); // Ensure directory exists
            File regionFile = new File(worldRegionsDirectory, "mcmmo_" + rx + "_" + rz + "_.mcm");
            if (!createIfAbsent && !regionFile.exists())
                return null; // Don't create the file on read-only operations
            return new McMMOSimpleRegionFile(regionFile, rx, rz);
        });
    }

    private ChunkStore loadChunk(int cx, int cz, World world) {
        ChunkStore chunkStore = null;

        try {
            chunkStore = readChunkStore(world, cx, cz);
        }
        catch (Exception ignored) {}

        return chunkStore;
    }

    public void unloadChunk(int cx, int cz, World world) {
        saveChunk(cx, cz, world);
        store.remove(toChunkKey(world, cx, cz));
    }

    @Override
    public synchronized void saveChunk(int cx, int cz, World world) {
        if (world == null)
            return;

        ChunkKey key = toChunkKey(world, cx, cz);

        ChunkStore out = store.get(key);

        if (out == null)
            return;

        if (!out.isDirty())
            return;

        writeChunkStore(world, cx, cz, out);
    }

    @Override
    public synchronized void chunkUnloaded(int cx, int cz, World world) {
        if (world == null)
            return;

        unloadChunk(cx, cz, world);
    }

    @Override
    public synchronized void saveWorld(World world) {
        if (world == null)
            return;

        closeAll();
        UUID wID = world.getUID();

        List<ChunkKey> keys = new ArrayList<>(store.keySet());
        for (ChunkKey key : keys) {
            if (wID.equals(key.worldID)) {
                try {
                    saveChunk(key.cx, key.cz, world);
                }
                catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    @Override
    public synchronized void unloadWorld(World world) {
        if (world == null)
            return;

        closeAll();
        UUID wID = world.getUID();

        List<ChunkKey> keys = new ArrayList<>(store.keySet());
        for (ChunkKey key : keys) {
            if (wID.equals(key.worldID)) {
                try {
                    unloadChunk(key.cx, key.cz, world);
                }
                catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    @Override
    public synchronized void loadWorld(World world) {}

    @Override
    public synchronized void saveAll() {
        closeAll();

        for (World world : mcMMO.p.getServer().getWorlds()) {
            saveWorld(world);
        }
    }

    @Override
    public synchronized void unloadAll() {
        closeAll();

        for (World world : mcMMO.p.getServer().getWorlds()) {
            unloadWorld(world);
        }
    }

    @Override
    public synchronized boolean isTrue(int x, int y, int z, World world) {
        if (world == null)
            return false;

        int cx = x >> 4;
        int cz = z >> 4;

        ChunkKey key = toChunkKey(world, cx, cz);

        // Get chunk, load from file if necessary
        ChunkStore check = store.computeIfAbsent(key, k -> loadChunk(cx, cz, world));

        // No chunk, return false
        if (check == null)
            return false;

        int ix = Math.abs(x) % 16;
        int iz = Math.abs(z) % 16;

        return check.isTrue(ix, y, iz);
    }

    @Override
    public synchronized boolean isTrue(Block block) {
        if (block == null)
            return false;

        return isTrue(block.getX(), block.getY(), block.getZ(), block.getWorld());
    }

    @Override
    public synchronized boolean isTrue(BlockState blockState) {
        if (blockState == null)
            return false;

        return isTrue(blockState.getX(), blockState.getY(), blockState.getZ(), blockState.getWorld());
    }

    @Override
    public synchronized void setTrue(int x, int y, int z, World world) {
        set(x, y, z, world, true);
    }

    @Override
    public synchronized void setTrue(Block block) {
        if (block == null)
            return;

        setTrue(block.getX(), block.getY(), block.getZ(), block.getWorld());
    }

    @Override
    public void setTrue(BlockState blockState) {
        if (blockState == null)
            return;

        setTrue(blockState.getX(), blockState.getY(), blockState.getZ(), blockState.getWorld());
    }

    @Override
    public synchronized void setFalse(int x, int y, int z, World world) {
        set(x, y, z, world, false);
    }

    @Override
    public synchronized void setFalse(Block block) {
        if (block == null)
            return;

        setFalse(block.getX(), block.getY(), block.getZ(), block.getWorld());
    }

    @Override
    public synchronized void setFalse(BlockState blockState) {
        if (blockState == null)
            return;

        setFalse(blockState.getX(), blockState.getY(), blockState.getZ(), blockState.getWorld());
    }

    public synchronized  void set(int x, int y, int z, World world, boolean value){
        if (world == null)
            return;

        // Bitshift to chunk coordinate
        int cx = x >> 4;
        int cz = z >> 4;

        ChunkKey key = toChunkKey(world, cx, cz);

        // Get/Load/Create chunkstore
        ChunkStore cStore = store.computeIfAbsent(key, k -> {
            // Load from file
            ChunkStore loaded = loadChunk(cx, cz, world);
            if (loaded != null)
                return loaded;
            // If setting to false, no need to create an empty chunkstore
            if (!value)
                return null;
            // Create a new chunkstore
            return ChunkStoreFactory.getChunkStore(world, cx, cz);
        });

        // Indicates setting false on empty chunkstore
        if (cStore == null)
            return;

        // Get block offset (offset from chunk corner)
        int ix = Math.abs(x) % 16;
        int iz = Math.abs(z) % 16;

        // Set chunk store value
        cStore.set(ix, y, iz, value);
    }

    private ChunkKey toChunkKey(World world, int cx, int cz){
        return new ChunkKey(world.getUID(), cx, cz);
    }

    // Chunk Key class
    private static final class ChunkKey {
        public final UUID worldID;
        public final int cx;
        public final int cz;

        private ChunkKey(UUID worldID, int cx, int cz) {
            this.worldID = worldID;
            this.cx = cx;
            this.cz = cz;
        }
    }

    @Override
    public synchronized void cleanUp() {}

    // Handles loading the old serialized classes even though we have changed name/package
    private static class RefactorObjectInputStream extends ObjectInputStream {
        public RefactorObjectInputStream(InputStream in) throws IOException {
            super(in);
            enableResolveObject(true);
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass read = super.readClassDescriptor();
            if (read.getName().contentEquals("com.gmail.nossr50.util.blockmeta.chunkmeta.PrimitiveChunkStore")){
                return ObjectStreamClass.lookup(BitSetChunkStore.class);
            }
            return read;
        }
    }
}
