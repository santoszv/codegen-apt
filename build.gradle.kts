group = "mx.com.inftel.codegen"
version = "1.0.15"

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.6.10"
    `maven-publish`
    signing
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val kotlinJavadoc by tasks.registering(Jar::class) {
    archiveBaseName.set(project.name)
    archiveClassifier.set("javadoc")
    from(file("$projectDir/javadoc/README"))
}

publishing {
    repositories {
        maven {
            setUrl(file("$projectDir/build/repo"))
//            setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
//            credentials {
//                username = ""
//                password = ""
//            }
        }
    }

    publications {
        create<MavenPublication>("codegenApt") {
            artifact(kotlinJavadoc)
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