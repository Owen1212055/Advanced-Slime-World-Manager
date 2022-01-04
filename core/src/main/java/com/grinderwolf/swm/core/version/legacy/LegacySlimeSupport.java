package com.grinderwolf.swm.core.version.legacy;

import com.flowpowered.nbt.*;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.*;
import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.api.world.properties.*;
import com.grinderwolf.swm.core.version.*;
import com.grinderwolf.swm.core.version.legacy.reader.*;
import com.grinderwolf.swm.core.version.legacy.upgrade.*;

import java.util.*;

public class LegacySlimeSupport implements SlimeVersionSupport {

    public static final byte LAST_VERSION = 0x08;

    @Override
    public String getVersionName() {
        return "1.17-LEGACY PLATFORM";
    }

    @Override
    public int[] getSupportedVersions() {
        return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
    }

    @Override
    public SlimeWorld readWorld(SlimeLoader loader, String worldName, byte[] serializedWorld, SlimePropertyMap propertyMap, boolean readOnly) {
        try {
            LegacyCraftSlimeWorld world = SlimeWorldReaderRegistry.readWorld(loader, worldName, serializedWorld, propertyMap, readOnly);

            if (world.getVersion() < 9) {
                WorldUpgrader.upgradeWorld(world);
            }

            return world;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] serializeWorld(SlimeWorld world) {
        return ((LegacyCraftSlimeWorld) world).serialize();
    }

    @Override
    public SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) {
        return new LegacyCraftSlimeWorld(loader, worldName, new HashMap<>(), new CompoundTag("", new CompoundMap()), new ArrayList<>(), LAST_VERSION, propertyMap, readOnly, !readOnly);
    }

}
