package com.grinderwolf.swm.nms.v1171;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongArrayTag;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.core.version.legacy.LegacyCraftSlimeChunkSection;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.level.levelgen.*;

public class NMSSlimeChunk implements SlimeChunk {

    private LevelChunk chunk;

    public NMSSlimeChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public String getWorldName() {
        return chunk.level.serverLevelData.getLevelName();
    }

    @Override
    public int getX() {
        return chunk.getPos().x;
    }

    @Override
    public int getZ() {
        return chunk.getPos().z;
    }

    @Override
    public SlimeChunkSection[] getSections() {
        SlimeChunkSection[] sections = new SlimeChunkSection[16];
        ThreadedLevelLightEngine lightEngine = (ThreadedLevelLightEngine) chunk.level.getLightEngine();

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            LevelChunkSection section = chunk.getSections()[sectionId];

            if (section != null) {
                section.recalcBlockCounts();

                if (!section.isEmpty()) { // If the section is empty, just ignore it to save space
                    // Block Light Nibble Array
                    NibbleArray blockLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

                    // Sky light Nibble Array
                    NibbleArray skyLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

                    // Tile/Entity Data

                    // Block Data
                    PalettedContainer<?> dataPaletteBlock = section.getStates();
                    net.minecraft.nbt.CompoundTag blocksCompound = new net.minecraft.nbt.CompoundTag();
                    dataPaletteBlock.write(blocksCompound, "Palette", "BlockStates");
                    net.minecraft.nbt.ListTag paletteList = blocksCompound.getList("Palette", 10);
                    ListTag<CompoundTag> palette = (ListTag<CompoundTag>) Converter.convertTag("", paletteList);
                    long[] blockStates = blocksCompound.getLongArray("BlockStates");

                    sections[sectionId] = new LegacyCraftSlimeChunkSection(null, null, palette, blockStates, blockLightArray, skyLightArray);
                }
            }
        }

        return sections;
    }

    @Override
    public CompoundTag getHeightMaps() {
        // HeightMap
        CompoundMap heightMaps = new CompoundMap();

        for (var entry : chunk.heightmaps.entrySet()) {
            Heightmap.Types type = entry.getKey();
            Heightmap map = entry.getValue();

            heightMaps.put(type.getSerializedName(), new LongArrayTag(type.getSerializedName(), map.getRawData()));
        }

        return new CompoundTag("", heightMaps);
    }

    @Override
    public int[] getBiomes() {
        return chunk.getBiomes().writeBiomes();
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        List<CompoundTag> tileEntities = new ArrayList<>();

        for (BlockEntity entity : chunk.getBlockEntities().values()) {
            net.minecraft.nbt.CompoundTag entityNbt = new  net.minecraft.nbt.CompoundTag();
            entity.save(entityNbt);
            tileEntities.add((CompoundTag) Converter.convertTag(entityNbt.getString("name"), entityNbt));
        }

        return tileEntities;
    }

    @Override
    public List<CompoundTag> getEntities() {
        List<CompoundTag> entities = new ArrayList<>();

        PersistentEntitySectionManager<Entity> entityManager = chunk.level.entityManager;

        for (Entity entity : entityManager.getEntityGetter().getAll()) {
            ChunkPos chunkPos = chunk.getPos();
            ChunkPos entityPos = entity.chunkPosition();

            if (chunkPos.equals(entityPos)) {
                net.minecraft.nbt.CompoundTag entityNbt = new net.minecraft.nbt.CompoundTag();
                if (entity.save(entityNbt)) {
                    //chunk.b(true); setLightCorrect?
                    entities.add((CompoundTag) Converter.convertTag("", entityNbt));
                }
            }
        }
        return entities;
    }

    public void setChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }

    public LevelChunk getChunk() {
        return chunk;
    }
}
