plugins {
    id("java-library")
    id("maven-publish")
}

dependencyManagement {
    dependencies {
        // Boot 2.6.15 BOM이 Mockito(spring-boot-starter-test) 경유로 net.bytebuddy 버전을
        // 1.11.22로 강제 고정해버려서, core가 실제로 쓰는 1.14.18과 byte-buddy/byte-buddy-agent
        // 버전이 어긋나 self-attach는 "성공"하지만 Advice 위빙이 조용히 안 먹는 문제가 있었다.
        // 명시적으로 다시 고정해서 core와 동일한 버전을 쓰도록 강제한다.
        dependency("net.bytebuddy:byte-buddy:1.14.18")
        dependency("net.bytebuddy:byte-buddy-agent:1.14.18")
    }
    imports {
        // 2.6 라인의 마지막 패치 버전.
        mavenBom("org.springframework.boot:spring-boot-dependencies:2.6.15")
    }
}

dependencies {
    api(project(":request-flow-visualizer-core"))

    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-aop")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-aop")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("Request Flow Visualizer Spring Boot 2 Starter")
                description.set("Spring 애플리케이션의 HTTP 요청 처리 흐름을 브라우저에서 시각화하는 개발자 도구 (Spring Boot 2.6.x / Java 8+)")
            }
        }
    }
}
