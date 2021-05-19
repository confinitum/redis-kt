import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
/**
 * Gradle version 6.6+
 */
plugins {
    kotlin("jvm") version "1.5.0"
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka") version "1.4.30"
    signing
}
//see gradle.properties
val my_version: String by project
val kotlin_version: String by project
val kotlinx_coroutines: String by project
val dokka_version: String by project
val ktor_version: String by project
val junit5_version: String by project
val hamcrest_version: String by project
val logback_version: String by project
val kotest_version = "4.6.0"

group = "com.confinitum.common"
version = my_version
description = "Redis Client for Kotlin"

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

repositories {
    jcenter()
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlin_version}")
//    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines")
    implementation("io.ktor:ktor-network:$ktor_version")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<JavaCompile> {
    options.release.set(11)
}

java.withSourcesJar()

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}

/**
 * Testing
 */
sourceSets {
    getByName("test").java.srcDirs("src/test/kotlin")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    //common
    testImplementation("org.hamcrest:hamcrest:${hamcrest_version}")
    testImplementation("org.hamcrest:hamcrest-library:${hamcrest_version}")
    //Junit 5
    testImplementation("io.kotest:kotest-runner-junit5:${kotest_version}")
    testImplementation("io.kotest:kotest-assertions-core:${kotest_version}")
    testImplementation("io.kotest:kotest-framework-datatest:${kotest_version}")
//    testImplementation(platform("org.junit:junit-bom:${junit5_version}"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${kotlin_version}")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation( "ch.qos.logback:logback-classic:$logback_version")
}

/**
 * Publishing
 */
publishing {
    repositories {
        maven {
            name = "release"
            description = "Release repository"
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
            url = if(version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
//            credentials {
//                username = scdUserName
//                password = scdPassword
//            }
        }
    }
    publications {
        create<MavenPublication>("redisclient") {
            from(components["java"])
            pom {
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        name.set("Jacek Wicka")
                        email.set("jacek.wicka@confinitum.de")
                        organization.set("confinitum KG")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:confinitum/redis-kt.git")
                    developerConnection.set("scm:git:git@github.com:confinitum/redis-kt.git")
                    url.set("git@github.com:confinitum/redis-kt.git")
                }

            }
        }
    }
}

signing {
    setRequired({
        (project.extra["isReleaseVersion"] as Boolean) && gradle.taskGraph.hasTask("publish")
    })
    val signingKey: String? by project // ORG_GRADLE_PROJECT_signingKey
    val signingPassword: String? by project // ORG_GRADLE_PROJECT_signingPassword
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["redisclient"])
    }
}
