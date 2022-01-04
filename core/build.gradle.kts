plugins {
    java
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "com.owen1212055"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    api(project(":api"))

    api("net.bytebuddy:byte-buddy:1.12.6")
    api("net.bytebuddy:byte-buddy-agent:1.12.6")

    annotationProcessor("org.projectlombok:lombok:1.18.22")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}