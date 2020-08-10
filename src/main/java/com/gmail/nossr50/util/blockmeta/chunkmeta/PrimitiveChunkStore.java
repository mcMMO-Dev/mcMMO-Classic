package com.gmail.nossr50.util.blockmeta.chunkmeta;

import com.gmail.nossr50.util.blockmeta.ChunkletStore;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.UUID;

public class PrimitiveChunkStore implements ChunkStore {
    private static final long serialVersionUID = -1L;
    transient private boolean dirty = false;
    // Bitset store conforms to a "bottom-up" bit ordering consisting of a stack of {worldHeight} Y planes, each Y plane consists of 16 Z rows of 16 X bits.
    public BitSet store;
    private static final int CURRENT_VERSION = 8;
    private static final int MAGIC_NUMBER = 0xEA5EDEBB;
    private int cx;
    private int cz;
    private int worldHeight;
    private UUID worldUid;

    public PrimitiveChunkStore(World world, int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
        this.worldUid = world.getUID();
        this.worldHeight = world.getMaxHeight();
        this.store = new BitSet(16 * 16 * worldHeight);
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public int getChunkX() {
        return cx;
    }

    @Override
    public int getChunkZ() {
        return cz;
    }

    @Override
    public boolean isTrue(int x, int y, int z) {
        return store.get(coordToIndex(x, y, z));
    }

    @Override
    public void setTrue(int x, int y, int z) {
        set(x, y, z, true);
    }

    @Override
    public void setFalse(int x, int y, int z) {
        set(x, y, z, false);
    }

    private void set(int x, int y, int z, boolean value) {
        if (y >= worldHeight || y < 0)
            return;
        store.set(coordToIndex(x, y, z), value);
        dirty = true;
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public void copyFrom(ChunkletStore otherStore) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < worldHeight; y++) {
                    store.set(coordToIndex(x, y, z), otherStore.isTrue(x, y, z));
                }
            }
        }
        dirty = true;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(MAGIC_NUMBER);
        out.writeInt(CURRENT_VERSION);

        out.writeLong(worldUid.getLeastSignificantBits());
        out.writeLong(worldUid.getMostSignificantBits());
        out.writeInt(cx);
        out.writeInt(cz);
        out.writeInt(worldHeight);

        // Store the byte array directly so we don't have the object type info overhead
        byte[] storeData = store.toByteArray();
        out.writeInt(storeData.length);
        out.write(storeData);

        dirty = false;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int magic = in.readInt();
        // Can be used to determine the format of the file
        int fileVersionNumber = in.readInt();

        if (magic != MAGIC_NUMBER) {
            fileVersionNumber = 0;
        }

        long lsb = in.readLong();
        long msb = in.readLong();
        worldUid = new UUID(msb, lsb);
        cx = in.readInt();
        cz = in.readInt();

        // Prior to version 8 we stored a boolean array array array.  Read them in and copy into the new bitset
        if (fileVersionNumber < 8) {
            boolean[][][] oldStore = (boolean[][][]) in.readObject();
            worldHeight = oldStore[0][0].length;
            store = new BitSet(16 * 16 * worldHeight / 8);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < worldHeight; y++) {
                        store.set(coordToIndex(x, y, z), oldStore[x][z][y]);
                    }
                }
            }
            dirty = true;
        }
        else
        {
            worldHeight = in.readInt();
            byte[] temp = new byte[in.readInt()];
            in.readFully(temp);
            store = BitSet.valueOf(temp);
        }
        World world = Bukkit.getWorld(worldUid);
        // Not sure how this case could come up, but might as well handle it gracefully.  Loading a chunkstore for an unloaded world?
        if (world == null)
            return;
        // Lop off any extra data if the world height has shrunk
        int currentWorldHeight = world.getMaxHeight();
        if (currentWorldHeight < worldHeight)
        {
            store.clear(coordToIndex(16, currentWorldHeight, 16), store.length());
            worldHeight = currentWorldHeight;
            dirty = true;
        }
        // If the world height has grown, update the worldHeight variable, but don't bother marking it dirty as unless something else changes we don't need to force a file write;
        else if (currentWorldHeight > worldHeight)
            worldHeight = currentWorldHeight;
    }

    private int coordToIndex(int x, int y, int z) {
        return (z * 16 + x) + (256 * y);
    }
}
