plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    id("io.gitlab.arturbosch.detekt").version("1.23.8")
    jacoco
    application
}

val detektVersion = "1.23.8"

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")
        resources.srcDir("src/main/resources")
    }
    test {
        kotlin.srcDir("src/test/kotlin")
        resources.srcDir("src/test/resources")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
    create("systemtest") {
        kotlin.srcDir("src/systemtest/kotlin")
        resources.srcDir("src/systemtest/resources")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                setUrl("https://sopra.se.cs.uni-saarland.de:51623/resources/jars")
                credentials {
                    username = "resources"
                    password = "[O0zRfLh9x?6}X)]Ww[LaC-{g2Sobs+A"
                }
                metadataSources {
                    artifact()
                }
            }
        }
        filter {
            includeGroup("selab.systemtest")
        }
    }
}

dependencies {
    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Detekt
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")

    // CLI
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")

    // JSON
    implementation("org.json:json:20250517")
    implementation("com.github.erosb:json-sKema:0.23.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    // System Tests
    "systemtestImplementation"("selab.systemtest:systemtest-api:0.7.5") { isChanging = true }
    "systemtestImplementation"("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    "systemtestImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

tasks.register("detectSuppressions") {
    val sources = listOf(
        sourceSets.main.get().kotlin,
        sourceSets.test.get().kotlin,
        sourceSets["systemtest"].kotlin
    )
    sources.forEach { sourceSet ->
        sourceSet.forEach { file ->
            val regex = Regex("@.*suppress(.*)")
            file.forEachLine { line ->
                if (regex.containsMatchIn(line.lowercase())) {
                    throw GradleException("Suppressions of analysis tools detected in the following file: " + file.path)
                }
            }
        }
    }
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

kotlin {
    jvmToolchain(21)
}

detekt {
    toolVersion = detektVersion
    config.setFrom("config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}

tasks.build {
    dependsOn(tasks["detectSuppressions"])
    dependsOn(tasks.javadoc)
    dependsOn(tasks.detektMain)
    dependsOn(tasks.detektTest)
    dependsOn(tasks["detektSystemtest"])
}

tasks.jar {
    archiveFileName.set("selab.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Main-Class" to "de.unisaarland.cs.se.selab.MainKt",
            "Entry-Point" to "de.unisaarland.cs.se.selab.MainKt",
            "Implementation-Title" to "SeLab",
            "Application-Name" to "SeLab",
            "Implementation-Version" to project.version,
        )
    }

    from(sourceSets.main.get().output)
    from(sourceSets.named("systemtest").get().output)

    dependsOn(configurations.compileClasspath)
    from({
        configurations.compileClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    dependsOn(configurations.named("systemtestCompileClasspath"))
    from({
        configurations.named("systemtestCompileClasspath").get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })

    doLast {
        copy {
            from(this@jar.archiveFile)
            into("$projectDir/libs/")
        }
    }
}

// Configure the main class for the application plugin
application {
    mainClass.set("de.unisaarland.cs.se.selab.MainKt")
}

tasks.register<JavaExec>("serverExec") {
    description = "Runs the simulation on the group jar with the parameters in gradle.properties"
    group = "systemtest"
    dependsOn(tasks.jar)
    classpath(files("libs/selab.jar"))
    mainClass.set("de.unisaarland.cs.se.selab.MainKt")
    args = listOf("--food", properties["FOOD"].toString(),
        "--restaurants", properties["RESTAURANTS"].toString(),
        "--scenario", properties["SCENARIO"].toString(),
        "--maxTicks", properties["MAX_TICKS"].toString(),
        "--logLevel", properties["LOG_LEVEL"].toString(),
        "--out", properties["OUT"].toString()
    )
}

tasks.register<JavaExec>("systemtestExec") {
    description = "Runs the registered system tests on the group jar"
    group = "systemtest"
    dependsOn(tasks.jar)
    classpath(files("libs/selab.jar"))
    mainClass.set("de.unisaarland.cs.se.selab.systemtest.selab26.MainKt")
    val toRun = "reference_implementation"
//    val toRun = "validation_mutants"
//    val toRun = "simulation_mutants"
    args = listOf(
        "--jar", "libs/selab.jar",
        "--run", toRun,
        "--timeout", "10",
//        "--debug", "1337"
    )
}
