group = "mx.com.inftel.codegen"
version = "1.0.2"

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    kotlin("jvm") version "1.4.21"
    `maven-publish`
    signing
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

val fakeJavadoc by tasks.registering(Jar::class) {
    archiveBaseName.set("${project.name}-fake")
    archiveClassifier.set("javadoc")
    from(file("$projectDir/files/README"))
}

publishing {
    repositories {
        maven {
            setUrl(file("$projectDir/build/repo"))
        }
    }

    publications {
        create<MavenPublication>("codegenApt") {
            artifact(fakeJavadoc)
            from(components["java"])
        }
    }

    publications.withType<MavenPublication> {
        pom {
            name.set("${project.group}:${project.name}")
            description.set("Codegen APT Processor")
            url.set("https://github.com/santoszv/codegen-apt")
            inceptionYear.set("2021")
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                }
            }
            developers {
                developer {
                    id.set("santoszv")
                    name.set("Santos Zatarain Vera")
                    email.set("santoszv@inftel.com.mx")
                    url.set("https://www.inftel.com.mx")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/santoszv/codegen-apt")
                developerConnection.set("scm:git:https://github.com/santoszv/codegen-apt")
                url.set("https://github.com/santoszv/codegen-apt")
            }
        }
        signing.sign(this)
    }
}

signing {
    useGpgCmd()
}