plugins {
    id("java-library")
    id("maven-publish")
}

dependencyManagement {
    dependencies {
        // Boot 2.6.15 BOM이 Mockito(spring-boot-starter-test) 경유로 net.bytebuddy 버전을 낮게
        // 강제 고정하는 걸 막기 위해 우리가 실제로 쓰는 버전으로 다시 고정한다.
        dependency("net.bytebuddy:byte-buddy:1.14.18")
        dependency("net.bytebuddy:byte-buddy-agent:1.14.18")
    }
    imports {
        // 여기서 참조하는 API(스프링 AOP, WebMvcConfigurer, ConfigurationProperties/Binder 등)는
        // Spring Framework 5.3(Boot 2.6)과 6(Boot 3.x) 사이에 시그니처가 바뀌지 않는 부분만 골라
        // 썼다 - core.jar 하나로 :starter-boot2/:starter-boot3 양쪽 런타임에서 재사용 가능하다.
        // (javax/jakarta.servlet.* 관련 타입은 core에서 클래스 리터럴로 직접 참조하지 않고
        // ReflectiveTypeMatcher로 이름 매칭한다 - 그래서 servlet-api 자체엔 의존성이 없다.)
        //
        // 컴파일 타임 기준을 2.6(더 낮은/보수적인 쪽)으로 잡는다 - 3.3.5의 Gradle Module Metadata는
        // "JVM 17+ 필요"로 표시돼 있어서, core가 Java 8을 타깃으로 하는 이상 애초에 compileOnly로도
        // 끌어올 수 없다(Gradle이 변형(variant) 해석 단계에서 막음).
        mavenBom("org.springframework.boot:spring-boot-dependencies:2.6.15")
    }
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-aop")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")

    // @DeepTrace(self-invocation 계측)는 런타임 클래스 재정의가 필요해서, 호스트 클래스패스에
    // 이미 있는 jackson/spring-web과 달리 실제로 셰이딩 없이 그대로 딸려가는 진짜 런타임 의존성이다.
    // ByteBuddy는 Java 6+를 지원해서 core의 Java 8 타깃과도 호환된다.
    implementation("net.bytebuddy:byte-buddy:1.14.18")
    implementation("net.bytebuddy:byte-buddy-agent:1.14.18")

    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-aop")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

java {
    withSourcesJar()
    withJavadocJar()
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
                name.set("Request Flow Visualizer Core")
                description.set("request-flow-visualizer의 Spring Boot 버전 무관 공통 로직 (모델, 직렬화, AOP 포인트컷, DeepTrace 계측)")
            }
        }
    }
}
