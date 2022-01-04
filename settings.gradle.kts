pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
        mavenCentral()
    }
}

rootProject.name = "aswm"

include("api", "core", "plugin", "v117nms")
