# Request Flow Visualizer

Spring 애플리케이션에서 HTTP 요청이 들어와 응답이 나가기까지 거치는 **Controller → Service →
Repository** 호출 흐름을, Swagger UI처럼 브라우저에서 시각적인 트리로 보여주는 로컬 개발용 도구입니다.
각 노드에서 클래스/메서드, 실제 파라미터 값, 리턴값(또는 예외), 소요 시간을 확인할 수 있습니다.

- 계측 방식: Spring AOP `@Around` (프록시 기반) — 사용자가 지정한 base package 안의
  `@Controller`/`@RestController`/`@Service`/`@Repository`/`@Component` 빈 호출 경계만 추적합니다.
  DispatcherServlet, Hibernate 등 프레임워크 내부는 계측하지 않습니다.
- 저장 방식: 인메모리 링버퍼(기본 최근 50개 요청). 별도 인프라(수집 서버, DB) 불필요.
- 노출 방식: 의존성만 추가하면 `/trace-viewer`에서 바로 확인 가능 (springdoc-openapi와 동일한 패턴).

## 모듈 구성

- `starter/` → `request-flow-visualizer-spring-boot-starter`: 실제 배포되는 라이브러리
- `sample-app/`: 동작을 눈으로 확인할 수 있는 데모 앱 (Order Controller→Service→Repository)

## 1. 적용법

### 1-1. 의존성 추가

아직 Maven Central에는 배포하지 않았으므로, 우선 로컬(`mavenLocal()`)에 배포해서 사용합니다.
(공개 배포 방법은 "2. 공유법" 참고)

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :request-flow-visualizer-spring-boot-starter:publishToMavenLocal
```

호스트 프로젝트(`build.gradle.kts`)에서:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.wooongchan:request-flow-visualizer-spring-boot-starter:0.1.0-SNAPSHOT")
}
```

### 1-2. 설정

`application.yml`에 계측 대상 base package를 **반드시** 지정해야 합니다 (비워두면 자동으로 비활성화되고
경고 로그가 남습니다).

```yaml
request-flow:
  enabled: true                 # 기본 true. 운영 프로파일에서는 false 권장
  base-packages:
    - com.mycompany.order       # 이 패키지 및 하위 패키지의 빈 호출만 추적
    - com.mycompany.user
  viewer-path: /trace-viewer    # 기본값
  max-traces: 50                # 최근 몇 건까지 보관할지
  max-collection-size: 50       # 컬렉션/맵 값 직렬화 시 최대 원소 수
  max-value-length: 5000        # 직렬화된 값 문자열 최대 길이
  mask-field-patterns:          # 필드명 부분일치(대소문자 무시) 시 값 마스킹
    - password
    - secret
    - token
```

메서드 인자 이름(`id`, `item` 등)이 `arg0`, `arg1`로 나온다면 호스트 프로젝트 컴파일 옵션에 `-parameters`를
추가하세요 (Spring Boot Gradle 플러그인은 기본으로 켜져 있는 경우가 많습니다).

### 1-3. 사용

앱을 실행하고 API를 몇 번 호출한 뒤, 브라우저에서 `http://localhost:8080/trace-viewer` 를 엽니다.
왼쪽에서 최근 요청을 고르면 오른쪽에 Controller→Service→Repository 트리가 펼쳐지고, 각 노드를 클릭하면
파라미터/리턴값/예외 정보를 볼 수 있습니다.

### 1-4. 알려진 한계

- **Self-invocation 미포집**: 같은 클래스 안에서 `this.method()`로 호출하면 AOP 프록시를 거치지 않아
  잡히지 않습니다.
- **`@Async`/WebFlux 미지원**: 스레드가 바뀌는 실행 경로에는 traceId가 전파되지 않습니다.
- **Spring MVC 비동기(`Callable`/`DeferredResult`) 미지원**.
- **운영 환경**: 계측 자체가 오버헤드이므로 `request-flow.enabled=false`로 끄거나, 아래처럼 의존성 자체를
  `local`/`dev` 프로파일에만 넣는 것을 권장합니다.

```kotlin
// 예: local 전용 구성으로 분리
val localImplementation by configurations.creating
configurations.implementation.get().extendsFrom(localImplementation)
// CI/운영 빌드에서는 -PwithLocalTools=false 등으로 스킵
```

## 2. 공유법

로컬에서만 쓰는 게 아니라 팀/외부에 공유하려면 아래 중 상황에 맞는 방법을 선택하세요. 난이도 순입니다.

### 2-1. `mavenLocal()` — 지금 바로, 같은 컴퓨터/팀원 각자 로컬에서

가장 빠르지만 각자 `publishToMavenLocal`을 실행해야 합니다. 소규모 팀 내부 실험 단계에 적합합니다.

```bash
./gradlew :request-flow-visualizer-spring-boot-starter:publishToMavenLocal
```

### 2-2. JitPack — 가장 빠른 "진짜 공유" (권장, 별도 계정/서버 불필요)

1. 이 프로젝트를 GitHub에 push하고 태그(예: `v0.1.0`)를 만듭니다.
2. 소비하는 프로젝트에서:

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
dependencies {
    implementation("com.github.<github-id>:request-flow-visualizer:v0.1.0")
}
```

JitPack이 태그를 보고 자동으로 빌드/배포해줍니다. 별도 배포 인프라나 계정 설정이 필요 없어 사내 공유에
가장 실용적입니다.

### 2-3. GitHub Packages — 사내에 GitHub Enterprise/Org를 이미 쓰는 경우

`starter/build.gradle.kts`의 `publishing.repositories`에 GitHub Packages 저장소를 추가하고
`GITHUB_TOKEN`으로 인증합니다. 소비자도 GitHub 인증 토큰이 있어야 `implementation`으로 받을 수 있어
JitPack보다 설정이 한 단계 더 필요합니다.

### 2-4. Maven Central — 공개 배포(범용 라이브러리로 키우고 싶을 때)

가장 정식이지만 준비물이 많습니다: Sonatype(Central Portal) 계정, `io.github.wooongchan` 네임스페이스
소유권 인증(GitHub 계정 인증으로 간단히 가능), GPG 서명 키, `starter/build.gradle.kts`에
`signing`/`nexus-publish` 플러그인 추가. 지금 단계에서는 생략했고, 실제로 여러 사람이 쓰기 시작해서
공개 배포가 필요해지면 별도로 진행하면 됩니다.

## 3. 로컬 개발/검증

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # 시스템 java가 PATH에 없다면 필요
./gradlew :request-flow-visualizer-spring-boot-starter:test
./gradlew :sample-app:bootRun
# 다른 터미널에서
curl http://localhost:8080/api/orders/1
open http://localhost:8080/trace-viewer
```
