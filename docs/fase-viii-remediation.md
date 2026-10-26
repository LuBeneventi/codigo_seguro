# Fase VIII — Remediation (Plan y Trazabilidad de Remediación)

**Proyecto:** Pedidos360 Coffee Store  
**Fecha:** 2026-10-19  
**Objetivo:** Establecer la cadena formal e inmutable de trazabilidad que vincula cada vulnerabilidad detectada (**Finding**) con su requisito de seguridad (**SEC-REQ**), el cambio de código atómico (**Commit**), la prueba de regresión automatizada (**TEST-SEC**) y el resultado final verificado (**PASS**).

$$\text{FINDING-XXX} \longrightarrow \text{SEC-REQ-YYY} \longrightarrow \text{Commit [SHA]} \longrightarrow \text{TEST-SEC-ZZZ} \longrightarrow \text{Resultado [PASS]}$$

---

## 1. Cadena de Trazabilidad por Vulnerabilidad Remediada

---

### Remediación FINDING-001
```text
FINDING-001 (Fuerza bruta sin bloqueo en login)
    ↓
SEC-REQ-001 (Bloqueo de cuenta tras 5 intentos fallidos y Rate Limiting)
    ↓
Commit a3f1c82: "fix(auth): implement account lockout after 5 failed attempts [SEC-REQ-001]"
    ↓
TEST-SEC-001 (Verificación de bloqueo tras intentos fallidos de login)
    ↓
PASS
```
- **Archivos modificados:** `api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java`
- **Cambio aplicado:** Integración de `attemptsCache` y `lockTimeCache` con `MAX_ATTEMPTS = 5` y ventana de bloqueo de 15 minutos respondiendo HTTP 429.

---

### Remediación FINDING-002
```text
FINDING-002 (Vigencia desmedida de token JWT de 30 días)
    ↓
SEC-REQ-003 / SEC-REQ-002 continuo (Expiración de token JWT de acceso <= 1 hora)
    ↓
Commit e7b4d19: "fix(auth): reduce access token expiration to 1 hour [SEC-REQ-003]"
    ↓
TEST-SEC-003 (Expiración de token JWT de acceso <= 1 hora)
    ↓
PASS
```
- **Archivos modificados:** `api_cloud/src/main/java/cl/duoc/api/auth/JwtService.java`
- **Cambio aplicado:** Sustitución de `now.plus(30, ChronoUnit.DAYS)` por `now.plus(1, ChronoUnit.HOURS)`.

---

### Remediación FINDING-003
```text
FINDING-003 (BOLA / IDOR en listado y consulta de órdenes)
    ↓
SEC-REQ-004 / SEC-REQ-003 continuo (Aislamiento de pedidos por titularidad del cliente)
    ↓
Commit 9c42e05: "fix(orders): enforce clienteId ownership on getOrderById and listOrders [SEC-REQ-004]"
    ↓
TEST-SEC-004a y TEST-SEC-004b (Aislamiento multi-inquilino y titularidad en órdenes)
    ↓
PASS
```
- **Archivos modificados:** `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java`
- **Cambio aplicado:** Validación obligatoria de titularidad sobre `clienteId` en `getOrderById()` y estructuración de consulta `findByClienteId` en `listOrders()`.

---

### Remediación FINDING-004
```text
FINDING-004 (Bypass de rol administrativo por cabecera X-User-Role)
    ↓
SEC-REQ-005 / SEC-REQ-004 continuo (Autorización estricta basada en claims del JWT)
    ↓
Commit 2b88f31: "fix(security): remove X-User-Role header reliance, use JWT authorities [SEC-REQ-005]"
    ↓
TEST-SEC-005a (Autorización basada en token en lugar de cabecera arbitraria)
    ↓
PASS
```
- **Archivos modificados:** `api_cloud/src/main/java/cl/duoc/api/auth/RoleHeaderFilter.java`
- **Cambio aplicado:** Supresión de lectura de `X-User-Role` y extracción segura de roles desde `SecurityContextHolder.getContext().getAuthentication()`.

---

### Remediación FINDING-005
```text
FINDING-005 (Mass Assignment en registro de usuarios)
    ↓
SEC-REQ-006 / SEC-REQ-005 continuo (DTO dedicado y rol asignado exclusivamente en servidor)
    ↓
Commit 5f10a74: "fix(auth): introduce RegisterRequestDto to prevent mass assignment [SEC-REQ-006]"
    ↓
TEST-SEC-006a (Uso de DTO dedicado en registro para prevenir Mass Assignment)
    ↓
PASS
```
- **Archivos modificados:** `api_cloud/src/main/java/cl/duoc/api/auth/RegisterRequestDto.java` y `AuthController.java`
- **Cambio aplicado:** Creación de DTO inmutable sin atributo `role` y asignación obligatoria en servidor: `user.setRole("CLIENTE")`.

---

### Remediación FINDING-006
```text
FINDING-006 (Manipulación de precio unitario en órdenes)
    ↓
SEC-REQ-006 / SEC-REQ-005 continuo (Validación estricta de precio y descripción en catálogo oficial)
    ↓
Commit 4d63b20: "fix(orders): force catalog price lookup overriding client input [SEC-REQ-006]"
    ↓
TEST-SEC-006b (Validación estricta de precios de catálogo en creación de pedidos)
    ↓
PASS
```
- **Archivos modificados:** `api_cloud/src/main/java/cl/duoc/api/OrdersController.java`
- **Cambio aplicado:** Consulta forzosa a `ms-catalog` para sobrescribir determinísticamente `descripcion` y `precioUnitario` con valores de base de datos.

---

### Remediación FINDING-007
```text
FINDING-007 (Fuga de hash BCrypt en respuestas de la API)
    ↓
SEC-REQ-007 / SEC-REQ-006 continuo (Ocultamiento y minimización de credenciales en respuestas)
    ↓
Commit 1e99c43: "fix(auth): add @JsonIgnore on User.passwordHash [SEC-REQ-007]"
    ↓
TEST-SEC-007a (Ocultamiento de passwordHash en respuestas de API)
    ↓
PASS
```
- **Archivos modificados:** `api_cloud/src/main/java/cl/duoc/api/auth/User.java`
- **Cambio aplicado:** Inclusión de la directiva `@JsonIgnore` sobre el campo `passwordHash`.

---

### Remediación FINDING-008
```text
FINDING-008 (Ausencia de Rate Limiter perimetral en BFF)
    ↓
SEC-REQ-008 / SEC-REQ-007 continuo (Configuración de control de tasa de peticiones)
    ↓
Commit 8a55d12: "fix(security): configure Rate Limiter filter and security headers in BFF [SEC-REQ-008]"
    ↓
TEST-SEC-008 (Presencia de filtro de Rate Limiting en BFF)
    ↓
PASS
```
- **Archivos modificados:** `api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java`
- **Cambio aplicado:** Registro de filtro de limitación de tasa y cabeceras de protección perimetral.

---

### Remediación FINDING-009
```text
FINDING-009 (Inyección SQL en búsqueda de catálogo)
    ↓
SEC-REQ-009 / SEC-REQ-008 continuo (Parametrización estricta de consultas SQL)
    ↓
Commit 7c23f99: "fix(catalog): parameterize native query in searchProducts [SEC-REQ-010a]"
    ↓
TEST-SEC-010a (Uso de consultas SQL parametrizadas en búsqueda)
    ↓
PASS
```
- **Archivos modificados:** `ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java`
- **Cambio aplicado:** Consulta reescrita con `:q` y `.setParameter("q", "%" + q + "%")`.

---

### Remediación FINDING-010
```text
FINDING-010 (Cantidades y precios sin Bean Validation)
    ↓
SEC-REQ-010 / SEC-REQ-009 continuo (Validación de rangos @Min y @Positive en ítems)
    ↓
Commit 3d71e58: "fix(orders): restore Bean Validation @Min and @Positive in OrderItemRequestDto [SEC-REQ-010b]"
    ↓
TEST-SEC-010b (Validaciones Bean Validation en OrderItemRequestDto)
    ↓
PASS
```
- **Archivos modificados:** `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/dto/OrderItemRequestDto.java`
- **Cambio aplicado:** Descomentadas y activadas las anotaciones `@Min(value = 1)` y `@Positive`.

---

### Remediación FINDING-011
```text
FINDING-011 (Stored DOM XSS vía dangerouslySetInnerHTML)
    ↓
SEC-REQ-009 / SEC-REQ-008 continuo (Eliminación de HTML crudo; uso de escape automático JSX)
    ↓
Commit 6e44a17: "fix(frontend): replace dangerouslySetInnerHTML with safe JSX interpolation [SEC-REQ-009]"
    ↓
TEST-SEC-009 (Ausencia de renderizado HTML crudo inseguro en React)
    ↓
PASS
```
- **Archivos modificados:** `frontend/src/App.tsx`
- **Cambio aplicado:** Eliminación de todas las propiedades `dangerouslySetInnerHTML` reemplazadas por `{p.name}`, `{p.description}` y `{o.clienteNombre}`.

---

### Remediación FINDING-012
```text
FINDING-012 (Secretos débiles por defecto en Docker)
    ↓
SEC-REQ-012 / SEC-REQ-011 continuo (Erradicación de contraseñas predecibles changeme/postgres/guest)
    ↓
Commit 0b82f64: "fix(infra): purge weak default passwords in docker-compose.yml [SEC-REQ-012]"
    ↓
TEST-SEC-012 (Ausencia de contraseñas y secretos por defecto débiles)
    ↓
PASS
```
- **Archivos modificados:** `docker-compose.yml`
- **Cambio aplicado:** Sustitución de valores fijos por variables de entorno y claves criptográficas seguras de alta entropía.

---

### Remediación FINDING-013
```text
FINDING-013 (CORS con comodín wildcard * y falta de CSP)
    ↓
SEC-REQ-013 / SEC-REQ-012 continuo (Restricción estricta de orígenes CORS y políticas CSP/HSTS)
    ↓
Commit d912e73: "fix(security): restrict CORS origins and configure CSP/HSTS headers [SEC-REQ-013]"
    ↓
TEST-SEC-013 (Restricción estricta de orígenes CORS)
    ↓
PASS
```
- **Archivos modificados:** `api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java`
- **Cambio aplicado:** Eliminación del wildcard `*`, limitación a dominios del frontend y agregado de CSP, `X-Frame-Options: DENY` y `X-Content-Type-Options: nosniff`.

---

### Remediación FINDING-014
```text
FINDING-014 (Puertos internos de persistencia y broker expuestos al host)
    ↓
SEC-REQ-014 / SEC-REQ-013 continuo (Aislamiento de red interna en docker-compose)
    ↓
Commit c501d36: "fix(infra): isolate internal service ports from host network [SEC-REQ-014a]"
    ↓
TEST-SEC-014a (Aislamiento de puertos de microservicios y bases de datos)
    ↓
PASS
```
- **Archivos modificados:** `docker-compose.yml`
- **Cambio aplicado:** Supresión de directivas `ports:` en `ms-catalog` (8086), `ms-orders` (8087), `postgres-db` (5432) y `rabbitmq` (5672/15672).

---

### Remediación FINDING-015
```text
FINDING-015 (Contenedor web frontend ejecutándose como root)
    ↓
SEC-REQ-014 / SEC-REQ-013 continuo (Instrucción USER no-root en Dockerfiles)
    ↓
Commit f177a88: "fix(frontend): add USER nginx non-root directive in Dockerfile [SEC-REQ-014b]"
    ↓
TEST-SEC-014b (Instrucción USER no-root en contenedores Docker)
    ↓
PASS
```
- **Archivos modificados:** `frontend/Dockerfile`
- **Cambio aplicado:** Incorporación explícita de `USER nginx` antes del `CMD`.

---

## 2. Matriz General de Trazabilidad de Remediaciones (Fase VIII)

| ID Finding | Requisito | Commit Hash | Mensaje del Commit | Test Automatizado | Resultado Verificado |
|---|---|:---:|---|---|:---:|
| **FINDING-001** | **SEC-REQ-001** | `a3f1c82` | `fix(auth): implement account lockout after 5 failed attempts` | **TEST-SEC-001** | ✅ **PASS** |
| **FINDING-002** | **SEC-REQ-003** | `e7b4d19` | `fix(auth): reduce access token expiration to 1 hour` | **TEST-SEC-003** | ✅ **PASS** |
| **FINDING-003** | **SEC-REQ-004** | `9c42e05` | `fix(orders): enforce clienteId ownership on getOrderById and listOrders` | **TEST-SEC-004a/b** | ✅ **PASS** |
| **FINDING-004** | **SEC-REQ-005** | `2b88f31` | `fix(security): remove X-User-Role header reliance, use JWT authorities` | **TEST-SEC-005a** | ✅ **PASS** |
| **FINDING-005** | **SEC-REQ-006** | `5f10a74` | `fix(auth): introduce RegisterRequestDto to prevent mass assignment` | **TEST-SEC-006a** | ✅ **PASS** |
| **FINDING-006** | **SEC-REQ-006** | `4d63b20` | `fix(orders): force catalog price lookup overriding client input` | **TEST-SEC-006b** | ✅ **PASS** |
| **FINDING-007** | **SEC-REQ-007** | `1e99c43` | `fix(auth): add @JsonIgnore on User.passwordHash` | **TEST-SEC-007a** | ✅ **PASS** |
| **FINDING-008** | **SEC-REQ-008** | `8a55d12` | `fix(security): configure Rate Limiter filter and security headers in BFF` | **TEST-SEC-008** | ✅ **PASS** |
| **FINDING-009** | **SEC-REQ-009** | `7c23f99` | `fix(catalog): parameterize native query in searchProducts` | **TEST-SEC-010a** | ✅ **PASS** |
| **FINDING-010** | **SEC-REQ-010** | `3d71e58` | `fix(orders): restore Bean Validation @Min and @Positive in OrderItemRequestDto` | **TEST-SEC-010b** | ✅ **PASS** |
| **FINDING-011** | **SEC-REQ-009** | `6e44a17` | `fix(frontend): replace dangerouslySetInnerHTML with safe JSX interpolation` | **TEST-SEC-009** | ✅ **PASS** |
| **FINDING-012** | **SEC-REQ-012** | `0b82f64` | `fix(infra): purge weak default passwords in docker-compose.yml` | **TEST-SEC-012** | ✅ **PASS** |
| **FINDING-013** | **SEC-REQ-013** | `d912e73` | `fix(security): restrict CORS origins and configure CSP/HSTS headers` | **TEST-SEC-013** | ✅ **PASS** |
| **FINDING-014** | **SEC-REQ-014** | `c501d36` | `fix(infra): isolate internal service ports from host network` | **TEST-SEC-014a** | ✅ **PASS** |
| **FINDING-015** | **SEC-REQ-014** | `f177a88` | `fix(frontend): add USER nginx non-root directive in Dockerfile` | **TEST-SEC-014b** | ✅ **PASS** |

---

## 3. Conclusión de la Fase VIII

1. **Reversibilidad y Auditoría Completa:** Cada cambio de código ejecutado responde de forma unívoca a una vulnerabilidad modelada en el catálogo de hallazgos formales.
2. **Eficacia de Remediación Comprobada:** La suite de regresión `run_automated_security_tests.py` certifica una tasa de remediación del **100% (16/16 pruebas en PASS)** en el entorno seguro `CloudComputing_Seguro`.
3. **Cero Regresiones:** Todos los microservicios Java y componentes de frontend compilan y construyen de forma limpia (`mvn -q compile` y `tsc` sin fallos).
