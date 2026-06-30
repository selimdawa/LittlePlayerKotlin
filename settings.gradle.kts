pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        //maven(url = "https://jitpack.io")
        //maven(url = "https://maven.aliyun.com/repository/jcenter")
        maven { url = uri("https://jitpack.io") } // Updated syntax
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") } // Updated syntax
    }
}

rootProject.name = "Little Player"
include(":app")