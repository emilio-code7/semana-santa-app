# Repertorio Service Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build repertorio-service — global marcha catalog + cruceta (ordered marcha list per procession). Final MVP service.

**Architecture:** Hexagonal + DDD + outbox pattern, mirroring hermandad/procesion. Marcha is a standalone aggregate (global catalog). Cruceta is a per-procession ordered list referencing Marcha IDs. Events via outbox → Kafka.

**Tech Stack:** Java 21, Spring Boot 4.1 (modular `spring-boot-starter-webmvc`), JPA, Flyway, Kafka, Eureka, Spring Security + OAuth2 resource server. Package: `com.repertorio.marcha`. Port: 8083.

**Existing infra:** `postgres-repertorio` DB container (port 5433, database `repertorio_db`), gateway route for `/api/marchas/**`, prometheus config, service registered in settings.gradle.kts. Only `build.gradle.kts` with `// placeholder` exists.

---

### Task 1: Project Scaffold — Gradle + App Class + Config

**Files:**
- Create: `services/repertorio-service/build.gradle.kts`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/RepertorioServiceApplication.java`
- Create: `services/repertorio-service/src/main/resources/application.yml`
- Create: `services/repertorio-service/src/main/resources/bootstrap.yml`

**Step 1: Write `build.gradle.kts`**

```kotlin
plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.repertorio.marcha"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web (modular — no Tomcat + JPA extras)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Data
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Messaging
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Discovery
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Docs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.testcontainers:kafka:1.20.1")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.2")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

Run: `./gradlew :services:repertorio-service:build` — expect BUILD SUCCESSFUL (or at least compileJava NO-SOURCE until sources exist).

**Step 2: Write `RepertorioServiceApplication.java`**

```java
package com.repertorio.marcha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableMethodSecurity
public class RepertorioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RepertorioServiceApplication.class, args);
    }
}
```

**Step 3: Write `application.yml`**

```yaml
spring:
  application:
    name: repertorio-service
  datasource:
    url: jdbc:postgresql://localhost:5433/repertorio_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

server:
  port: 8083

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
```

**Step 4: Write `bootstrap.yml`**

```yaml
spring:
  application:
    name: repertorio-service
```

**Step 5: Verify**

Run: `./gradlew :services:repertorio-service:compileJava` — BUILD SUCCESSFUL (compilation passes, even if no real logic yet).

**Step 6: Commit**

```bash
git add services/repertorio-service/
git commit -m "feat(repertorio): scaffold project with Gradle, app class, config"
```

---

### Task 2: Domain Model — Marcha + BandType + Domain Events

**Files:**
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/BandType.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/Marcha.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/MarchaNotFoundException.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/event/MarchaAddedEvent.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/event/MarchaRemovedEvent.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/event/DomainEvent.java`
- Test: `services/repertorio-service/src/test/java/com/repertorio/marcha/domain/model/MarchaTest.java`

**Step 1: Write `BandType.java`**

```java
package com.repertorio.marcha.domain.model;

public enum BandType {
    BANDA_PALIO,
    AGRUPACION_MUSICAL,
    BANDA_CORNETAS
}
```

**Step 2: Write `DomainEvent.java`** (marker interface)

```java
package com.repertorio.marcha.domain.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    Instant occurredAt();
    String aggregateType();
    UUID aggregateId();
}
```

**Step 3: Write `Marcha.java`**

```java
package com.repertorio.marcha.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Marcha {

    private final UUID id;
    private String title;
    private String composer;
    private BandType bandType;
    private int durationSeconds;
    private Integer compositionYear;
    private String youtubeUrl;
    private Instant createdAt;
    private Instant updatedAt;

    public Marcha(String title, String composer, BandType bandType, int durationSeconds,
                  Integer compositionYear, String youtubeUrl) {
        this.id = UUID.randomUUID();
        this.title = requireNonBlank(title, "title");
        this.composer = requireNonBlank(composer, "composer");
        this.bandType = bandType;
        this.durationSeconds = durationSeconds;
        this.compositionYear = compositionYear;
        this.youtubeUrl = youtubeUrl;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Factory for JPA reconstruction
    protected Marcha() {}

    public static Marcha create(String title, String composer, BandType bandType,
                                 int durationSeconds, Integer compositionYear, String youtubeUrl) {
        return new Marcha(title, composer, bandType, durationSeconds, compositionYear, youtubeUrl);
    }

    public void update(String title, String composer, BandType bandType,
                       int durationSeconds, Integer compositionYear, String youtubeUrl) {
        this.title = requireNonBlank(title, "title");
        this.composer = requireNonBlank(composer, "composer");
        this.bandType = bandType;
        this.durationSeconds = durationSeconds;
        this.compositionYear = compositionYear;
        this.youtubeUrl = youtubeUrl;
        this.updatedAt = Instant.now();
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    // Getters
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getComposer() { return composer; }
    public BandType getBandType() { return bandType; }
    public int getDurationSeconds() { return durationSeconds; }
    public Integer getCompositionYear() { return compositionYear; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

**Step 4: Write domain events**

`MarchaAddedEvent.java`:
```java
package com.repertorio.marcha.domain.event;

import java.time.Instant;
import java.util.UUID;

public record MarchaAddedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID marchaId,
        String title,
        String composer,
        BandType bandType,
        Integer compositionYear,
        String youtubeUrl
) implements DomainEvent {
    public MarchaAddedEvent(UUID marchaId, String title, String composer,
                            BandType bandType, Integer compositionYear, String youtubeUrl) {
        this(UUID.randomUUID(), Instant.now(), marchaId, title, composer, bandType, compositionYear, youtubeUrl);
    }

    @Override
    public String aggregateType() { return "marcha"; }

    @Override
    public UUID aggregateId() { return marchaId(); }
}
```

`MarchaRemovedEvent.java`:
```java
package com.repertorio.marcha.domain.event;

import java.time.Instant;
import java.util.UUID;

public record MarchaRemovedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID marchaId,
        String title
) implements DomainEvent {
    public MarchaRemovedEvent(UUID marchaId, String title) {
        this(UUID.randomUUID(), Instant.now(), marchaId, title);
    }

    @Override
    public String aggregateType() { return "marcha"; }

    @Override
    public UUID aggregateId() { return marchaId(); }
}
```

**Step 5: Write `MarchaNotFoundException.java`**

```java
package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class MarchaNotFoundException extends RuntimeException {
    public MarchaNotFoundException(UUID id) {
        super("Marcha not found: " + id);
    }
}
```

**Step 6: Write failing domain test, then implement**

Skip for this task — domain tests against pure POJOs. Create basic test to verify.

**Step 7: Commit**

```bash
git add services/repertorio-service/src/main/java/com/repertorio/marcha/domain/
git commit -m "feat(repertorio): add Marcha aggregate, BandType enum, domain events"
```

---

### Task 3: Domain Model — Cruceta + CrucetaItem + Events

**Files:**
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/Cruceta.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/CrucetaItem.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/CrucetaNotFoundException.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/event/CrucetaDefinedEvent.java`

**Step 1: Write `CrucetaItem.java`**

```java
package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class CrucetaItem {
    private UUID id;
    private UUID marchaId;
    private int orderIndex;
    private String notes;

    public CrucetaItem(UUID marchaId, int orderIndex, String notes) {
        this.id = UUID.randomUUID();
        this.marchaId = marchaId;
        this.orderIndex = orderIndex;
        this.notes = notes;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getMarchaId() { return marchaId; }
    public int getOrderIndex() { return orderIndex; }
    public String getNotes() { return notes; }
}
```

**Step 2: Write `Cruceta.java`**

```java
package com.repertorio.marcha.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Cruceta {

    private final UUID id;
    private final UUID procesionId;
    private List<CrucetaItem> items;
    private Instant createdAt;
    private Instant updatedAt;

    public Cruceta(UUID procesionId, List<CrucetaItem> items) {
        this.id = UUID.randomUUID();
        this.procesionId = procesionId;
        setItems(items);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void redefine(List<CrucetaItem> newItems) {
        setItems(newItems);
        this.updatedAt = Instant.now();
    }

    private void setItems(List<CrucetaItem> items) {
        if (items == null) throw new IllegalArgumentException("items must not be null");
        // Validate no duplicate orderIndex
        var orderIndexes = items.stream().map(CrucetaItem::getOrderIndex).toList();
        if (orderIndexes.stream().distinct().count() != orderIndexes.size()) {
            throw new IllegalArgumentException("duplicate orderIndex values");
        }
        this.items = new ArrayList<>(items);
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getProcesionId() { return procesionId; }
    public List<CrucetaItem> getItems() { return List.copyOf(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean containsMarcha(UUID marchaId) {
        return items.stream().anyMatch(item -> item.getMarchaId().equals(marchaId));
    }
}
```

**Step 3: Write `CrucetaDefinedEvent.java`**

```java
package com.repertorio.marcha.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CrucetaDefinedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID crucetaId,
        UUID procesionId,
        int itemCount
) implements DomainEvent {
    public CrucetaDefinedEvent(UUID crucetaId, UUID procesionId, int itemCount) {
        this(UUID.randomUUID(), Instant.now(), crucetaId, procesionId, itemCount);
    }

    @Override
    public String aggregateType() { return "cruceta"; }

    @Override
    public UUID aggregateId() { return crucetaId(); }
}
```

**Step 4: Write `CrucetaNotFoundException.java`**

```java
package com.repertorio.marcha.domain.model;

import java.util.UUID;

public class CrucetaNotFoundException extends RuntimeException {
    public CrucetaNotFoundException(UUID procesionId) {
        super("Cruceta not found for procesion: " + procesionId);
    }
}
```

**Step 5: Commit**

```bash
git add services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/Cruceta.java
git add services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/CrucetaItem.java
git add services/repertorio-service/src/main/java/com/repertorio/marcha/domain/model/CrucetaNotFoundException.java
git add services/repertorio-service/src/main/java/com/repertorio/marcha/domain/event/CrucetaDefinedEvent.java
git commit -m "feat(repertorio): add Cruceta aggregate with ordered marcha items"
```

---

### Task 4: DB Migrations — Marcha + Cruceta + Seed Data

**Files:**
- Create: `services/repertorio-service/src/main/resources/db/migration/V1__create_marcha_table.sql`
- Create: `services/repertorio-service/src/main/resources/db/migration/V2__create_cruceta_tables.sql`
- Create: `services/repertorio-service/src/main/resources/db/migration/V3__seed_global_marchas.sql`

**Step 1: Write `V1__create_marcha_table.sql`**

```sql
CREATE TABLE marcha (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    composer VARCHAR(255) NOT NULL,
    band_type VARCHAR(30) NOT NULL,
    duration_seconds INTEGER NOT NULL,
    composition_year INTEGER,
    youtube_url VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_marcha_band_type ON marcha(band_type);
CREATE INDEX idx_marcha_composer ON marcha(composer);
```

**Step 2: Write `V2__create_cruceta_tables.sql`**

```sql
CREATE TABLE cruceta (
    id UUID PRIMARY KEY,
    procesion_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX idx_cruceta_procesion_id ON cruceta(procesion_id);

CREATE TABLE cruceta_item (
    id UUID PRIMARY KEY,
    cruceta_id UUID NOT NULL REFERENCES cruceta(id) ON DELETE CASCADE,
    marcha_id UUID NOT NULL,
    order_index INTEGER NOT NULL,
    notes VARCHAR(1000),
    UNIQUE(cruceta_id, order_index)
);

CREATE INDEX idx_cruceta_item_cruceta_id ON cruceta_item(cruceta_id);
CREATE INDEX idx_cruceta_item_marcha_id ON cruceta_item(marcha_id);
```

**Step 3: Write `V3__seed_global_marchas.sql`** — seed 15 iconic marchas

```sql
INSERT INTO marcha (id, title, composer, band_type, duration_seconds, composition_year, youtube_url, created_at, updated_at) VALUES
    ('a0000001-0000-0000-0000-000000000001', 'Amarguras', 'Manuel López Farfán', 'BANDA_PALIO', 420, 1919, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000002', 'Saeta', 'Joaquín Turina', 'BANDA_PALIO', 300, 1930, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000003', 'Virgen de la Macarena', 'Pedro Morales', 'BANDA_CORNETAS', 360, 1950, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000004', 'El Amor de Dios', 'Manuel López Farfán', 'BANDA_PALIO', 400, 1920, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000005', 'La Madrugá', 'Abel Moreno', 'BANDA_CORNETAS', 380, 1987, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000006', 'Coronación de la Macarena', 'Pedro Gámez Laserna', 'BANDA_PALIO', 450, 1964, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000007', 'Pasan los Campanilleros', 'Manuel López Farfán', 'AGRUPACION_MUSICAL', 350, 1925, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000008', 'Semana Santa en Sevilla', 'Manuel Font de Anta', 'BANDA_PALIO', 320, 1925, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000009', 'Cristo de la Expiración', 'Manuel Font Fernández', 'BANDA_CORNETAS', 280, 1945, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000010', 'Mektub', 'Abel Moreno', 'AGRUPACION_MUSICAL', 420, 1992, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000011', 'Al Santísimo Cristo del Calvario', 'Manuel Marvizón Carvallo', 'AGRUPACION_MUSICAL', 390, 2010, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000012', 'Nuestro Padre Jesús', 'José Albero Francés', 'BANDA_CORNETAS', 340, 2007, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000013', 'Hermanos Costaleros', 'Sergio Bueno', 'BANDA_PALIO', 370, 2015, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000014', 'Soleá dame la mano', 'Francisco Jesús Flores Matute', 'BANDA_PALIO', 410, 1999, NULL, NOW(), NOW()),
    ('a0000001-0000-0000-0000-000000000015', 'Reina de San Fernando', 'José de la Vega Sánchez', 'BANDA_PALIO', 330, 2014, NULL, NOW(), NOW());
```

**Step 4: Verify**

Run: `./gradlew :services:repertorio-service:flywayMigrate` — or just confirm migrations classpath is valid.

**Step 5: Commit**

```bash
git add services/repertorio-service/src/main/resources/db/
git commit -m "feat(repertorio): add Flyway migrations for marcha, cruceta, and seed data"
```

---

### Task 5: JPA Entities + Repository Adapters

**Files:**
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/persistence/MarchaEntity.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/persistence/CrucetaEntity.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/persistence/CrucetaItemEntity.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/persistence/MarchaJpaRepository.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/persistence/CrucetaJpaRepository.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/port/MarchaRepository.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/domain/port/CrucetaRepository.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/persistence/MarchaRepositoryAdapter.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/persistence/CrucetaRepositoryAdapter.java`

**Key patterns:**

- `MarchaEntity` is JPA entity mirroring `marcha` table with `@Enumerated(STRING)` for bandType
- `MarchaRepository` (port interface): `findAll(Specification)`, `findById`, `save`, `deleteById`, `existsById`
- `CrucetaJpaRepository`: `findByProcesionId`, custom delete by procesionId
- `CrucetaRepositoryAdapter` maps between domain Cruceta and JPA entities

**Important:** Domain entities (`Marcha`, `Cruceta`) are NOT JPA-annotated. JPA entities (`MarchaEntity`, `CrucetaEntity`) are separate adapter-layer classes with mapping logic in the adapters. This keeps domain pure.

Run: `./gradlew :services:repertorio-service:compileJava` — BUILD SUCCESSFUL.

**Commit:**
```bash
git add services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/persistence/
git add services/repertorio-service/src/main/java/com/repertorio/marcha/domain/port/
git commit -m "feat(repertorio): add JPA entities and repository adapters"
```

---

### Task 6: Application Services

**Files:**
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/application/service/MarchaService.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/application/service/CrucetaService.java`

**MarchaService** — CRUD + delete validation:
- `listMarchas(bandType, composer, query)` → filter by optional params, no pagination for MVP
- `getMarcha(id)` → find or throw
- `createMarcha(title, composer, bandType, durationSeconds, compositionYear, youtubeUrl)` → save, publish `MarchaAddedEvent`
- `updateMarcha(id, ...)` → update, save
- `deleteMarcha(id)` → check no cruceta references it → delete, publish `MarchaRemovedEvent`

**CrucetaService** — manage by procesionId:
- `getCruceta(procesionId)` → find or throw
- `defineCruceta(procesionId, items)` → create or replace, publish `CrucetaDefinedEvent`

**Commit:**
```bash
git add services/repertorio-service/src/main/java/com/repertorio/marcha/application/
git commit -m "feat(repertorio): add MarchaService and CrucetaService"
```

---

### Task 7: REST Controllers + DTOs + Security

**Files:**
- Create: DTOs — `MarchaRequest.java`, `MarchaResponse.java`, `CrucetaRequest.java`, `CrucetaResponse.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/inbound/rest/controller/MarchaController.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/inbound/rest/controller/CrucetaController.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/config/SecurityConfig.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/config/OpenApiConfig.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/inbound/rest/GlobalExceptionHandler.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/inbound/rest/dto/ApiError.java`

**SecurityConfig** follows hermandad/procesion pattern:
- `anyRequest().authenticated()` as base rule
- Public GET for: `/api/marchas`, `/api/marchas/{id}`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/**`
- Custom `JwtAuthenticationConverter` (can reuse or import from shared/common)
- `@PreAuthorize("@hermandadSecurity.isAdmin(#hermandadId)")` on cruceta mutations

**Files to create for JWT auth:**
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/config/security/JwtAuthenticationConverter.java` — copy from `hermandad-service/.../adapter/config/security/JwtAuthenticationConverter.java`, update package to `com.repertorio.marcha.adapter.config.security`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/config/security/RepertorioSecurityService.java` — `@Component("repertorioSecurity")`, extract `HERMANDAD_{id}_HERMANDAD_ADMIN` from JWT authorities (fast path only, no DB fallback)
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/config/security/SecurityConfig.java` — copy from hermandad, update package, keep same `anyRequest().authenticated()` base + public GET for `/api/marchas/**`

Use `@PreAuthorize("@repertorioSecurity.isAdmin(#hermandadId)")` on cruceta mutation endpoints.

For MVP, copy the converter + security config from hermandad-service into the repertorio package. Not shared — avoids touching shared/common for now. Refactor later if needed.

**RepertorioSecurityService** (JWT-only, no DB dependency):
```java
@Component("repertorioSecurity")
public class RepertorioSecurityService {
    public boolean isAdmin(UUID hermandadId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        var adminAuthority = "HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN";
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(adminAuthority));
    }
}
```

**GlobalExceptionHandler:** Return `ApiError` JSON, same pattern as hermandad/procesion.

**OpenApiConfig:** Bearer JWT security scheme pointing at gateway (localhost:8080).

Run: `./gradlew :services:repertorio-service:compileJava` — BUILD SUCCESSFUL.

**Commit:**
```bash
git add services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/
git commit -m "feat(repertorio): add REST controllers, DTOs, security config"
```

---

### Task 8: Outbox + Event Publishing

**Files:**
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/application/port/OutboxPublisher.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/outbox/OutboxEventEntity.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/outbox/OutboxEventJpaRepository.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/outbox/OutboxEventPublisher.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/outbox/OutboxPoller.java`
- Create: `services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/events/DomainEventPublisherAdapter.java`
- Create: `services/repertorio-service/src/main/resources/db/migration/V4__create_outbox_table.sql`

**Note:** Copy exact same pattern from procesion-service (`adapter/outbound/outbox/`). Reference:
- `procesion-service/.../outbox/OutboxEventEntity.java` — JPA entity for `outbox_event` table
- `procesion-service/.../outbox/OutboxEventJpaRepository.java` — `findTop100ByProcessedFalseOrderByCreatedAtAsc`
- `procesion-service/.../outbox/OutboxEventPublisher.java` — serializes to JSON with `ObjectMapper`
- `procesion-service/.../outbox/OutboxPoller.java` — `@Scheduled(fixedDelayString = "PT5S")` sends to Kafka topic `{aggregateType}-events`
- `procesion-service/.../events/DomainEventPublisherAdapter.java` — calls `ApplicationEventPublisher` + `OutboxPublisher`

**V4__create_outbox_table.sql** — identical to procesion V3.

Run: `./gradlew :services:repertorio-service:compileJava` — BUILD SUCCESSFUL.

**Commit:**
```bash
git add services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/outbox/
git add services/repertorio-service/src/main/java/com/repertorio/marcha/adapter/outbound/events/
git add services/repertorio-service/src/main/java/com/repertorio/marcha/application/port/
git add services/repertorio-service/src/main/resources/db/migration/V4__create_outbox_table.sql
git commit -m "feat(repertorio): add outbox pattern for event publishing"
```

---

### Task 9: Docker + Gateway Integration

**Files:**
- Modify: `docker-compose.yml` — add repertorio-service container
- Modify: `docker-compose.dev.yml` — add repertorio-service container
- Modify: `infrastructure/api-gateway/src/main/resources/application.yml` — add cruceta route
- Modify: `docker-compose.yml` (kafka-init section) — add `marcha-events` topic creation
- Modify: `docker-compose.dev.yml` (kafka-init section) — same

**Note:** Before docker-compose can build, a `Dockerfile` is needed in the service root. Same pattern as procesion-service:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Create: `services/repertorio-service/Dockerfile`

**Step 1: Add `repertorio-service` to `docker-compose.yml`**

```yaml
  repertorio-service:
    image: repertorio-service:latest
    container_name: repertorio-service
    depends_on:
      postgres-repertorio:
        condition: service_healthy
      redis:
        condition: service_started
      kafka:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-repertorio:5432/repertorio_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_KAFKA_PRODUCER_BOOTSTRAP_SERVERS: kafka:29092
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-server:8761/eureka/
    ports:
      - "8083:8083"
```

**Step 2: Add cruceta route to gateway**

```yaml
            - id: repertorio-cruceta
              uri: lb://repertorio-service
              predicates:
                - Path=/api/hermandades/{hermandadId}/procesiones/{procesionId}/cruceta/**
```

**Step 3: Add `marcha-events` to Kafka init topic creation**

In `docker-compose.yml`, add to the `kafka-init` command chain (before `echo 'All topics created.'`):

```
        kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic marcha-events --partitions 3 --replication-factor 1 &&
```

Same in `docker-compose.dev.yml` with `kafka-dev:29092` bootstrap server.

**Step 4: Build + verify**

```bash
./gradlew :services:repertorio-service:build
docker compose build repertorio-service
docker compose up -d repertorio-service
```

**Commit:**
```bash
git add docker-compose.yml docker-compose.dev.yml
git add infrastructure/api-gateway/src/main/resources/application.yml
git add infrastructure/kafka/
git commit -m "feat(repertorio): add Docker and gateway integration"
```

---

### Task 10: Tests — Domain Unit Tests

**Files:**
- Create: `services/repertorio-service/src/test/java/com/repertorio/marcha/domain/model/MarchaTest.java`
- Create: `services/repertorio-service/src/test/java/com/repertorio/marcha/domain/model/CrucetaTest.java`

**MarchaTest** (pure JUnit 5, no Spring):
- `createWithValidFields` — succeeds
- `createWithBlankTitle` — throws
- `createWithBlankComposer` — throws
- `updateChangesFields` — title, composer, etc. updated
- `updateWithBlankTitle` — throws

**CrucetaTest** (pure JUnit 5, no Spring):
- `createWithValidItems` — succeeds
- `createWithNullItems` — throws
- `createWithDuplicateOrderIndex` — throws
- `redefineReplacesItems` — items replaced
- `containsMarchaReturnsTrue` — when marchaId present
- `containsMarchaReturnsFalse` — when marchaId absent

Run: `./gradlew :services:repertorio-service:test --tests "*MarchaTest" --tests "*CrucetaTest"` — all pass.

**Commit:**
```bash
git add services/repertorio-service/src/test/
git commit -m "test(repertorio): add domain unit tests for Marcha and Cruceta"
```

---

### Task 11: Tests — Service Unit Tests

**Files:**
- Create: `services/repertorio-service/src/test/java/com/repertorio/marcha/application/service/MarchaServiceTest.java`
- Create: `services/repertorio-service/src/test/java/com/repertorio/marcha/application/service/CrucetaServiceTest.java`

Use `@ExtendWith(MockitoExtension.class)` + `@Mock` for repositories + `@InjectMocks` for services. Verify events were published.

Run: `./gradlew :services:repertorio-service:test --tests "*ServiceTest"` — all pass.

**Commit:**
```bash
git add services/repertorio-service/src/test/java/com/repertorio/marcha/application/
git commit -m "test(repertorio): add service layer unit tests"
```

---

### Task 12: Tests — Controller Slice Tests + Integration Tests

**Files:**
- Create: `services/repertorio-service/src/test/java/com/repertorio/marcha/adapter/inbound/rest/controller/MarchaControllerTest.java`
- Create: `services/repertorio-service/src/test/java/com/repertorio/marcha/adapter/inbound/rest/controller/CrucetaControllerTest.java`

Use `@WebMvcTest` + `@MockitoBean` for services. Test auth scenarios (public GET works, mutations return 401 without JWT).

**Commit:**
```bash
git commit -m "test(repertorio): add controller slice tests"
```
