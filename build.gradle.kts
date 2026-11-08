plugins {
    java
}

group = "online.vanillaway"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")   // Paper API
    maven("https://repo.extendedclip.com/releases/")             // PlaceholderAPI
    maven("https://repo.opencollab.dev/main/")                    // Floodgate + Cumulus (Bedrock-формы)
}

dependencies {
    // Собираем под стабильный Paper API 1.21.4 — плагин использует только базовые вещи
    // и работает на новых версиях сервера (26.x) без пересборки.
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")    // тянет за собой Cumulus (формы)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("VWGlow-${project.version}.jar")
}
