# Spanish → English Code Realignment: procesion-service

## Scope

~80 Spanish identifiers, strings, and comments across **13 files** in procesion-service.
The service domain name (`procesion`) is intentional and correct (ubiquitous language for a Semana Santa app).
Everything else — methods, fields, enum values, messages, comments — goes to English.

This is a **single-phase full refactor**: Java identifiers, JSON fields, DB columns, URL paths, all at once.

## Breaking Changes (accepted)

| Change | Impact |
|--------|--------|
| `fecha` → `date`, `hora` → `time`, `estado` → `status` (JSON + DB + code) | API + DB breaking |
| `PATCH /api/procesiones/{id}/estado` → `/{id}/status` | URL path change |
| `nuevoEstado` → `newStatus` in request body | API field rename |
| Enum values `PLANIFICADA` etc → `PLANNED` etc | Event payload change |

---

## Changes Per File

### 1. `domain/model/ProcesionEstado.java` → rename to `ProcesionStatus.java`
- Delete old file, create new with:
  - Class name: `ProcesionStatus`
  - Enum values: `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

### 2. `domain/model/Procesion.java`
- `fecha` → `date`
- `hora` → `time`  
- `estado` → `status`
- `crear()` → `create()`
- `cambiarEstado(ProcesionEstado nuevoEstado)` → `changeStatus(ProcesionStatus newStatus)`
- `getFecha()` → `getDate()`
- `getHora()` → `getTime()`
- `getEstado()` → `getStatus()`
- Exception messages: English
- All enum refs: `PLANIFICADA` → `PLANNED`, etc.

### 3. `domain/model/ProcesionNotFoundException.java`
- `"Procesión no encontrada: "` → `"Procesion not found: "`

### 4. `domain/event/ProcesionEstadoChangedEvent.java` → `ProcesionStatusChangedEvent`
- Record name change
- `ProcesionEstado estadoAnterior` → `ProcesionStatus previousStatus`
- `ProcesionEstado nuevoEstado` → `ProcesionStatus newStatus`
- `"PROCESION_ESTADO_CHANGED"` → `"PROCESION_STATUS_CHANGED"`

### 5. `domain/event/ProcesionCreatedEvent.java`
- `fecha` → `date`
- `hora` → `time`

### 6. `application/service/ProcesionService.java`
- `crearProcesion()` → `createProcesion()`
- `obtenerProcesion()` → `getProcesion()`
- `cambiarEstado()` → `changeStatus()`
- `listarPorHermandad()` → `listByHermandad()`
- `eliminarProcesion()` → `deleteProcesion()`
- Variables: `estadoAnterior` → `previousStatus`, `nuevoEstado` → `newStatus`
- `ProcesionEstado` → `ProcesionStatus`

### 7. `adapter/inbound/rest/dto/EstadoChangeRequest.java` → `StatusChangeRequest`
- Record name change
- `ProcesionEstado nuevoEstado` → `ProcesionStatus newStatus`

### 8. `adapter/inbound/rest/dto/ProcesionResponse.java`
- `fecha` → `date`
- `hora` → `time`
- `estado` → `status`
- `getFecha()` → `getDate()`, `getHora()` → `getTime()`, `getEstado()` → `getStatus()`

### 9. `adapter/inbound/rest/dto/CreateProcesionRequest.java`
- `fecha` → `date`
- `hora` → `time`

### 10. `adapter/inbound/rest/controller/ProcesionController.java`
- Method renames match service
- `@PatchMapping("/{id}/estado")` → `"/{id}/status"`
- Swagger: `"procesión"` → `"procesion"` (remove accent)
- Log messages: English
- `request.nuevoEstado()` → `request.newStatus()`

### 11. `test/.../ProcesionServiceTest.java`
- All method/field/variable names as above
- `Procesion.crear()` → `Procesion.create()`
- Enum values, event field names

### 12. `test/.../ProcesionControllerTest.java`
- Method names: English
- URL `/{id}/estado` → `/{id}/status`
- JSON keys: `fecha` → `date`, `hora` → `time`, `nuevoEstado` → `newStatus`
- Enum values: `EN_CURSO` → `IN_PROGRESS`, `PLANIFICADA` → `PLANNED`, `FINALIZADA` → `COMPLETED`
- Error text: `"no encontrada"` → `"not found"`

### 13. New Flyway migration: `V2__rename_procesion_columns.sql`

```sql
ALTER TABLE procesion RENAME COLUMN fecha TO date;
ALTER TABLE procesion RENAME COLUMN hora TO time;
ALTER TABLE procesion RENAME COLUMN estado TO status;
```

### 14. `resources/db/migration/V1__create_procesion_table.sql`
- Do NOT edit (existing migrations are immutable)

---

## Files NOT touched (no Spanish content)
- `SecurityConfig.java`, `JwtAuthenticationConverter.java`, `DomainEventPublisherAdapter.java`
- `DomainEventPublisher.java`, `DomainEvent.java`, `ProcesionServiceApplication.java`
- `ProcesionRepository.java`, `ProcesionRepositoryAdapter.java`, `ProcesionJpaRepository.java`
- `application.yml`, `bootstrap.yml`, `Dockerfile`, `build.gradle.kts`

---

## Verification

1. `./gradlew :services:procesion-service:compileJava` — must compile clean
2. `./gradlew :services:procesion-service:test` — all 21 tests must pass
3. Grep for any remaining Spanish:
   ```
   grep -ri "fecha\|hora\|estado\|crear\|obtener\|cambiar\|listarPor\|eliminar\|nuevoEstado\|estadoAnterior\|PLANIFICADA\|EN_CURSO\|FINALIZADA\|CANCELADA\|Procesión\|encontrada" services/procesion-service/src/
   ```
   Should return zero matches (excluding V1 migration and intentional `procesion` domain name).
