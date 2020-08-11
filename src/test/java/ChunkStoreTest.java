import com.gmail.nossr50.util.blockmeta.chunkmeta.PrimitiveChunkStore;
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

import java.util.Random;
import java.util.UUID;

import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Bukkit.class)
public class ChunkStoreTest {
    @Test
    public void testRoundTrip() {
        Random random = new Random(1000);
        UUID worldUUID = UUID.randomUUID();

        World world = mock(World.class);
        Mockito.when(world.getUID()).thenReturn(worldUUID);
        Mockito.when(world.getMaxHeight()).thenReturn(256);
        PowerMockito.mockStatic(Bukkit.class);
        BDDMockito.given(Bukkit.getWorld(worldUUID)).willReturn(world);

        PrimitiveChunkStore original = new PrimitiveChunkStore(world, random.nextInt(1000)- 500, random.nextInt(1000) - 500);
        for (int i = 0; i < random.nextInt(16 * 16 * 256); i++)
            original.setTrue(random.nextInt(16), random.nextInt(16), random.nextInt(256));
        byte[] serializedBytes = SerializationUtils.serialize(original);
        PrimitiveChunkStore deserialized = (PrimitiveChunkStore)SerializationUtils.deserialize(serializedBytes);
        for (int y = 0; y < 256; y++)
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    Assert.assertEquals(original.isTrue(x, y, z), deserialized.isTrue(x, y, z));
    }
}
