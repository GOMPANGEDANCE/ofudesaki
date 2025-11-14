// 파일 경로: settings.gradle.kts

// 1. pluginManagement 블록을 파일의 가장 위에 하나만 둡니다.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// 2. 그 다음에 다른 설정들을 배치합니다.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ofudesaki_KR-main"
include(":app")

