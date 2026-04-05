plugins {
    java
}

group = "com.splatage"
version = "0.1.0-SNAPSHOT"

val paperApiVersion = "1.21.11-R0.1-SNAPSHOT"
val javaVersion = 21
val hikariVersion = "6.3.2"
val mysqlConnectorVersion = "9.6.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("com.mysql:mysql-connector-j:$mysqlConnectorVersion")

    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
