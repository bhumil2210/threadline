plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(providers.gradleProperty("jdk").getOrElse("21").toInt())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    inputs.dir("../../scenarios/java").withPropertyName("scenarios")
    // Two carrier threads, so virtual-thread facts match the scenario.
    systemProperty("jdk.virtualThreadScheduler.parallelism", "2")
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
