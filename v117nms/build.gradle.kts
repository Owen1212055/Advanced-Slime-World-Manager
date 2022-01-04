plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.2.0"
}

group = "com.owen1212055"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.jpenilla.xyz/snapshots/")
}

dependencies {
    paperDevBundle("1.17.1-R0.1-SNAPSHOT")
    compileOnly(project(":core"))

    implementation(project(":api"))

    //implementation("xyz.jpenilla:reflection-remapper:0.1.0-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}