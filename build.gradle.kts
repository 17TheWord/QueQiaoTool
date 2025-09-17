import org.gradle.external.javadoc.StandardJavadocDocletOptions


plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.github.theword.queqiao"
version = "0.2.8"

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

    // 注解处理器
    annotationProcessor("org.projectlombok:lombok:${property("lombokVersion")}")

    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junitJupiterApiVersion")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junitJupiterApiVersion")}")
}

tasks.test {
    useJUnitPlatform()
}

// Shadow JAR 配置 (等同于 Maven Shade Plugin)
tasks.shadowJar {
    archiveClassifier.set("")

    // 排除签名文件
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    // 可选的包重定位 (对应Maven中注释的relocation)
    // relocate("org.slf4j", "com.github.theword.queqiao.shaded.slf4j")
}

tasks.withType<Javadoc> {
    // 设置 Javadoc 选项
    options {
        this as StandardJavadocDocletOptions
        // 设置字符编码
        encoding = "UTF-8"
        // 设置源文件编码
        charSet = "UTF-8"
        // 添加链接到外部 Javadoc
        links("https://docs.oracle.com/javase/8/docs/api/")
        // 如果是模块化项目，添加模块路径
        if (JavaVersion.current().isJava9Compatible) {
            addBooleanOption("html5", true)
        }
    }
    // 设置类路径
    classpath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}

// 发布配置
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            // 使用 Shadow JAR 作为主要构件
            artifact(tasks.shadowJar)
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/17TheWord/QueQiaoTool")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
}

// 确保构建时生成 Shadow JAR
tasks.build {
    dependsOn(tasks.shadowJar)
}
