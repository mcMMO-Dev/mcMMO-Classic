/*
 * This file is part of SpoutPlugin.
 *
 * Copyright (c) 2011-2012, SpoutDev <http://www.spout.org/>
 * SpoutPlugin is licensed under the GNU Lesser General Public License.
 *
 * SpoutPlugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SpoutPlugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.nossr50.util.blockmeta;

import java.io.*;
import java.util.BitSet;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class McMMOSimpleRegionFile {
    private static final int SEGMENT_EXPONENT = 10; // Segments are 2^10 bytes long. (1024)
    // Chunk info
    private final int[] chunkSegmentIndex = new int[1024];
    private final int[] chunkNumBytes = new int[1024];
    private final int[] chunkNumSegments = new int[1024];

    // Segments
    private final BitSet segments = new BitSet();
    private int segmentExponent;
    private int segmentMask;

    // File location
    private final File parent;
    // File access
    private RandomAccessFile file;

    // Region index
    private final int rx;
    private final int rz;

    public McMMOSimpleRegionFile(File f, int rx, int rz) {
        this.rx = rx;
        this.rz = rz;
        this.parent = f;

        loadFile();
    }

    public synchronized final RandomAccessFile getFile() {
        if (file == null)
            loadFile();
        return file;
    }

    private synchronized void loadFile()
    {
        try {
            this.file = new RandomAccessFile(parent, "rw");

            // New file, write out header bytes
            if (file.length() < 4096 * 3) {
                for (int i = 0; i < 1024 * 3; i++) {
                    file.writeInt(0);
                }
                file.seek(4096 * 2);
                file.writeInt(SEGMENT_EXPONENT);
            }

            file.seek(4096 * 2);

            this.segmentExponent = file.readInt();
            this.segmentMask = (1 << segmentExponent) - 1;

            int reservedSegments = this.sizeToSegments(4096 * 3);
            segments.flip(0, reservedSegments);

            // Read in FAT header data
            file.seek(0);

            for (int i = 0; i < 1024; i++)
                chunkSegmentIndex[i] = file.readInt();

            for (int i = 0; i < 1024; i++) {
                chunkNumBytes[i] = file.readInt();
                chunkNumSegments[i] = sizeToSegments(chunkNumBytes[i]);
                markChunkSegments(i, true);
            }

            fixFileLength();
        }
        catch (IOException fnfe) {
            throw new RuntimeException(fnfe);
        }
    }

    public synchronized DataOutputStream getOutputStream(int x, int z) {
        int index = getChunkIndex(x, z); // Get chunk index
        return new DataOutputStream(new DeflaterOutputStream(new McMMOSimpleChunkBuffer(this, index))); // Our chunkbuffer contains a call to write when it is closed
    }

    public synchronized DataInputStream getInputStream(int x, int z) throws IOException {
        int index = getChunkIndex(x, z); // Get chunk index
        int byteLength = chunkNumBytes[index]; // Get byte length of data

        // No bytes
        if (byteLength == 0)
            return null;

        byte[] data = new byte[byteLength];

        getFile().seek(chunkSegmentIndex[index] << segmentExponent); // Seek to file location
        getFile().readFully(data); // Read in the data
        return new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)));
    }

    synchronized void write(int index, byte[] buffer, int size) throws IOException {
        int oldStart = chunkSegmentIndex[index]; // Get current index
        markChunkSegments(index, false); // Clear our old segments
        int start = findContiguousSegments(oldStart, size); // Find contiguous segments to save to
        getFile().seek(start << segmentExponent); // Seek to file location
        getFile().write(buffer, 0, size); // Write data
        // update in memory info
        chunkSegmentIndex[index] = start;
        chunkNumBytes[index] = size;
        chunkNumSegments[index] = sizeToSegments(size);
        // Mark segments in use
        markChunkSegments(index, true);
        // Write out updates header info
        writeFATHeader();
    }

    public synchronized void close() {
        try {
            if (file != null) {
                file.seek(4096 * 2);
                file.close();
            }

            file = null;
            segments.clear();
        }
        catch (IOException ioe) {
            throw new RuntimeException("Unable to close file", ioe);
        }
    }

    private synchronized void markChunkSegments(int index, boolean inUse) {
        // No bytes used
        if (chunkNumBytes[index] == 0)
            return;

        int start = chunkSegmentIndex[index];
        int end = start + chunkNumSegments[index];

        // If we are writing, assert we don't write over any in-use segments
        if (inUse)
        {
            int nextSetBit = segments.nextSetBit(start);
            if (nextSetBit != -1 && nextSetBit < end)
                throw new IllegalStateException("Attempting to overwrite an in-use segment");
        }

        segments.set(start, end, inUse);
    }

    private synchronized void fixFileLength() throws IOException {
        int fileLength = (int)getFile().length();
        int extend = -fileLength & segmentMask; // how many bytes do we need to be divisible by segment size

        // Go to end of file
        getFile().seek(fileLength);
        // Append bytes
        getFile().write(new byte[extend], 0, extend);
    }

    private synchronized int findContiguousSegments(int hint, int size) {
        int segments = sizeToSegments(size); // Number of segments we need

        // Check the hinted location (previous location of chunk) most of the time we can fit where we were.
        boolean oldFree = true;
        for (int i = hint; i < this.segments.size() && i < hint + segments; i++) {
            if (this.segments.get(i)) {
                oldFree = false;
                break;
            }
        }

        // We fit!
        if (oldFree)
            return hint;

        // Find somewhere to put us
        int start = 0;
        int current = 0;

        while (current < this.segments.size()) {
            boolean segmentInUse = this.segments.get(current); // check if segment is in use
            current++; // Move up a segment

            // Move up start if the segment was in use
            if (segmentInUse)
                start = current;

            // If we have enough segments now, return
            if (current - start >= segments)
                return start;
        }

        // Return the end of the segments (will expand to fit them)
        return start;
    }

    private synchronized int sizeToSegments(int size) {
        if (size <= 0)
            return 1;

        return ((size - 1) >> segmentExponent) + 1; // ((Size - 1) / segmentSize) + 1
    }

    private synchronized int getChunkIndex(int x, int z) {
        if (rx != (x >> 5) || rz != (z >> 5)) {
            throw new RuntimeException(x + ", " + z + " not in region " + rx + ", " + rz);
        }

        x = x & 0x1F; // 5 bits (mod 32)
        z = z & 0x1F; // 5 bits (mod 32)

        return (x << 5) + z; // x in the upper 5 bits, z in the lower 5 bits
    }

    private synchronized void writeFATHeader() throws IOException {
        getFile().seek(0);
        for (int i = 0; i < 1024; i++) {
            getFile().writeInt(chunkSegmentIndex[i]);
        }

        for (int i = 0; i < 1024; i++) {
            getFile().writeInt(chunkNumBytes[i]);
        }
    }
}
