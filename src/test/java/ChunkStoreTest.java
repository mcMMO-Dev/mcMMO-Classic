import com.gmail.nossr50.util.blockmeta.BitSetChunkStore;
import com.gmail.nossr50.util.blockmeta.ChunkStore;
import com.gmail.nossr50.util.blockmeta.HashChunkManager;
import com.gmail.nossr50.util.blockmeta.McMMOSimpleRegionFile;
import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Bukkit.class)
public class ChunkStoreTest {
    @Test
    public void testRoundTrip() {
        Random random = new Random(1000);

        World world = mockBukkitWorld();

        ChunkStore original = new BitSetChunkStore(world, random.nextInt(1000)- 500, random.nextInt(1000) - 500);
        populateChunkstore(original, random);
        byte[] serializedBytes = SerializationUtils.serialize(original);
        ChunkStore deserialized = (BitSetChunkStore)SerializationUtils.deserialize(serializedBytes);
        assertEqual(original, deserialized);
    }

    @Test
    public void testUpgrade() {
        Random random = new Random(1000);

        World world = mockBukkitWorld();

        ChunkStore original = new LegacyChunkStore(world, random.nextInt(1000) - 500, random.nextInt(1000) - 500);
        populateChunkstore(original, random);
        byte[] serializedBytes = SerializationUtils.serialize(original);
        ChunkStore deserialized = (BitSetChunkStore)deserialize(new ByteArrayInputStream(serializedBytes));
        assertEqual(original, deserialized);
    }

    @Test
    public void testSimpleRegionFileRoundTrip() throws IOException, ClassNotFoundException {
        Random random = new Random(1000);

        World world = mockBukkitWorld();

        File file = File.createTempFile("mcMMOUnitTestRegion", null);
        try
        {
            McMMOSimpleRegionFile region = new McMMOSimpleRegionFile(file, 0, 0);
            List<ChunkStore> chunks = new ArrayList<>();
            for (int cx = 0; cx < 32; cx++)
                for (int cz = 0; cz < 32; cz++) {
                    ChunkStore chunk = new BitSetChunkStore(world, cx, cz);
                    populateChunkstore(chunk, random);
                    chunks.add(chunk);
                }
            for (ChunkStore chunk : chunks)
            {
                try (ObjectOutputStream objectStream = new ObjectOutputStream(region.getOutputStream(chunk.getChunkX(), chunk.getChunkZ()))) {
                    objectStream.writeObject(chunk);
                    objectStream.flush();
                }
            }
            region.close();
            region = new McMMOSimpleRegionFile(file, 0, 0);
            for (ChunkStore original : chunks)
            {
                InputStream is = region.getInputStream(original.getChunkX(), original.getChunkZ());
                Assert.assertNotNull(is);
                ChunkStore deserialized;
                try (ObjectInputStream objectStream = new ObjectInputStream(is)) {
                    deserialized = (ChunkStore)objectStream.readObject();
                }
                assertEqual(original, deserialized);
            }
            region.close();
        }
        finally
        {
            file.delete();
        }
    }

    private void populateChunkstore(ChunkStore chunkStore, Random random)
    {
        for (int i = 0; i < random.nextInt(16 * 16 * 256); i++)
            chunkStore.setTrue(random.nextInt(16), random.nextInt(256), random.nextInt(16));
    }

    private World mockBukkitWorld(){
        UUID worldUUID = UUID.randomUUID();

        World world = mock(World.class);
        Mockito.when(world.getUID()).thenReturn(worldUUID);
        Mockito.when(world.getMaxHeight()).thenReturn(256);
        PowerMockito.mockStatic(Bukkit.class);
        BDDMockito.given(Bukkit.getWorld(worldUUID)).willReturn(world);

        return world;
    }

    private void assertEqual(ChunkStore expected, ChunkStore actual)
    {
        for (int y = 0; y < 256; y++)
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    Assert.assertEquals(expected.isTrue(x, y, z), actual.isTrue(x, y, z));
    }

    public static Object deserialize(ByteArrayInputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("The InputStream must not be null");
        } else {
            UnitTestObjectInputStream in = null;

            Object var2;
            try {
                in = new UnitTestObjectInputStream(inputStream);
                var2 = in.readObject();
            } catch (ClassNotFoundException var12) {
                throw new SerializationException(var12);
            } catch (IOException var13) {
                throw new SerializationException(var13);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException var11) {
                }

            }

            return var2;
        }
    }

    public static class LegacyChunkStore implements ChunkStore {
        private static final long serialVersionUID = -1L;
        transient private boolean dirty = false;
        public boolean[][][] store;
        private static final int CURRENT_VERSION = 7;
        private static final int MAGIC_NUMBER = 0xEA5EDEBB;
        private int cx;
        private int cz;
        private UUID worldUid;

        public LegacyChunkStore(World world, int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
            this.worldUid = world.getUID();
            this.store = new boolean[16][16][world.getMaxHeight()];
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
            return store[x][z][y];
        }

        @Override
        public void setTrue(int x, int y, int z) {
            if (y >= store[0][0].length || y < 0)
                return;
            store[x][z][y] = true;
            dirty = true;
        }

        @Override
        public void setFalse(int x, int y, int z) {
            if (y >= store[0][0].length || y < 0)
                return;
            store[x][z][y] = false;
            dirty = true;
        }

        @Override
        public void set(int x, int y, int z, boolean value) {
            if (y >= store[0][0].length || y < 0)
                return;
            store[x][z][y] = value;
            dirty = true;
        }

        @Override
        public boolean isEmpty() {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < store[0][0].length; y++) {
                        if (store[x][z][y]) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeInt(MAGIC_NUMBER);
            out.writeInt(CURRENT_VERSION);

            out.writeLong(worldUid.getLeastSignificantBits());
            out.writeLong(worldUid.getMostSignificantBits());
            out.writeInt(cx);
            out.writeInt(cz);
            out.writeObject(store);

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

            store = (boolean[][][]) in.readObject();

            if (fileVersionNumber < 5) {
                fixArray();
                dirty = true;
            }
        }

        private void fixArray() {
            boolean[][][] temp = this.store;
            this.store = new boolean[16][16][Bukkit.getWorld(worldUid).getMaxHeight()];
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < store[0][0].length; y++) {
                        try {
                            store[x][z][y] = temp[x][y][z];
                        }
                        catch (Exception e) { e.printStackTrace(); }
                    }
                }
            }
        }
    }

    private static class UnitTestObjectInputStream extends ObjectInputStream {
        public UnitTestObjectInputStream(ByteArrayInputStream byteArrayInputStream) throws IOException {
            super(byteArrayInputStream);
            enableResolveObject(true);
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass read = super.readClassDescriptor();
            if (read.getName().contentEquals("ChunkStoreTest$LegacyChunkStore")){
                return ObjectStreamClass.lookup(BitSetChunkStore.class);
            }
            return read;
        }
    }
}
