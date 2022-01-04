package com.grinderwolf.swm.nms.v1171;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.flowpowered.nbt.*;
import com.google.common.util.concurrent.*;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.api.world.properties.*;
import com.grinderwolf.swm.core.version.legacy.*;
import com.mojang.serialization.*;
import lombok.*;
import net.minecraft.core.Registry;
import net.minecraft.core.*;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.util.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.dimension.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.*;
import org.bukkit.*;
import org.bukkit.event.world.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

public class CustomWorldServer extends ServerLevel {

    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SWM Pool Thread #%1$d").build());
    private static final TicketType<Unit> SWM_TICKET = TicketType.create("swm-chunk", (a, b) -> 0);

    private final LegacyCraftSlimeWorld slimeWorld;
    private final Object saveLock = new Object();
    private final BiomeSource defaultBiomeSource;

    private boolean ready = false;

    public CustomWorldServer(LegacyCraftSlimeWorld world, ServerLevelData worldData, ResourceKey<Level> worldKey,
                             ResourceKey<LevelStem> dimensionKey, DimensionType dimensionType, ChunkGenerator chunkGenerator,
                             org.bukkit.World.Environment environment) throws IOException {
        super(MinecraftServer.getServer(), MinecraftServer.getServer().executor,
                PlatformNMS.CONVERTABLE.createAccess(world.getName(), dimensionKey),
                worldData, worldKey, dimensionType, MinecraftServer.getServer().progressListenerFactory.create(11),
                chunkGenerator, false, 0, new ArrayList<>(), true, environment, null, null);

        this.slimeWorld = world;

        SlimePropertyMap propertyMap = world.getPropertyMap();

        this.serverLevelData.setDifficulty(Difficulty.valueOf(propertyMap.getValue(SlimeProperties.DIFFICULTY).toUpperCase()));
        this.serverLevelData.setSpawn(new BlockPos(propertyMap.getValue(SlimeProperties.SPAWN_X), propertyMap.getValue(SlimeProperties.SPAWN_Y), propertyMap.getValue(SlimeProperties.SPAWN_Z)), 0);
        super.setSpawnSettings(propertyMap.getValue(SlimeProperties.ALLOW_MONSTERS), propertyMap.getValue(SlimeProperties.ALLOW_ANIMALS));

        this.pvpMode = propertyMap.getValue(SlimeProperties.PVP);
        {
            String biomeStr = slimeWorld.getPropertyMap().getValue(SlimeProperties.DEFAULT_BIOME);
            Biome defaultBiome = registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY).get(new ResourceLocation(biomeStr));
            this.defaultBiomeSource = new BiomeSource(Collections.emptyList()) {
                @Override
                public Biome getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
                    return defaultBiome;
                }

                @Override
                protected Codec<? extends BiomeSource> codec() {
                    return null;
                }

                @Override
                public BiomeSource withSeed(long seed) {
                    return this;
                }
            };
        }
    }
    @Override
    public void save(@Nullable ProgressListener progressListener, boolean flush, boolean flag1) {
        if (!slimeWorld.isReadOnly() && !flag1) {
            Bukkit.getPluginManager().callEvent(new WorldSaveEvent(getWorld()));

            this.chunkSource.save(flush);
            this.serverLevelData.setWorldBorder(this.getWorldBorder().createSettings());
            this.serverLevelData.setCustomBossEvents(MinecraftServer.getServer().getCustomBossEvents().save());

            // Update level data
            net.minecraft.nbt.CompoundTag compound = new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.CompoundTag nbtTagCompound = serverLevelData.createTag(MinecraftServer.getServer().registryHolder, compound);
            slimeWorld.getExtraData().getValue().put(Converter.convertTag("LevelData", nbtTagCompound));

            if (MinecraftServer.getServer().isStopped()) { // Make sure the world gets saved before stopping the server by running it from the main thread
                save();

                // Have to manually unlock the world as well
                try {
                    slimeWorld.getLoader().unlockWorld(slimeWorld.getName());
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (UnknownWorldException ignored) {

                }
            } else {
                WORLD_SAVER_SERVICE.execute(this::save);
            }
        }
    }

    private void save() {
        synchronized (saveLock) { // Don't want to save the SlimeWorld from multiple threads simultaneously
            try {
                byte[] serializedWorld = slimeWorld.serialize();
                slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld, false);
            } catch (IOException | IllegalStateException ex) {
                ex.printStackTrace();
            }
        }
    }

    public ImposterProtoChunk getChunkCustom(ChunkPos pos) {
        SlimeChunk slimeChunk = slimeWorld.getChunk(pos.x, pos.z);
        LevelChunk chunk;

        if (slimeChunk instanceof NMSSlimeChunk) {
            chunk = ((NMSSlimeChunk) slimeChunk).getChunk();
        } else {
            if (slimeChunk == null) {

                // Biomes
                // Use the default biome source to automatically populate the map with the default biome.
                ChunkBiomeContainer biomeStorage = new ChunkBiomeContainer(MinecraftServer.getServer().registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY), this, pos,
                        chunkSource.getGenerator().getBiomeSource());

                // Tick lists
                ProtoTickList<Block> blockTickList = new ProtoTickList<>(
                        (block) -> block == null || block.defaultBlockState().isAir(), pos, this);
                ProtoTickList<Fluid> fluidTickList = new ProtoTickList<>(
                        (type) -> type == null || type == Fluids.EMPTY, pos, this);

                chunk = new LevelChunk(this, pos, biomeStorage, UpgradeData.EMPTY, blockTickList, fluidTickList,
                        0L, null, null);

                // Height Maps
//                HeightMap.a(chunk, ChunkStatus.FULL.h());
            } else {
                chunk = createChunk(slimeChunk);
            }

            slimeWorld.updateChunk(new NMSSlimeChunk(chunk));
        }

        return new ImposterProtoChunk(chunk);
    }

    private LevelChunk createChunk(SlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        ChunkPos pos = new ChunkPos(x, z);

        // Biomes
        int[] biomeIntArray = chunk.getBiomes();

        ChunkBiomeContainer biomeStorage = new ChunkBiomeContainer(MinecraftServer.getServer().registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY), this, pos,
                chunkSource.getGenerator().getBiomeSource(), biomeIntArray);

        // Tick lists
        ProtoTickList<Block> blockTickList = new ProtoTickList<>(
                (block) -> block == null || block.defaultBlockState().isAir(), pos, this);
        ProtoTickList<Fluid> fluidTickList = new ProtoTickList<>(
                (type) -> type == null || type == Fluids.EMPTY, pos, this);

        // Chunk sections
        LevelChunkSection[] sections = new LevelChunkSection[16];
        ThreadedLevelLightEngine lightEngine = (ThreadedLevelLightEngine) getLightEngine();

        lightEngine.retainData(pos, true);

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
                LevelChunkSection section = new LevelChunkSection(sectionId << 4);

                section.getStates().read((ListTag) Converter.convertTag(slimeSection.getPalette()), slimeSection.getBlockStates());

                if (slimeSection.getBlockLight() != null) {
                    lightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(pos, sectionId), Converter.convertArray(slimeSection.getBlockLight()), true);
                }

                if (slimeSection.getSkyLight() != null) {
                    lightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(pos, sectionId), Converter.convertArray(slimeSection.getSkyLight()), true);
                }

                section.recalcBlockCounts();
                sections[sectionId] = section;
            }
        }

        // Keep the chunk loaded at level 33 to avoid light glitches
        // Such a high level will let the server not tick the chunk,
        // but at the same time it won't be completely unloaded from memory
//        getChunkProvider().addTicket(SWM_TICKET, pos, 33, Unit.INSTANCE);

        Consumer<LevelChunk> loadEntities = (nmsChunk) -> {

            // Load tile entities
//            System.out.println("Loading tile entities for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());
            List<CompoundTag> tileEntities = chunk.getTileEntities();
            int loadedEntities = 0;

            if (tileEntities != null) {
                for (CompoundTag tag : tileEntities) {
                    Optional<String> type = tag.getStringValue("id");

                    // Sometimes null tile entities are saved
                    if (type.isPresent()) {
                        BlockPos blockPosition = new BlockPos(tag.getIntValue("x").get(), tag.getIntValue("y").get(), tag.getIntValue("z").get());
                        BlockState blockData = nmsChunk.getBlockState(blockPosition);
                        BlockEntity entity = BlockEntity.loadStatic(blockPosition, blockData, (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag));

                        if (entity != null) {
                            nmsChunk.addAndRegisterBlockEntity(entity);
                            loadedEntities++;
                        }
                    }
                }
            }

            // Load entities
            List<CompoundTag> entities = chunk.getEntities();
            loadedEntities = 0;

            if (entities != null) {
                this.entityManager.addLegacyChunkEntities(EntityType.loadEntitiesRecursive(entities
                                .stream()
                                .map((tag) -> (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag))
                                .collect(Collectors.toList()),
                        this));
            }
        };

        CompoundTag upgradeDataTag = ((LegacyCraftSlimeChunk) chunk).getUpgradeData();
        LevelChunk nmsChunk = new LevelChunk(this, pos, biomeStorage, upgradeDataTag == null ? UpgradeData.EMPTY : new UpgradeData((net.minecraft.nbt.CompoundTag) Converter.convertTag(upgradeDataTag), this), blockTickList, fluidTickList, 0L, sections, loadEntities);

        // Height Maps
        EnumSet<Heightmap.Types> heightMapTypes = nmsChunk.getStatus().heightmapsAfter();
        CompoundMap heightMaps = chunk.getHeightMaps().getValue();
        EnumSet<Heightmap.Types> unsetHeightMaps = EnumSet.noneOf(Heightmap.Types.class);

        for (Heightmap.Types type : heightMapTypes) {
            String name = type.getSerializedName();

            if (heightMaps.containsKey(name)) {
                LongArrayTag heightMap = (LongArrayTag) heightMaps.get(name);
                nmsChunk.setHeightmap(type, heightMap.getValue());
            } else {
                unsetHeightMaps.add(type);
            }
        }

        // Don't try to populate heightmaps if there are none.
        // Does a crazy amount of block lookups
        if (!unsetHeightMaps.isEmpty()) {
            Heightmap.primeHeightmaps(nmsChunk, unsetHeightMaps);
        }

        return nmsChunk;
    }

    public void saveChunk(LevelChunk chunk) {
        SlimeChunk slimeChunk = slimeWorld.getChunk(chunk.getPos().x, chunk.getPos().z);

        // In case somehow the chunk object changes (might happen for some reason)
        if (slimeChunk instanceof NMSSlimeChunk) {
            ((NMSSlimeChunk) slimeChunk).setChunk(chunk);
        } else {
            slimeWorld.updateChunk(new NMSSlimeChunk(chunk));
        }
    }

    public LegacyCraftSlimeWorld getSlimeWorld() {
        return slimeWorld;
    }
}