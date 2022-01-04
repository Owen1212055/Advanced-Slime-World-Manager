package com.grinderwolf.swm.core;

import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.core.version.*;
import org.bukkit.*;

public interface SlimePlatformSupport {

//    void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) throws IOException;
//    void generateWorld(SlimeWorld world);
//
//    SlimeWorld getSlimeWorld(World world);
//    byte getWorldVersion();
//
//    default CompoundTag convertChunk(CompoundTag chunkTag) {
//        return chunkTag;
//    }

    void inject() throws Exception;

    SlimeVersionSupport getVersion();

    void registerWorld(SlimeWorld slimeWorld);

    SlimeWorld getSlimeWorld(World world);
}
