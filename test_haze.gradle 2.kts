repositories {
    mavenCentral()
    google()
}

configurations {
    create("testConfig")
}

dependencies {
    "testConfig"("dev.chrisbanes.haze:haze:0.7.3")
}

tasks.register("resolveDependencies") {
    doLast {
        configurations.getByName("testConfig").files.forEach { println(it.name) }
    }
}
