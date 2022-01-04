plugins {
    java
    `java-library`
}

group = "com.grinderwolf"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    compileOnlyApi("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")
    api("org.projectlombok:lombok:1.18.22")
    api(files("libs/flow-nbt-2.0.2.jar"))
    api("com.github.luben:zstd-jni:1.5.1-1")

    annotationProcessor("org.projectlombok:lombok:1.18.22")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

