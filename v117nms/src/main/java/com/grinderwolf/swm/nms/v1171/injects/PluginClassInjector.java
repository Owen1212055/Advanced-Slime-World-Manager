package com.grinderwolf.swm.nms.v1171.injects;

import net.bytebuddy.agent.*;
import net.bytebuddy.description.type.*;
import net.bytebuddy.dynamic.*;
import net.bytebuddy.dynamic.loading.*;

import java.io.*;
import java.lang.instrument.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class PluginClassInjector {

    private final Set<Class<?>> classes;

    public PluginClassInjector() {
        this.classes = new HashSet<>();
    }

    public boolean addClass(Class<?> clazz) {
        return classes.add(clazz);
    }

    private Map<? extends TypeDescription, byte[]> getTypes() {
        return classes.stream().collect(Collectors.toMap(TypeDescription.ForLoadedType::new, ClassFileLocator.ForClassLoader::read));
    }

    public void inject(ClassInjector.UsingInstrumentation.Target target) throws IOException {
        inject(target, ByteBuddyAgent.install());
    }

    public void inject(ClassInjector.UsingInstrumentation.Target target, Instrumentation instrumentation) throws IOException {
        File tempDir = Files.createTempDirectory("slimeInjector" + hashCode()).toFile();
        tempDir.deleteOnExit();

        inject(tempDir, target, instrumentation);
    }

    public void inject(File folder, ClassInjector.UsingInstrumentation.Target target, Instrumentation instrumentation) {
        ClassInjector injector = ClassInjector.UsingInstrumentation.of(folder, target, instrumentation);

        injector.inject(getTypes());
    }
}