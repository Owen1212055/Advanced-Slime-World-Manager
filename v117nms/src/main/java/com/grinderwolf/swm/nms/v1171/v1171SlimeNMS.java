package com.grinderwolf.swm.nms.v1171;

import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.core.*;
import com.grinderwolf.swm.core.version.*;
import com.grinderwolf.swm.core.version.legacy.*;
import com.grinderwolf.swm.nms.v1171.injects.*;
import net.bytebuddy.*;
import net.bytebuddy.agent.*;
import net.bytebuddy.asm.*;
import net.bytebuddy.dynamic.loading.*;
import net.bytebuddy.implementation.*;
import net.bytebuddy.matcher.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_17_R1.*;

public class v1171SlimeNMS implements SlimePlatformSupport {
    @Override
    public void inject() throws Exception {
        ByteBuddyAgent.install();

        PluginClassInjector injector = new PluginClassInjector();

        injector.addClass(ChunkMapInject.class);
        injector.addClass(ChunkMapInjectLoad.class);
        injector.addClass(CustomWorldServer.class);
        injector.inject(ClassInjector.UsingInstrumentation.Target.SYSTEM);
        new ByteBuddy()
                .redefine(ChunkMap.class)
                .method(ElementMatchers.named("a").and(ElementMatchers.takesArguments(ChunkAccess.class))).intercept(MethodDelegation.to(ChunkMapInject.class)) // save
                .method(ElementMatchers.named("f").and(ElementMatchers.takesArguments(ChunkPos.class))).intercept(MethodDelegation.to(ChunkMapInjectLoad.class)) // scheduleChunkLoad
                .make()
                .load(MinecraftServer.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }

    @Override
    public SlimeVersionSupport getVersion() {
        return new LegacySlimeSupport();
    }

    @Override
    public void registerWorld(SlimeWorld slimeWorld) {

    }

    @Override
    public SlimeWorld getSlimeWorld(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        if (!(craftWorld.getHandle() instanceof CustomWorldServer worldServer)) {
            return null;
        }

        return worldServer.getSlimeWorld();
    }

    //@Override
    //    public Object[] getDefaultWorlds() {
    //        WorldServer defaultWorld = nmsInstance.getDefaultWorld();
    //        WorldServer netherWorld = nmsInstance.getDefaultNetherWorld();
    //        WorldServer endWorld = nmsInstance.getDefaultEndWorld();
    //
    //        if (defaultWorld != null || netherWorld != null || endWorld != null) {
    //            return new WorldServer[] { defaultWorld, netherWorld, endWorld };
    //        }
    //
    //        // Returning null will just run the original load world method
    //        return null;
    //    }
    //
    //    @Override
    //    public boolean isCustomWorld(Object world) {
    //        return world instanceof CustomWorldServer;
    //    }
    //
    //    @Override
    //    public boolean skipWorldAdd(Object world) {
    //        if (!isCustomWorld(world) || nmsInstance.isLoadingDefaultWorlds()) {
    //            return false;
    //        }
    //
    //        CustomWorldServer worldServer = (CustomWorldServer) world;
    //        return !worldServer.isReady();
    //    }
    //
    //    @Override
    //    public Object getDefaultGamemode() {
    //        if (nmsInstance.isLoadingDefaultWorlds()) {
    //            return ((DedicatedServer) MinecraftServer.getServer()).getDedicatedServerProperties().o;
    //        }
    //
    //        return null;
    //    }
    //
    //    static void initialize(v1171SlimeNMS instance) {
    //        ClassModifier.setLoader(new CraftCLSMBridge(instance));
    //    }
}
