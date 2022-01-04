import java.nio.file.*

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "com.owen1212055"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    implementation(project(":core"))

    compileOnly(project(":v117nms"))

    implementation("org.bstats:bstats-bukkit:2.2.1")
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("org.mongodb:mongo-java-driver:3.12.10")
    implementation("io.lettuce:lettuce-core:6.1.5.RELEASE")
    implementation("org.spongepowered:configurate-yaml:3.7")
    compileOnly("commons-io:commons-io:2.11.0") // Exposed in server


    annotationProcessor("org.projectlombok:lombok:1.18.22")
}


tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)


        val mainFile = file("../v117nms/build/libs/v117nms-1.0.jar").toPath() // TODO: Figure out better way
        if (Files.exists(mainFile)) {
            Files.copy(
                mainFile,
                file("src/main/resources/nms-117.jar").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    shadowJar {
        dependencies {
            exclude(project(":v117nms"))
            relocate("org.bstats", "com.grinderwolf.${rootProject.name}.libs.bstats")
        }
    }

}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}




