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

- `core/` → `request-flow-visualizer-core`: 모델/직렬화/AOP 포인트컷/`@DeepTrace` 계측 등 Spring Boot
  버전과 무관한 공통 로직 (Java 8 타깃, javax/jakarta.servlet 어느 쪽에도 컴파일 의존성 없음).
  두 스타터가 공통으로 의존하는 내부 모듈 — 직접 의존성으로 추가할 일은 없음.
- `starter-boot3/` → `request-flow-visualizer-spring-boot-starter`: **Spring Boot 3.x / Java 17+**용
  스타터. `jakarta.servlet` 기반 Filter + `@AutoConfiguration` 등록.
- `starter-boot2/` → `request-flow-visualizer-spring-boot2-starter`: **Spring Boot 2.6.x / Java 8+**용
  스타터. `javax.servlet` 기반 Filter + `spring.factories` 등록.
- `sample-app/`: 동작을 눈으로 확인할 수 있는 데모 앱 (Order Controller→Service→Repository, Boot 3 기준)

두 스타터 모두 패키지·클래스명이 완전히 동일합니다(`io.github.wooongchan.requestflow.*`) — 어느 쪽을
쓰든 애플리케이션 코드(예: `@DeepTrace` import)는 똑같고, 좌표만 프로젝트의 Spring Boot 버전에 맞는
쪽으로 고르면 됩니다.

## 1. 적용법

### 1-1. 의존성 추가

GitHub에 공개된 저장소를 JitPack이 빌드해주므로, 별도 배포 없이 바로 가져다 쓸 수 있습니다
(JitPack이 처음 요청을 받으면 그 태그를 빌드하는 데 1~2분 정도 걸릴 수 있습니다).

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
    mavenCentral()
}

dependencies {
    implementation("com.github.dbsdndcks:request-flow-visualizer:v0.2.0")
}
```

로컬에서 라이브러리 자체를 수정하며 개발 중이라면 `mavenLocal()`이 더 빠릅니다:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :request-flow-visualizer-spring-boot-starter:publishToMavenLocal
```

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.wooongchan:request-flow-visualizer-spring-boot-starter:0.2.0")
}
```

> **주의**: JitPack의 `v0.2.0` 태그는 core/starter-boot2 분리 이전 버전입니다. Spring Boot 2.6
> 지원이 필요하면 아직은 위 `mavenLocal()` 방식으로만 받을 수 있고, 분리된 구조로 새 태그를 찍은
> 뒤에 JitPack 좌표도 이 문서에 갱신할 예정입니다.

### 1-1-b. Spring Boot 2.6.x / Java 8+ 프로젝트라면

좌표만 다르고 사용법(설정/애노테이션)은 완전히 동일합니다. 지금은 mavenLocal로만 받을 수 있습니다:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :request-flow-visualizer-spring-boot2-starter:publishToMavenLocal
```

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.wooongchan:request-flow-visualizer-spring-boot2-starter:0.2.0")
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

### 1-4. `@DeepTrace` — 클래스 내부 호출(self-invocation)까지 보기

기본 계측은 Spring AOP 프록시 기반이라 같은 클래스 안에서 `this.method()`로 부르는 내부 호출은
잡히지 않습니다. 내부 흐름까지 보고 싶은 클래스에는 `@DeepTrace`를 붙이세요 — 프로덕션 코드 변경은
이 애노테이션 한 줄이 전부입니다.

```java
import io.github.wooongchan.requestflow.annotation.DeepTrace;

@DeepTrace
@Service
public class UserService {
    public void doSomething() {
        validate(); // this.validate() — 애노테이션 없이는 안 잡히던 내부 호출도 계측됨
    }
    private void validate() { ... }
}
```

동작 방식: 앱 기동 시 `@DeepTrace` 클래스가 하나라도 있으면 ByteBuddy 에이전트를 self-attach해서
(별도 `-javaagent` 플래그 불필요) 해당 클래스의 바이트코드를 재정의합니다 — private 메서드를 포함한
모든 인스턴스 메서드 호출이 계측 대상이 됩니다. 그래서:

- `@DeepTrace` 클래스는 일반 프록시 기반 계측에서는 제외됩니다(중복 계측 방지). 진입 호출부터 내부
  호출까지 전부 바이트코드 계측 경로 하나로 통일됩니다.
- self-attach는 JDK 배포판/실행 환경에 따라 실패할 수 있습니다 — 실패해도 앱 기동은 막지 않고,
  경고 로그만 남긴 채 `@DeepTrace` 없이(빈 경계 계측은 그대로) 정상 동작합니다.
- 이 기능 때문에 `starter`는 처음으로 실제 런타임 의존성(`net.bytebuddy:byte-buddy`,
  `byte-buddy-agent`)을 갖습니다(그 전까지는 jackson/spring-web 등에 전부 `compileOnly`로만
  얹혀가서 런타임 의존성이 0개였습니다).
- static 메서드, 생성자, `equals`/`hashCode`/`toString`은 계측 대상에서 제외됩니다.

### 1-5. 그 외 알려진 한계

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

**현재 상태: GitHub에 공개(public) 저장소로 push되어 있고, JitPack으로 배포 중입니다**
(https://github.com/dbsdndcks/request-flow-visualizer, 태그 `v0.2.0`). 위 "1-1. 의존성 추가"의
JitPack 좌표를 그대로 쓰면 됩니다. 아래는 참고용 대안입니다.

### 2-1. JitPack — 지금 쓰는 방식

새 버전을 배포하려면 태그만 새로 만들어서 push하면 됩니다 (별도 배포 서버/계정 불필요, JitPack이
태그를 보고 자동으로 빌드):

```bash
git tag v0.3.0
git push origin v0.3.0
```

소비하는 프로젝트는 버전만 바꿔서 받으면 됩니다:

```kotlin
implementation("com.github.dbsdndcks:request-flow-visualizer:v0.3.0")
```

### 2-2. `mavenLocal()` — 라이브러리 자체를 수정 중일 때

각자 `publishToMavenLocal`을 실행해야 하지만, JitPack의 빌드 대기시간 없이 바로 반영됩니다.
라이브러리 코드를 고치면서 바로 테스트할 때 유용합니다.

```bash
./gradlew :request-flow-visualizer-spring-boot-starter:publishToMavenLocal
```

### 2-3. GitHub Packages — private을 유지해야 하는 경우

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
