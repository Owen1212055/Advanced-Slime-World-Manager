package com.grinderwolf.swm.nms.v1171.injects;

import com.destroystokyo.paper.util.*;
import com.grinderwolf.swm.nms.v1171.*;
import com.mojang.datafixers.util.*;
import net.bytebuddy.implementation.bind.annotation.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;

import java.util.concurrent.*;

public class ChunkMapInject {

    public static boolean save(@SuperCall Callable<Boolean> zuperCall, @This ChunkMap zuperObject, @Argument(0) ChunkAccess chunkAccess) {
        System.out.println("HI");
        ServerLevel serverLevel = zuperObject.level;
        if (!(serverLevel instanceof CustomWorldServer)) {
            try {
                return zuperCall.call(); // Default logic
            } catch (Exception e) {
                SneakyThrow.sneaky(e);
            }
        }

        if (!(chunkAccess instanceof ImposterProtoChunk || chunkAccess instanceof LevelChunk) || !chunkAccess.isUnsaved()) {
            // We're only storing fully-loaded chunks that need to be saved
            return true;
        }

        LevelChunk chunk;

        if (chunkAccess instanceof ImposterProtoChunk protoChunk) {
            chunk = protoChunk.getWrapped();
        } else {
            chunk = (LevelChunk) chunkAccess;
        }

        ((CustomWorldServer) serverLevel).saveChunk(chunk);
        chunk.setUnsaved(false);

        return true;
    }

}
