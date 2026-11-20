import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test-junit"))
}

application {
    mainClass.set("edu.console.MainKt")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}

tasks.register<JavaExec>("sources") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("edu.console.MainKt")
    args("sources")
    if (project.hasProperty("fail")) args("fail")
    if (project.hasProperty("success")) args("success")
}

tasks.register<JavaExec>("duplicates") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("edu.console.MainKt")
    args(
        "duplicates",
        providers.gradleProperty("directory").getOrElse("src/main/resources"),
        providers.gradleProperty("seconds").getOrElse("10"),
    )
}
