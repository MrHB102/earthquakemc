plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.230-stable")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

version = "0.1.0"

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.test { useJUnitPlatform() }

// plugin.yml carries the version.
tasks.processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }
