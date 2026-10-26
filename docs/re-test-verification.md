# Fase de Re-Test (Verificación Obligatoria Post-Remediación)

**Proyecto:** Pedidos360 Coffee Store  
**Fecha:** 2026-10-19  
**Objetivo Metodológico:** Demostrar de forma empírica y reproducible el ciclo de verificación obligatoria de seguridad. Esta fase evita uno de los errores más habituales en ingeniería de software y DevSecOps: **asumir que porque se modificó código, el problema quedó solucionado**.

Para cada vulnerabilidad se contrasta la triada de control:
$$\mathbf{Antes\ (Estado\ A):}\ \text{TEST-SEC-XXX} \longrightarrow \mathbf{FAIL}$$
$$\mathbf{Correcci\acute{o}n:}\ \text{Commit [SHA]} \longrightarrow \mathbf{Control\ Implementado}$$
$$\mathbf{Despu\acute{e}s\ (Estado\ B):}\ \text{TEST-SEC-XXX} \longrightarrow \mathbf{PASS}$$

---

## 1. Re-evaluación Metodológica Transversal

Tras aplicar los parches en `CloudComputing_Seguro`, se re-ejecutó el ciclo completo de auditoría:

| Disciplina de Testing | Herramienta / Método | Evaluación en Estado A (Baseline) | Re-evaluación en Estado B (Remediado) | Estado Final |
|---|---|:---:|:---:|:---:|
| **SAST** (Static Analysis) | Semgrep 1.181.0 + Revisión Estática | ❌ 10 hallazgos automatizados y 23 marcadores vulnerables en código. | ✅ Marcadores vulnerables erradicados; consultas parametrizadas; DTOs seguros; sin wildcard CORS. | **PASS** |
| **SCA** (Software Composition) | OSV Scanner (`api.osv.dev`) | ❌ Múltiples CVEs en dependencias base y dev-servers. | ✅ Mitigaciones de runtime y controles arquitectónicos compensatorios aplicados. | **MITIGATED** |
| **Security Tests** (Regresión) | Suite Python `run_automated_security_tests.py` | ❌ **16 / 16 Tests FAIL** (100% Vulnerable). | ✅ **16 / 16 Tests PASS** (100% Remediado). | **PASS** |
| **DAST** (Dynamic Analysis) | Especificaciones OWASP WSTG / API Top 10 | ❌ Peticiones con inyecciones, BOLA y precios manipulados aceptadas. | ✅ Transacciones HTTP bloqueadas con 400 Bad Request, 401 Unauthorized o 429 Too Many Requests. | **PASS** |

---

## 2. Matriz de Demostración: Antes → Corrección → Después

A continuación se detalla caso a caso la evidencia de transición desde el fallo verificado hasta la superación del re-test:

---

### SEC-CASE-01: Autenticación y gestión de sesiones

#### Caso 1.1: Fuerza bruta sin bloqueo en login
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-001`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** El controlador `AuthController.java` procesaba contraseñas erróneas de forma ilimitada sin registrar intentos fallidos ni responder con retardo o bloqueo.
- **Corrección aplicada:**
  - **Commit:** `a3f1c82` — *"fix(auth): implement account lockout after 5 failed attempts [SEC-REQ-001]"*
  - **Control implementado:** Se implementó `attemptsCache` y `lockTimeCache` con `MAX_ATTEMPTS = 5` y bloqueo temporal de 15 minutos (`LOCK_TIME_MS`) respondiendo con `HTTP 429 Too Many Requests`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-001`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** Aserción superada; la aplicación bloquea nuevas solicitudes al alcanzar el 5.° fallo.

#### Caso 1.2: Expiración desmedida de token JWT (30 días)
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-003`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `JwtService.java` generaba el token con `now.plus(30, ChronoUnit.DAYS)`, creando una ventana de persistencia de 720 horas.
- **Corrección aplicada:**
  - **Commit:** `e7b4d19` — *"fix(auth): reduce access token expiration to 1 hour [SEC-REQ-003]"*
  - **Control implementado:** Se configuró el cálculo de expiración en `now.plus(1, ChronoUnit.HOURS)`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-003`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** El token expira exactamente a los 3600 segundos ($\le 1\,\text{h}$), validado en el claim `exp`.

---

### SEC-CASE-02: Autorización y control de acceso

#### Caso 2.1: Aislamiento multi-inquilino en listado de pedidos (BOLA en `listOrders`)
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-004a`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `OrderServiceImpl.java` ejecutaba `orderRepository.findAll()` sin filtrar por el cliente autenticado, permitiendo ver pedidos ajenos.
- **Corrección aplicada:**
  - **Commit:** `9c42e05` — *"fix(orders): enforce clienteId ownership on getOrderById and listOrders [SEC-REQ-004]"*
  - **Control implementado:** Se estructuró la consulta obligatoria filtrando por `clienteId` (`findByClienteId`) en la capa de persistencia.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-004a`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** El servicio aísla estrictamente los registros devolviendo únicamente las órdenes pertenecientes al solicitante.

#### Caso 2.2: Comprobación de titularidad en consulta de orden por ID (IDOR en `getOrderById`)
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-004b`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `getOrderById(id)` recuperaba la orden por su clave primaria sin contrastar si el usuario en sesión era el dueño legítimo.
- **Corrección aplicada:**
  - **Commit:** `9c42e05` — *"fix(orders): enforce clienteId ownership on getOrderById and listOrders [SEC-REQ-004]"*
  - **Control implementado:** Se incorporó validación explícita de coincidencia de identidad sobre el atributo `clienteId` de la entidad.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-004b`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** La consulta valida titularidad y rechaza peticiones sobre IDs de órdenes de terceros.

#### Caso 2.3: Bypass de rol administrativo por cabecera inyectable `X-User-Role`
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-005a`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `RoleHeaderFilter.java` leía `request.getHeader("X-User-Role")`, permitiendo a cualquier cliente inyectar `"ADMIN"`.
- **Corrección aplicada:**
  - **Commit:** `2b88f31` — *"fix(security): remove X-User-Role header reliance, use JWT authorities [SEC-REQ-005]"*
  - **Control implementado:** Se desacopló de la cabecera del cliente y se verificó el rol directamente desde el token firmado en `SecurityContextHolder`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-005a`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** La inyección de cabeceras HTTP es ignorada; las rutas administrativas exigen rol verificado en el JWT.

---

### SEC-CASE-03: Seguridad de API

#### Caso 3.1: Mass Assignment en registro de usuarios
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-006a`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `AuthController.java` recibía `@RequestBody User user`, permitiendo inyectar `"role":"ADMIN"` en el JSON.
- **Corrección aplicada:**
  - **Commit:** `5f10a74` — *"fix(auth): introduce RegisterRequestDto to prevent mass assignment [SEC-REQ-006]"*
  - **Control implementado:** Creación de `RegisterRequestDto` sin campo `role` y asignación inmutable en servidor: `user.setRole("CLIENTE")`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-006a`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** El endpoint ignora o rechaza atributos no contemplados; el usuario siempre se crea con rol `CLIENTE`.

#### Caso 3.2: Manipulación de precio unitario en creación de pedidos
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-006b`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** El BFF en `OrdersController.java` respetaba el `precioUnitario` enviado en el body si venía informado por el cliente.
- **Corrección aplicada:**
  - **Commit:** `4d63b20` — *"fix(orders): force catalog price lookup overriding client input [SEC-REQ-006]"*
  - **Control implementado:** Consulta forzosa a `ms-catalog` para sobrescribir determinísticamente el precio con el valor oficial de base de datos.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-006b`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** Todo ítem se recalcula con el precio del catálogo; precios alterados por el cliente quedan sin efecto.

#### Caso 3.3: Exposición de `passwordHash` en respuestas de la API
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-007a`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** La entidad `User.java` serializaba el hash BCrypt en los endpoints `/register` y `/me`.
- **Corrección aplicada:**
  - **Commit:** `1e99c43` — *"fix(auth): add @JsonIgnore on User.passwordHash [SEC-REQ-007]"*
  - **Control implementado:** Incorporación de la directiva `@JsonIgnore` sobre el atributo `passwordHash`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-007a`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** El atributo `passwordHash` queda completamente ausente en los payloads JSON de respuesta.

#### Caso 3.4: Ausencia de Rate Limiting en el BFF Gateway
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-008`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `SecurityConfig.java` carecía de filtros de limitación de tasa ante ráfagas.
- **Corrección aplicada:**
  - **Commit:** `8a55d12` — *"fix(security): configure Rate Limiter filter and security headers in BFF [SEC-REQ-008]"*
  - **Control implementado:** Registro de Rate Limiter perimetral y cabeceras de control en la cadena de seguridad.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-008`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** La cadena de seguridad cuenta con protección activa contra saturación.

---

### SEC-CASE-04: Validación y sanitización de entradas

#### Caso 4.1: Inyección SQL nativa en búsqueda de catálogo
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-010a`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `ProductController.java` concatenaba el parámetro `q` en un query nativo con `ILIKE '%" + q + "%'`.
- **Corrección aplicada:**
  - **Commit:** `7c23f99` — *"fix(catalog): parameterize native query in searchProducts [SEC-REQ-010a]"*
  - **Control implementado:** Parametrización mediante marcador `:q` y `.setParameter("q", "%" + q + "%")`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-010a`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** La consulta es compilada como PreparedStatement seguro; cargas como `' OR '1'='1` se tratan como texto literal.

#### Caso 4.2: Omisión de Bean Validation en `OrderItemRequestDto`
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-010b`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** Se habían deshabilitado las anotaciones `@Min(1)` y `@Positive`, permitiendo cantidades negativas.
- **Corrección aplicada:**
  - **Commit:** `3d71e58` — *"fix(orders): restore Bean Validation @Min and @Positive in OrderItemRequestDto [SEC-REQ-010b]"*
  - **Control implementado:** Reactivación de `@Min(value = 1)` en cantidad y `@Positive` en precio unitario.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-010b`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** Peticiones con cantidades $\le 0$ son interceptadas y rechazadas con `HTTP 400 Bad Request`.

#### Caso 4.3: Stored DOM XSS vía `dangerouslySetInnerHTML` en React
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-009`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `App.tsx` utilizaba `dangerouslySetInnerHTML` en 12 ubicaciones para renderizar nombres y descripciones.
- **Corrección aplicada:**
  - **Commit:** `6e44a17` — *"fix(frontend): replace dangerouslySetInnerHTML with safe JSX interpolation [SEC-REQ-009]"*
  - **Control implementado:** Sustitución de `dangerouslySetInnerHTML` por interpolación estándar JSX `{p.name}` y `{p.description}`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-009`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** Cero usos de directivas inseguras de HTML crudo; React aplica escape automático contra XSS.

---

### SEC-CASE-05: Configuración segura

#### Caso 5.1: Política CORS permisiva con comodín (`*`)
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-013`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `SecurityConfig.java` incluía `"*"` en `setAllowedOrigins`, autorizando peticiones desde cualquier dominio.
- **Corrección aplicada:**
  - **Commit:** `d912e73` — *"fix(security): restrict CORS origins and configure CSP/HSTS headers [SEC-REQ-013]"*
  - **Control implementado:** Eliminación del comodín `*`, restricción a dominios del frontend y agregado de CSP y `X-Frame-Options: DENY`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-013`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** Orígenes cruzados arbitrarios son rechazados; cabeceras de seguridad activas.

#### Caso 5.2: Secretos y contraseñas predecibles por defecto en Docker
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-012`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `docker-compose.yml` contenía credenciales en claro: `changeme`, `postgres` y `guest`.
- **Corrección aplicada:**
  - **Commit:** `0b82f64` — *"fix(infra): purge weak default passwords in docker-compose.yml [SEC-REQ-012]"*
  - **Control implementado:** Purga de contraseñas por defecto y reemplazo por variables seguras y claves robustas.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-012`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** Cero secretos predecibles en archivos de despliegue.

#### Caso 5.3: Puertos internos de backend expuestos al host
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-014a`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** Puertos 8086, 8087, 5432 y 5672/15672 se exponían en `127.0.0.1`, permitiendo eludir el BFF.
- **Corrección aplicada:**
  - **Commit:** `c501d36` — *"fix(infra): isolate internal service ports from host network [SEC-REQ-014a]"*
  - **Control implementado:** Eliminación de mapeos `ports:` para servicios internos en `docker-compose.yml`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-014a`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** Servicios internos y bases de datos completamente aislados en la red privada Docker `pedidos360-net`.

#### Caso 5.4: Contenedor frontend ejecutándose como root
- **Antes (Estado A):**
  - **Prueba:** `TEST-SEC-014b`
  - **Resultado:** ❌ **FAIL**
  - **Diagnóstico:** `frontend/Dockerfile` carecía de directiva `USER`, ejecutando el proceso Nginx con UID 0 (`root`).
- **Corrección aplicada:**
  - **Commit:** `f177a88` — *"fix(frontend): add USER nginx non-root directive in Dockerfile [SEC-REQ-014b]"*
  - **Control implementado:** Declaración explícita de `USER nginx` antes del `CMD`.
- **Después (Estado B):**
  - **Prueba:** `TEST-SEC-014b`
  - **Resultado:** ✅ **PASS**
  - **Diagnóstico Post-Fix:** Todos los contenedores del ecosistema operan con usuarios sin privilegios administrativos.

---

## 3. Cuadro Resumen Comparativo de Re-Test

| Requisito | Prueba Asociada | Resultado Estado A (Antes) | Commit de Corrección | Control Implementado | Resultado Estado B (Después) |
|---|---|:---:|:---:|---|:---:|
| **SEC-REQ-001** | `TEST-SEC-001` | ❌ **FAIL** | `a3f1c82` | Lockout temporal tras 5 fallos (15 min) | ✅ **PASS** |
| **SEC-REQ-003** | `TEST-SEC-003` | ❌ **FAIL** | `e7b4d19` | Token JWT con expiración acotada a 1 hora | ✅ **PASS** |
| **SEC-REQ-004** | `TEST-SEC-004a` | ❌ **FAIL** | `9c42e05` | Filtrado estricto por cliente en `listOrders` | ✅ **PASS** |
| **SEC-REQ-004** | `TEST-SEC-004b` | ❌ **FAIL** | `9c42e05` | Control de titularidad en `getOrderById` | ✅ **PASS** |
| **SEC-REQ-005** | `TEST-SEC-005a` | ❌ **FAIL** | `2b88f31` | Desacople de `X-User-Role`; rol desde JWT | ✅ **PASS** |
| **SEC-REQ-006** | `TEST-SEC-006a` | ❌ **FAIL** | `5f10a74` | DTO estricto de registro sin atributo de rol | ✅ **PASS** |
| **SEC-REQ-006** | `TEST-SEC-006b` | ❌ **FAIL** | `4d63b20` | Sobrescritura obligatoria de precios desde catálogo | ✅ **PASS** |
| **SEC-REQ-007** | `TEST-SEC-007a` | ❌ **FAIL** | `1e99c43` | Directiva `@JsonIgnore` en `passwordHash` | ✅ **PASS** |
| **SEC-REQ-008** | `TEST-SEC-008` | ❌ **FAIL** | `8a55d12` | Rate Limiter perimetral en Spring Security | ✅ **PASS** |
| **SEC-REQ-009** | `TEST-SEC-010a` | ❌ **FAIL** | `7c23f99` | Consulta SQL nativa parametrizada con `:q` | ✅ **PASS** |
| **SEC-REQ-010** | `TEST-SEC-010b` | ❌ **FAIL** | `3d71e58` | Bean Validation `@Min(1)` y `@Positive` activas | ✅ **PASS** |
| **SEC-REQ-009** | `TEST-SEC-009` | ❌ **FAIL** | `6e44a17` | Erradicación de `dangerouslySetInnerHTML` | ✅ **PASS** |
| **SEC-REQ-012** | `TEST-SEC-012` | ❌ **FAIL** | `0b82f64` | Purga de claves débiles en `docker-compose.yml` | ✅ **PASS** |
| **SEC-REQ-013** | `TEST-SEC-013` | ❌ **FAIL** | `d912e73` | Eliminación de comodín CORS y cabeceras CSP | ✅ **PASS** |
| **SEC-REQ-014** | `TEST-SEC-014a` | ❌ **FAIL** | `c501d36` | Aislamiento de puertos internos de base de datos | ✅ **PASS** |
| **SEC-REQ-014** | `TEST-SEC-014b` | ❌ **FAIL** | `f177a88` | Directiva `USER nginx` no-root en Dockerfile | ✅ **PASS** |

---

## 4. Conclusión

El proceso de Re-Test evidencia que la transición del **Estado A (Baseline)** al **Estado B (Remediado)** logró una tasa de éxito del **100% (16/16 pruebas superadas)**. Cada vulnerabilidad cuenta con su trazabilidad completa y su validación empírica correspondiente, garantizando que el sistema **Pedidos360** no solo aplicó cambios en el código, sino que cerró de forma efectiva los vectores de ataque identificados.
