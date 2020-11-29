group = "mx.com.inftel.codegen"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    kotlin("jvm") version "1.4.21"
    `maven-publish`
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        javaParameters = true
        jvmTarget = "11"
    }
}

publishing {
    repositories {
        maven {
            url = if (project.version.toString().endsWith("-SNAPSHOT")) {
                uri("https://nexus.inftelapps.com/repository/maven-snapshots/")
            } else {
                uri("https://nexus.inftelapps.com/repository/maven-releases/")
            }
            credentials {
                username = properties["inftel.nexus.username"]?.toString()
                password = properties["inftel.nexus.password"]?.toString()
            }
        }
    }
    publications {
        create<MavenPublication>("codegenApt") {
            from(components["java"])
        }
    }
}