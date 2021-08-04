package com.grinderwolf.swm.plugin.loaders.slime.impl;

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.github.luben.zstd.Zstd;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.plugin.loaders.slime.SlimeWorldReader;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class v10SlimeWorldFormat implements SlimeWorldReader {

    @Override
    public CraftSlimeWorld deserializeWorld(byte version, SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly)
            throws IOException, CorruptedWorldException {

        try {

            // World version
            byte worldVersion = dataStream.readByte();

            // Chunk
            short minX = dataStream.readShort();
            short minZ = dataStream.readShort();
            int width = dataStream.readShort();
            int depth = dataStream.readShort();

            if (width <= 0 || depth <= 0) {
                throw new CorruptedWorldException(worldName);
            }

            int bitmaskSize = (int) Math.ceil((width * depth) / 8.0D);
            byte[] chunkBitmask = new byte[bitmaskSize];
            dataStream.read(chunkBitmask);
            BitSet chunkBitset = BitSet.valueOf(chunkBitmask);

            int compressedChunkDataLength = dataStream.readInt();
            int chunkDataLength = dataStream.readInt();
            byte[] compressedChunkData = new byte[compressedChunkDataLength];
            byte[] chunkData = new byte[chunkDataLength];

            dataStream.read(compressedChunkData);

            // Tile Entities
            int compressedTileEntitiesLength = dataStream.readInt();
            int tileEntitiesLength = dataStream.readInt();
            byte[] compressedTileEntities = new byte[compressedTileEntitiesLength];
            byte[] tileEntities = new byte[tileEntitiesLength];

            dataStream.read(compressedTileEntities);

            // Entities
            byte[] compressedEntities = new byte[0];
            byte[] entities = new byte[0];

            boolean hasEntities = dataStream.readBoolean();
            if (hasEntities) {
                int compressedEntitiesLength = dataStream.readInt();
                int entitiesLength = dataStream.readInt();
                compressedEntities = new byte[compressedEntitiesLength];
                entities = new byte[entitiesLength];

                dataStream.read(compressedEntities);
            }

            // Extra NBT tag
            byte[] compressedExtraTag = new byte[dataStream.readInt()];
            byte[] extraTag = new byte[dataStream.readInt()];
            dataStream.read(compressedExtraTag);

            // World Map NBT tag
            byte[] compressedMapsTag = new byte[dataStream.readInt()];
            byte[] mapsTag = new byte[dataStream.readInt()];
            dataStream.read(compressedMapsTag);

            if (dataStream.read() != -1) {
                throw new CorruptedWorldException(worldName);
            }

            // Data decompression
            Zstd.decompress(chunkData, compressedChunkData);
            Zstd.decompress(tileEntities, compressedTileEntities);
            Zstd.decompress(entities, compressedEntities);
            Zstd.decompress(extraTag, compressedExtraTag);
            Zstd.decompress(mapsTag, compressedMapsTag);

            // Chunk deserialization
            Map<Long, SlimeChunk> chunks = readChunks(worldName, minX, minZ, width, depth, chunkBitset, chunkData);

            // Entity deserialization
            CompoundTag entitiesCompound = readCompoundTag(entities);

            if (entitiesCompound != null) {
                ListTag<CompoundTag> entitiesList = (ListTag<CompoundTag>) entitiesCompound.getValue().get("entities");

                for (CompoundTag entityCompound : entitiesList.getValue()) {
                    ListTag<DoubleTag> listTag = (ListTag<DoubleTag>) entityCompound.getAsListTag("Pos").get();

                    int chunkX = floor(listTag.getValue().get(0).getValue()) >> 4;
                    int chunkZ = floor(listTag.getValue().get(2).getValue()) >> 4;
                    long chunkKey = ((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX);
                    SlimeChunk chunk = chunks.get(chunkKey);

                    if (chunk == null) {
                        throw new CorruptedWorldException(worldName);
                    }

                    chunk.getEntities().add(entityCompound);
                }
            }

            // Tile Entity deserialization
            CompoundTag tileEntitiesCompound = readCompoundTag(tileEntities);

            if (tileEntitiesCompound != null) {
                ListTag<CompoundTag> tileEntitiesList = (ListTag<CompoundTag>) tileEntitiesCompound.getValue().get("tiles");
                for (CompoundTag tileEntityCompound : tileEntitiesList.getValue()) {
                    int chunkX = ((IntTag) tileEntityCompound.getValue().get("x")).getValue() >> 4;
                    int chunkZ = ((IntTag) tileEntityCompound.getValue().get("z")).getValue() >> 4;
                    long chunkKey = ((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX);
                    SlimeChunk chunk = chunks.get(chunkKey);

                    if (chunk == null) {
                        throw new CorruptedWorldException(worldName);
                    }

                    chunk.getTileEntities().add(tileEntityCompound);
                }
            }

            // Extra Data
            CompoundTag extraCompound = readCompoundTag(extraTag);

            if (extraCompound == null) {
                extraCompound = new CompoundTag("", new CompoundMap());
            }

            // World Maps
            CompoundTag mapsCompound = readCompoundTag(mapsTag);
            List<CompoundTag> mapList;

            if (mapsCompound != null) {
                mapList = (List<CompoundTag>) mapsCompound.getAsListTag("maps").map(ListTag::getValue).orElse(new ArrayList<>());
            } else {
                mapList = new ArrayList<>();
            }

            // World properties
            SlimePropertyMap worldPropertyMap = propertyMap;
            Optional<CompoundMap> propertiesMap = extraCompound
                    .getAsCompoundTag("properties")
                    .map(CompoundTag::getValue);

            if (propertiesMap.isPresent()) {
                worldPropertyMap = new SlimePropertyMap(propertiesMap.get());
                worldPropertyMap.merge(propertyMap); // Override world properties
            } else if (propertyMap == null) { // Make sure the property map is never null
                worldPropertyMap = new SlimePropertyMap();
            }

            return new CraftSlimeWorld(loader, worldName, chunks, extraCompound, mapList, worldVersion, worldPropertyMap, readOnly, !readOnly);
        } catch (EOFException ex) {
            throw new CorruptedWorldException(worldName, ex);
        }
    }

    private static int floor(double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    private static Map<Long, SlimeChunk> readChunks(String worldName, int minX, int minZ, int width, int depth, BitSet chunkBitset, byte[] chunkData) throws IOException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(chunkData));
        Map<Long, SlimeChunk> chunkMap = new HashMap<>();

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                int bitsetIndex = z * width + x;

                if (chunkBitset.get(bitsetIndex)) {
                    // Height Maps
                    byte[] heightMapsArray = new byte[dataStream.readInt()];
                    dataStream.read(heightMapsArray);
                    CompoundTag heightMaps = readCompoundTag(heightMapsArray);

                    // Height Maps might be null if empty
                    if (heightMaps == null) {
                        heightMaps = new CompoundTag("", new CompoundMap());
                    }

                    // Biome array
                    int[] biomes = new int[dataStream.readInt()];
                    for (int i = 0; i < biomes.length; i++) {
                        biomes[i] = dataStream.readInt();
                    }

                    // Chunk Sections
                    SlimeChunkSection[] sections = readChunkSections(dataStream);

                    chunkMap.put(((long) minZ + z) * Integer.MAX_VALUE + ((long) minX + x), new CraftSlimeChunk(worldName,minX + x, minZ + z,
                            sections, heightMaps, biomes, new ArrayList<>(), new ArrayList<>()));
                }
            }
        }

        return chunkMap;
    }

    private static int[] toIntArray(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
        int[] ret = new int[buf.length / 4];

        buffer.asIntBuffer().get(ret);

        return ret;
    }

    private static SlimeChunkSection[] readChunkSections(DataInputStream dataStream) throws IOException {
        byte chunkSectionAmount = dataStream.readByte();
        SlimeChunkSection[] chunkSectionArray = new SlimeChunkSection[chunkSectionAmount];
        byte[] sectionBitmask = new byte[(int) Math.ceil(chunkSectionAmount / 8.0D)];

        dataStream.read(sectionBitmask);
        BitSet sectionBitset = BitSet.valueOf(sectionBitmask);

        for (int i = 0; i < chunkSectionAmount; i++) {
            if (sectionBitset.get(i)) {
                // Block Light Nibble Array
                NibbleArray blockLightArray;

                if (dataStream.readBoolean()) {
                    byte[] blockLightByteArray = new byte[2048];
                    dataStream.read(blockLightByteArray);
                    blockLightArray = new NibbleArray((blockLightByteArray));
                } else {
                    blockLightArray = null;
                }

                ListTag<CompoundTag> paletteTag;
                long[] blockStatesArray;

                // Palette
                int paletteLength = dataStream.readInt();
                List<CompoundTag> paletteList = new ArrayList<>(paletteLength);
                for (int index = 0; index < paletteLength; index++) {
                    int tagLength = dataStream.readInt();
                    byte[] serializedTag = new byte[tagLength];
                    dataStream.read(serializedTag);

                    CompoundTag tag = readCompoundTag(serializedTag);
                    paletteList.add(tag);
                }

                paletteTag = new ListTag<>("", TagType.TAG_COMPOUND, paletteList);

                // Block states
                int blockStatesArrayLength = dataStream.readInt();
                blockStatesArray = new long[blockStatesArrayLength];

                for (int index = 0; index < blockStatesArrayLength; index++) {
                    blockStatesArray[index] = dataStream.readLong();
                }

                // Sky Light Nibble Array
                NibbleArray skyLightArray;

                if (dataStream.readBoolean()) {
                    byte[] skyLightByteArray = new byte[2048];
                    dataStream.read(skyLightByteArray);
                    skyLightArray = new NibbleArray(skyLightByteArray);
                } else {
                    skyLightArray = null;
                }

                chunkSectionArray[i] = new CraftSlimeChunkSection(null, null, paletteTag, blockStatesArray, blockLightArray, skyLightArray);
            }
        }

        return chunkSectionArray;
    }

    private static CompoundTag readCompoundTag(byte[] serializedCompound) throws IOException {
        if (serializedCompound.length == 0) {
            return null;
        }

        NBTInputStream stream = new NBTInputStream(new ByteArrayInputStream(serializedCompound), NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);

        return (CompoundTag) stream.readTag();
    }
}
