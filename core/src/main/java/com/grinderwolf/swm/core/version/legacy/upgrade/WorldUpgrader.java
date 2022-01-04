package com.grinderwolf.swm.core.version.legacy.upgrade;

import com.grinderwolf.swm.core.version.legacy.*;
import com.grinderwolf.swm.core.version.legacy.upgrade.v117.v117WorldUpgrade;
import com.grinderwolf.swm.core.version.legacy.upgrade.v1_14.*;
import com.grinderwolf.swm.core.version.legacy.upgrade.v1_16.*;

import java.util.HashMap;
import java.util.Map;

public class WorldUpgrader {

    private static final Map<Byte, Upgrade> upgrades = new HashMap<>();

    static {
        upgrades.put((byte) 0x04, new v1_14WorldUpgrade());
        // Todo we need a 1_14_WorldUpgrade class as well for 0x05
        upgrades.put((byte) 0x06, new v1_16WorldUpgrade());
        upgrades.put((byte) 0x07, new v117WorldUpgrade());
    }

    public static void upgradeWorld(LegacyCraftSlimeWorld world) {

        for (byte ver = (byte) (world.getVersion() + 1); ver <= 0x08; ver++) {
            Upgrade upgrade = upgrades.get(ver);

            if (upgrade == null) {
                throw new IllegalStateException("Missing world upgrader for version " + ver + ". World will not be upgraded.");
            }

            upgrade.upgrade(world);
        }

        world.setVersion((byte) 0x08);
    }

}
