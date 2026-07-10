# Sprint 7 — MVP Foundation: Member Removal + Procesión Service

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete the hermandad-service CRUD gap (member removal) and bootstrap the Procesión service at MVP scope.

**Architecture:** Member removal is a vertical slice in the existing hexagonal structure (soft delete). Procesión Service follows the same hexagonal pattern as hermandad-service: domain aggregate with ports, JPA adapter, outbox event publishing, REST controller, Keycloak auth.

**Tech Stack:** Spring Boot 4.1, Spring Data JPA, Flyway, Kafka (outbox), Keycloak Admin Client 24, springdoc-openapi, Eureka

**MVP Scope for Procesión Service:** No Recorrido, no GPS, no Kafka consumers for cross-service events. Just the aggregate CRUD with outbox publishing and basic auth.

---

### Task 1: Member Removal (Hermandad Service)

**Files:**
- Modify: `.../domain/repository/HermandadMemberRepository.java` — add `deleteById(UUID)`
- Modify: `.../adapter/outbound/persistence/HermandadMemberJpaRepository.java` — Spring Data provides `deleteById`
- Modify: `.../adapter/outbound/persistence/HermandadMemberRepositoryAdapter.java` — implement `deleteById`
- Modify: `.../application/service/HermandadService.java` — add `removeMember(UUID hermandadId, UUID memberId)`
- Modify: `.../adapter/inbound/rest/controller/HermandadController.java` — add `DELETE /api/hermandades/{hermandadId}/members/{memberId}`
- Test: `HermandadServiceTest.java` — add test for successful removal + not-found
- Test: `HermandadControllerTest.java` — add test for 204 + 404

**Step 1: Add `deleteById` to port interface**

`HermandadMemberRepository.java`: Add `void deleteById(UUID id)`.

**Step 2: Write failing service test**

In `HermandadServiceTest`:
```java
@Test
void removeMemberDeletesWhenFound() {
    UUID hermandadId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    var member = new HermandadMember(hermandadId, "user-1", HermandadRole.MUSICIAN);

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
    when(hermandadRepository.existsById(hermandadId)).thenReturn(true);

    hermandadService.removeMember(hermandadId, memberId);

    verify(memberRepository).deleteById(memberId);
}

@Test
void removeMemberThrowsWhenNotFound() {
    UUID hermandadId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();

    when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

    assertThrows(HermandadMemberNotFoundException.class,
        () -> hermandadService.removeMember(hermandadId, memberId));
}
```

**Step 3: Run test to verify it fails**

Run: `./gradlew :services:hermandad-service:test --tests "*HermandadServiceTest.removeMember*" --no-daemon`
Expected: Compilation error or test failure

**Step 4: Implement service method**

```java
public void removeMember(UUID hermandadId, UUID memberId) {
    var member = memberRepository.findById(memberId)
        .orElseThrow(() -> new HermandadMemberNotFoundException(memberId));
    if (!member.getHermandadId().equals(hermandadId)) {
        throw new HermandadMemberNotFoundException(memberId);
    }
    memberRepository.deleteById(memberId);
    // ponytail: no event published for member removal for now — MVP scope
}
```

**Step 5: Implement repository adapter**

`HermandadMemberRepositoryAdapter.java`:
```java
@Override
public void deleteById(UUID id) {
    memberJpaRepository.deleteById(id);
}
```

**Step 6: Add controller endpoint**

```java
@DeleteMapping("/{hermandadId}/members/{memberId}")
@PreAuthorize("@hermandadSecurity.isAdmin(#hermandadId)")
@ResponseStatus(NO_CONTENT)
public void removeMember(@PathVariable UUID hermandadId, @PathVariable UUID memberId) {
    hermandadService.removeMember(hermandadId, memberId);
}
```

**Step 7: Write controller test**

In `HermandadControllerTest`:
```java
@Test
void removeMemberReturns204() throws Exception {
    var hermandadId = UUID.randomUUID();
    var memberId = UUID.randomUUID();

    doNothing().when(hermandadService).removeMember(hermandadId, memberId);

    mockMvc.perform(delete("/api/hermandades/{hermandadId}/members/{memberId}", hermandadId, memberId)
            .with(jwt().authorities(new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
        .andExpect(status().isNoContent());
}

@Test
void removeMemberReturns404WhenNotFound() throws Exception {
    var hermandadId = UUID.randomUUID();
    var memberId = UUID.randomUUID();

    doThrow(new HermandadMemberNotFoundException(memberId))
        .when(hermandadService).removeMember(hermandadId, memberId);

    mockMvc.perform(delete("/api/hermandades/{hermandadId}/members/{memberId}", hermandadId, memberId)
            .with(jwt().authorities(new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
        .andExpect(status().isNotFound());
}
```

**Step 8: Run all tests**

Run: `./gradlew :services:hermandad-service:test --no-daemon`
Expected: All tests pass

**Step 9: Commit**

```bash
git add -A
git commit -m "feat: add member removal endpoint"
```

---

### Task 2: Procesión Service — Project Skeleton + Build Setup

**Files:**
- Create: `services/procesion-service/build.gradle.kts`
- Modify: `settings.gradle.kts` — add `procesion-service`
- Create: `services/procesion-service/src/main/java/com/repertorio/procesion/ProcesionServiceApplication.java`
- Create: `services/procesion-service/src/main/resources/application.yml`
- Create: `services/procesion-service/src/main/resources/bootstrap.yml`
- Create: `services/procesion-service/Dockerfile`

**Step 1: Add to settings.gradle.kts**

```kotlin
include("services:procesion-service")
```

**Step 2: Create build.gradle.kts**

Copied from `hermandad-service/build.gradle.kts`, changing:
- `serviceName = "procesion-service"`
- `spring.boot.group = "com.repertorio.procesion"`
- Remove hermandad-specific dependencies (Keycloak admin client not needed for MVP)

**Step 3: Create Application class**

```java
@SpringBootApplication
@EnableFeignClients
@EnableEurekaClient
@EnableMethodSecurity
@EnableScheduling
public class ProcesionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcesionServiceApplication.class, args);
    }
}
```

**Step 4: Create application.yml**

```yaml
spring:
  application:
    name: procesion-service
  datasource:
    url: jdbc:postgresql://localhost:5433/procesion-db
    username: procesion
    password: procesion
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    schemas: public
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8084/realms/semana-santa
          jwk-set-uri: http://keycloak:8084/realms/semana-santa/protocol/openid-connect/certs

server:
  port: 8082

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

**Step 5: Create bootstrap.yml**

```yaml
spring:
  cloud:
    compatibility-verifier:
      enabled: false
```

**Step 6: Create Dockerfile**

Same pattern as `hermandad-service/Dockerfile`.

**Step 7: Verify build compiles**

Run: `./gradlew :services:procesion-service:compileJava --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 8: Commit**

```bash
git add -A
git commit -m "feat: add procesion-service project skeleton"
```

---

### Task 3: Procesión Service — Domain Aggregate + Repository Port

**Files:**
- Create: `.../domain/model/ProcesionEstado.java` — enum
- Create: `.../domain/model/Procesion.java` — aggregate root
- Create: `.../domain/repository/ProcesionRepository.java` — port
- Create: `.../domain/event/ProcesionCreatedEvent.java` — domain event
- Create: `.../domain/event/ProcesionEstadoChangedEvent.java` — domain event

**Step 1: Create ProcesionEstado enum**

```java
public enum ProcesionEstado {
    PLANIFICADA,      // Planned
    EN_CURSO,         // In progress
    FINALIZADA,       // Finished
    CANCELADA         // Cancelled
}
```

**Step 2: Create Procesion aggregate**

```java
@AggregateRoot
public class Procesion {

    private final UUID id;
    private final UUID hermandadId;
    private final LocalDate fecha;
    private final LocalTime hora;
    private ProcesionEstado estado;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor, getters
    // changeEstado(ProcesionEstado nuevoEstado) with state transition validation
    // Transitions: PLANIFICADA → EN_CURSO, EN_CURSO → FINALIZADA, any → CANCELADA
}
```

**Step 3: Create domain events**

`ProcesionCreatedEvent` and `ProcesionEstadoChangedEvent` implementing `DomainEvent` (from shared lib).

**Step 4: Create repository port**

```java
public interface ProcesionRepository {
    Procesion save(Procesion procesion);
    Optional<Procesion> findById(UUID id);
    Page<Procesion> findByHermandadId(UUID hermandadId, Pageable pageable);
    void deleteById(UUID id);
}
```

**Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Procesion domain aggregate and repository port"
```

---

### Task 4: Procesión Service — JPA Adapter + Flyway Migration

**Files:**
- Create: `.../adapter/outbound/persistence/ProcesionEntity.java`
- Create: `.../adapter/outbound/persistence/ProcesionJpaRepository.java`
- Create: `.../adapter/outbound/persistence/ProcesionRepositoryAdapter.java`
- Create: `.../adapter/outbound/persistence/JpaProcesionMapper.java`
- Create: `.../resources/db/migration/V1__create_procesion_table.sql`

**Step 1: Create Flyway migration**

```sql
CREATE TABLE procesion (
    id UUID PRIMARY KEY,
    hermandad_id UUID NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADA',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_procesion_hermandad_id ON procesion(hermandad_id);
```

**Step 2: Write failing adapter test**

```java
@ExtendWith(MockitoExtension.class)
class ProcesionRepositoryAdapterTest {
    // Test save -> maps entity -> saves -> returns domain
    // Test findById -> found -> returns Optional.of(domain)
    // Test findById -> not found -> returns Optional.empty()
}
```

**Step 3: Implement adapter**

Follow exact same pattern as `HermandadMemberRepositoryAdapter`.

**Step 4: Run test**

Run: `./gradlew :services:procesion-service:test --no-daemon`
Expected: All tests pass

**Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Procesion JPA adapter with Flyway migration"
```

---

### Task 5: Procesión Service — Service Layer + Outbox Publishing

**Files:**
- Create: `.../application/service/ProcesionService.java`
- Test: `ProcesionServiceTest.java`

**Step 1: Write failing test**

Test cases:
1. `createProcesionPersistsAndPublishesEvent()` — save, verify event published
2. `getProcesionByIdReturnsWhenFound()`
3. `getProcesionByIdThrowsWhenNotFound()`
4. `changeEstadoTransitionsCorrectly()` — PLANIFICADA → EN_CURSO, verify event
5. `changeEstadoRejectsInvalidTransition()` — CANCELADA → EN_CURSO throws
6. `listProcesionesByHermandadReturnsPage()`
7. `removeProcesionDeletes()`

**Step 2: Implement service**

```java
@Service
@RequiredArgsConstructor
public class ProcesionService {

    private final ProcesionRepository procesionRepository;
    private final DomainEventPublisher eventPublisher;

    public Procesion crearProcesion(UUID hermandadId, LocalDate fecha, LocalTime hora) {
        var procesion = Procesion.crear(hermandadId, fecha, hora);
        procesion = procesionRepository.save(procesion);
        eventPublisher.publish(new ProcesionCreatedEvent(procesion.getId(), hermandadId, fecha, hora));
        return procesion;
    }

    public Procesion obtenerProcesion(UUID id) {
        return procesionRepository.findById(id)
            .orElseThrow(() -> new ProcesionNotFoundException(id));
    }

    public Procesion cambiarEstado(UUID id, ProcesionEstado nuevoEstado) {
        var procesion = obtenerProcesion(id);
        var estadoAnterior = procesion.getEstado();
        procesion.cambiarEstado(nuevoEstado);
        procesion = procesionRepository.save(procesion);
        eventPublisher.publish(new ProcesionEstadoChangedEvent(id, procesion.getHermandadId(), estadoAnterior, nuevoEstado));
        return procesion;
    }

    public Page<Procesion> listarPorHermandad(UUID hermandadId, Pageable pageable) {
        return procesionRepository.findByHermandadId(hermandadId, pageable);
    }

    public void eliminarProcesion(UUID id) {
        if (procesionRepository.findById(id).isEmpty()) {
            throw new ProcesionNotFoundException(id);
        }
        procesionRepository.deleteById(id);
    }
}
```

**Step 3: Run test**

Run: `./gradlew :services:procesion-service:test --no-daemon`
Expected: All tests pass

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: add Procesion service layer with outbox event publishing"
```

---

### Task 6: Procesión Service — REST Controller + Exception Handler

**Files:**
- Create: `.../adapter/inbound/rest/dto/CreateProcesionRequest.java`
- Create: `.../adapter/inbound/rest/dto/ProcesionResponse.java`
- Create: `.../adapter/inbound/rest/dto/EstadoChangeRequest.java`
- Create: `.../adapter/inbound/rest/controller/ProcesionController.java`
- Create: `.../adapter/inbound/rest/GlobalExceptionHandler.java`
- Create: `.../adapter/config/SecurityConfig.java`
- Test: `ProcesionControllerTest.java`
- Test: `GlobalExceptionHandlerTest.java`

**Step 1: Create DTOs**

```java
public record CreateProcesionRequest(
    @NotNull UUID hermandadId,
    @NotNull LocalDate fecha,
    @NotNull LocalTime hora
) {}

public record ProcesionResponse(
    UUID id,
    UUID hermandadId,
    LocalDate fecha,
    LocalTime hora,
    String estado,
    Instant createdAt,
    Instant updatedAt
) {}

public record EstadoChangeRequest(
    @NotNull ProcesionEstado nuevoEstado
) {}
```

**Step 2: Write failing controller test**

Use `@WebMvcTest(ProcesionController.class)` with mocked service.

Test cases:
1. `createProcesionReturns201()` — POST with valid body, expect 201 + location header
2. `createProcesionReturns400WhenInvalid()` — POST with missing fields, expect 400
3. `getProcesionByIdReturns200()` — GET existing, expect 200 + body
4. `getProcesionByIdReturns404()` — GET non-existent, expect 404
5. `changeEstadoReturns200()` — PATCH estado, expect 200
6. `changeEstadoReturns400WhenInvalid()` — PATCH invalid transition, expect 400
7. `listByHermandadReturnsPage()` — GET with pageable, expect 200 + page
8. `deleteProcesionReturns204()` — DELETE, expect 204

**Step 3: Implement controller**

```java
@RestController
@RequestMapping("/api/procesiones")
@RequiredArgsConstructor
public class ProcesionController {

    private final ProcesionService procesionService;

    @PostMapping
    @ResponseStatus(CREATED)
    public ProcesionResponse crearProcesion(@Valid @RequestBody CreateProcesionRequest request) {
        var procesion = procesionService.crearProcesion(request.hermandadId(), request.fecha(), request.hora());
        return ProcesionResponse.from(procesion);
    }

    @GetMapping("/{id}")
    public ProcesionResponse obtenerProcesion(@PathVariable UUID id) {
        return ProcesionResponse.from(procesionService.obtenerProcesion(id));
    }

    @PatchMapping("/{id}/estado")
    public ProcesionResponse cambiarEstado(@PathVariable UUID id, @Valid @RequestBody EstadoChangeRequest request) {
        return ProcesionResponse.from(procesionService.cambiarEstado(id, request.nuevoEstado()));
    }

    @GetMapping
    public Page<ProcesionResponse> listarPorHermandad(
            @RequestParam UUID hermandadId,
            @PageableDefault(size = 20) Pageable pageable) {
        return procesionService.listarPorHermandad(hermandadId, pageable)
            .map(ProcesionResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void eliminarProcesion(@PathVariable UUID id) {
        procesionService.eliminarProcesion(id);
    }
}
```

**Step 4: Create exception handler**

Same pattern as `GlobalExceptionHandler` in hermandad-service:
- `ProcesionNotFoundException` → 404
- `IllegalArgumentException` → 400
- `MethodArgumentNotValidException` → 400
- `AccessDeniedException` → 403
- Generic `Exception` → 500

**Step 5: Create SecurityConfig**

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(GET, "/api/procesiones/**").authenticated()
                .requestMatchers(POST, "/api/procesiones/**").authenticated()
                .requestMatchers(PATCH, "/api/procesiones/**").authenticated()
                .requestMatchers(DELETE, "/api/procesiones/**").authenticated()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS))
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri("http://keycloak:8084/realms/semana-santa/protocol/openid-connect/certs").build();
    }
}
```

**Step 6: Run tests**

Run: `./gradlew :services:procesion-service:test --no-daemon`
Expected: All tests pass

**Step 7: Commit**

```bash
git add -A
git commit -m "feat: add Procesion REST controller with auth and exception handling"
```

---

### Task 7: Docker Compose — Procesión Service + DB

**Files:**
- Modify: `docker-compose.yml` — add `procesion-db` PostgreSQL service and `procesion-service`

**Step 1: Add procesion-db**

```yaml
procesion-db:
  image: postgres:16-alpine
  container_name: procesion-db
  environment:
    POSTGRES_DB: procesion-db
    POSTGRES_USER: procesion
    POSTGRES_PASSWORD: procesion
  ports:
    - "5433:5432"
  volumes:
    - procesion-db-data:/var/lib/postgresql/data
  networks:
    - semana-santa-network
```

**Step 2: Add procesion-service**

```yaml
procesion-service:
  build:
    context: ./services/procesion-service
  container_name: procesion-service
  ports:
    - "8082:8082"
  depends_on:
    - procesion-db
    - kafka
    - keycloak
    - discovery-server
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://procesion-db:5432/procesion-db
    SPRING_DATASOURCE_USERNAME: procesion
    SPRING_DATASOURCE_PASSWORD: procesion
    SPRING_KAFKA_PRODUCER_BOOTSTRAP_SERVERS: kafka:9092
    EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-server:8761/eureka/
    SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8084/realms/semana-santa
    SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8084/realms/semana-santa/protocol/openid-connect/certs
  networks:
    - semana-santa-network
```

**Step 3: Add volume**

```yaml
volumes:
  procesion-db-data:
```

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: add procesion-service and procesion-db to Docker Compose"
```

---

### Task 8: API Gateway — Procesión Routes

**Files:**
- Modify: `services/api-gateway/src/main/resources/application.yml` — add procesion-service route

**Step 1: Add route**

```yaml
- id: procesion-service
  uri: lb://procesion-service
  predicates:
    - Path=/api/procesiones/**
  filters:
    - TokenRelay
```

**Step 2: Commit**

```bash
git add -A
git commit -m "feat: add procesion-service routes to API Gateway"
```

---

## Summary

| Task | Effort | Dependencies |
|------|--------|-------------|
| 1. Member Removal | Small | None |
| 2. Procesión Skeleton | Medium | None |
| 3. Procesión Domain | Medium | Task 2 |
| 4. Procesión Persistence | Medium | Task 2, 3 |
| 5. Procesión Service | Medium | Task 3, 4 |
| 6. Procesión Controller | Medium | Task 5 |
| 7. Docker Compose | Small | Task 2 |
| 8. Gateway Routes | Trivial | Task 2 |

Total: ~1-2 sessions of work. Procesión Service follows the exact same hexagonal + outbox pattern as hermandad-service.
