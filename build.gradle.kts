plugins {
    application
    id("java")
}

application {
    mainClass.set("VM")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

group = "com.alexwyler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}



dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("commons-io:commons-io:2.20.0")
    implementation("it.unimi.dsi:fastutil:8.5.16")
    implementation("io.vavr:vavr:1.0.0-alpha-4")
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("org.jgrapht:jgrapht-core:1.5.2")
}