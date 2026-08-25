plugins {
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

allprojects {
    group = "io.github.wooongchan"
    version = "0.3.0"

    repositories {
        mavenCentral()
    }
}

// core/starter-boot2는 Spring Boot 2.6 + Java 8 프로젝트에서도 써야 해서 Java 8로 컴파일한다.
// starter-boot3/sample-app은 지금까지처럼 Java 17을 그대로 쓴다.
val java8Modules = setOf("request-flow-visualizer-core", "request-flow-visualizer-spring-boot2-starter")

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    configure<JavaPluginExtension> {
        val target = if (name in java8Modules) JavaVersion.VERSION_1_8 else JavaVersion.VERSION_17
        sourceCompatibility = target
        targetCompatibility = target
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
