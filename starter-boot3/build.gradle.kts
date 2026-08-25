plugins {
    id("java-library")
    id("maven-publish")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.5")
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

// io.spring.dependency-management이 관리하는 버전은 pom.xml에는 정상 반영되지만
// Gradle Module Metadata 생성기는 이를 인식하지 못해 검증에 실패한다. pom.xml만으로 충분하므로 끈다.
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("Request Flow Visualizer Spring Boot Starter")
                description.set("Spring 애플리케이션의 HTTP 요청 처리 흐름을 브라우저에서 시각화하는 개발자 도구 (Spring Boot 3.x)")
            }
        }
    }
}
