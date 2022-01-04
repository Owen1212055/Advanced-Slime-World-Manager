package com.grinderwolf.swm.core.version;

import com.grinderwolf.swm.api.loaders.*;
import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.api.world.properties.*;
import com.grinderwolf.swm.core.*;

public interface SlimeVersionSupport {

    String getVersionName();

    int[] getSupportedVersions();

    SlimeWorld readWorld(SlimeLoader loader, String worldName, byte[] serializedWorld, SlimePropertyMap propertyMap, boolean readOnly);

    byte[] serializeWorld(SlimeWorld world);

    SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap);

}
