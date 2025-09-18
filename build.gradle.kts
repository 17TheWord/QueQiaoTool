import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    java
    `maven-publish`
}

group = property("projectGroup")!!
version = property("projectVersion")!!

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:${property("gsonVersion")}")
    implementation("commons-io:commons-io:${property("commonsIoVersion")}")
    implementation("org.projectlombok:lombok:${property("lombokVersion")}")
    implementation("org.yaml:snakeyaml:${property("snakeyamlVersion")}")
    implementation("org.java-websocket:Java-WebSocket:${property("javaWebSocketVersion")}")
    implementation("org.slf4j:slf4j-api:${property("slf4jApiVersion")}")
    testImplementation("org.slf4j:slf4j-simple:${property("slf4jSimpleVersion")}")

    annotationProcessor("org.projectlombok:lombok:${property("lombokVersion")}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junitJupiterApiVersion")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junitJupiterApiVersion")}")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Javadoc> {
    options {
        this as StandardJavadocDocletOptions
        encoding = "UTF-8"
        charSet = "UTF-8"
        links("https://docs.oracle.com/javase/8/docs/api/")
        if (JavaVersion.current().isJava9Compatible) {
            addBooleanOption("html5", true)
        }
    }
    classpath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/17TheWord/QueQiaoTool")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
