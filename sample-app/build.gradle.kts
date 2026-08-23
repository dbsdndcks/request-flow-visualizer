plugins {
    id("java")
    id("org.springframework.boot") version "3.3.5"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.5")
    }
}

dependencies {
    implementation(project(":request-flow-visualizer-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
}

tasks.withType<JavaCompile> {
    // 뷰어에 파라미터 이름(id, item 등)이 arg0/arg1 대신 그대로 보이도록 명시적으로 설정
    options.compilerArgs.add("-parameters")
}
