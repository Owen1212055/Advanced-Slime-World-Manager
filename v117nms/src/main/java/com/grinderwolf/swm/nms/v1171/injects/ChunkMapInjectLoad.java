package com.grinderwolf.swm.nms.v1171.injects;

import com.destroystokyo.paper.util.*;
import com.grinderwolf.swm.nms.v1171.*;
import com.mojang.datafixers.util.*;
import net.bytebuddy.implementation.bind.annotation.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;

import java.lang.reflect.*;
import java.util.concurrent.*;

public class ChunkMapInjectLoad {

    public static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkLoad(@Argument(0) ChunkPos pos, @This ChunkMap zuperObject, @SuperCall Callable<?> zuper) {
        System.out.println("hello from inject");
        ServerLevel serverLevel = zuperObject.level;
        if (!(serverLevel instanceof CustomWorldServer)) {
            try {
                return (CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>) zuper.call(); // Default logic
            } catch (Exception e) {
                SneakyThrow.sneaky(e);
            }
        }

        CustomWorldServer world = (CustomWorldServer) serverLevel;
        return CompletableFuture.completedFuture(Either.left(world.getChunkCustom(pos)));
    }
}
