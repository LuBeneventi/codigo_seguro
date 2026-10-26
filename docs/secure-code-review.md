# Secure Code Review (Revisión Cruzada de Código)

**Proyecto:** Pedidos360 Coffee Store  
**Fecha:** 2026-10-19  
**Modalidad:** Peer Review Cruzado (Rol de Revisor de Seguridad Independiente vs. Desarrollador Original)  
**Alcance de la revisión:** Código fuente backend (BFF `api_cloud`, microservicios `catalog`, `orders`, `notify`), frontend React (`App.tsx`, `api.ts`), manifiestos de contenedores (`Dockerfile`, `docker-compose.yml`) y configuración (`application.yml`).

---

## 1. Metodología y Criterios de Evaluación

El reviewer independiente ejecutó un análisis exhaustivo enfocado en los 10 vectores de riesgo definidos en las directrices de revisión segura:
1. **Controles ausentes:** Lógicas críticas de negocio sin mitigaciones defensivas.
2. **Errores de autorización:** Falla de verificación de identidad, BOLA, IDOR y escalamiento de privilegios.
3. **Validación de entradas:** Parámetros sin tipado estricto, inyecciones (SQL/HTML) y falta de Bean Validation.
4. **Exposición de información:** Respuestas JSON excesivas, filtración de PII y credenciales.
5. **Secretos y credenciales:** Valores fijos, contraseñas predecibles y claves en repositorios.
6. **Dependencias vulnerables:** Componentes de terceros desactualizados con CVEs activos.
7. **Configuración de seguridad:** CORS permisivo, puertos abiertos y ausencia de cabeceras de protección (CSP/HSTS).
8. **Manejo de errores:** Fuga de stack traces o mensajes de excepción en respuestas HTTP 500.
9. **Logging y auditoría:** Registro de payloads completos o ausencia de trazas forenses.
10. **Malas prácticas del framework:** Uso indebido de Spring Security, JPA/Hibernate y ciclo de vida de componentes React.

---

## 2. Registro de Hallazgos de Code Review (CR-XXX) y Respuestas

---

### CR-001: Ausencia de Rate Limiting y mecanismo de bloqueo en endpoint de autenticación
- **Categoría:** Controles ausentes / Fuerza bruta
- **Archivo:Línea:** `api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java:46`
- **Severidad:** **Alta**
- **Observación del Reviewer:**
  > El método `login()` verifica las credenciales del usuario directamente contra la base de datos sin computar los intentos fallidos consecutivos ni aplicar límites de frecuencia de peticiones. Un actor malicioso puede lanzar ataques automatizados de diccionario o *credential stuffing* de manera ilimitada sin ser bloqueado.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta el hallazgo en su totalidad. Se implementó un contador de intentos fallidos en memoria con umbral de 5 intentos (`MAX_ATTEMPTS = 5`) y una ventana de bloqueo de 15 minutos (`LOCK_TIME_MS`). Si se superan los 5 intentos erróneos o la cuenta está bloqueada, el endpoint rechaza la petición respondiendo `HTTP 429 Too Many Requests`.

---

### CR-002: Expiración desmedida de tokens JWT sin política de refresco
- **Categoría:** Malas prácticas del framework / Gestión de sesiones
- **Archivo:Línea:** `api_cloud/src/main/java/cl/duoc/api/auth/JwtService.java:37`
- **Severidad:** **Media**
- **Observación del Reviewer:**
  > El token JWT se genera con una vigencia de 30 días (`ChronoUnit.DAYS`). En una arquitectura sin estado (*stateless*), si un token es interceptado o filtrado, el atacante mantendrá acceso persistente durante 720 horas continuas sin posibilidad de revocación en el servidor.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta la observación. Se redujo el tiempo de vida del token de acceso a 1 hora (`ChronoUnit.HOURS`), limitando sustancialmente la ventana de exposición en caso de compromiso de la credencial en el cliente.

---

### CR-003: Falta de comprobación de titularidad de pedidos (BOLA/IDOR) en `listOrders` y `getOrderById`
- **Categoría:** Errores de autorización / BOLA
- **Archivo:Línea:** `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java:85, 120`
- **Severidad:** **Alta**
- **Observación del Reviewer:**
  > `listOrders()` consulta la base de datos invocando `orderRepository.findAll()` sin filtrar por el cliente autenticado. A su vez, `getOrderById()` recupera el pedido por su ID primario y lo serializa sin comprobar si el `clienteId` de la orden coincide con el identificador del usuario que realiza la petición HTTP. Esto permite a cualquier cliente autenticado ver los pedidos de todos los demás.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Totalmente de acuerdo. Se reestructuró la capa de servicio para validar de forma obligatoria la titularidad del pedido con respecto al `clienteId` del usuario autenticado en `getOrderById()`, y se modificó la lógica de consulta para aislar las órdenes mediante el método seguro `findByClienteId` en `listOrders()`.

---

### CR-004: Confianza indebida en cabecera HTTP del cliente (`X-User-Role`) para autorizar acciones administrativas
- **Categoría:** Errores de autorización / Suplantación
- **Archivo:Línea:** `api_cloud/src/main/java/cl/duoc/api/auth/RoleHeaderFilter.java:28`
- **Severidad:** **Crítica**
- **Observación del Reviewer:**
  > Las rutas administrativas de creación de catálogo (`POST /api/catalog/products`) y actualización de estados autorizan al usuario leyendo el valor de la cabecera HTTP `request.getHeader("X-User-Role")`. Dado que cualquier cliente HTTP puede enviar `X-User-Role: ADMIN`, cualquier usuario regular puede escalar privilegios de forma trivial.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta el reporte. Se eliminó la lectura de la cabecera `X-User-Role` en la toma de decisiones de autorización. El filtro ahora extrae las autoridades del usuario autenticado de forma confiable directamente desde el contexto de seguridad (`SecurityContextHolder.getContext().getAuthentication()`).

---

### CR-005: Asignación masiva (Mass Assignment) en registro de usuarios por uso directo de entidad JPA
- **Categoría:** Validación de entradas / Malas prácticas del framework
- **Archivo:Línea:** `api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java:28`
- **Severidad:** **Alta**
- **Observación del Reviewer:**
  > El método `register(@RequestBody User user)` recibe la entidad JPA `User` directamente desde el payload JSON. Jackson mapea automáticamente el campo `"role":"ADMIN"` si el cliente lo incluye en el cuerpo de la petición, permitiendo a cualquier atacante auto-asignarse permisos de administrador durante el registro.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta la corrección. Se reemplazó la entidad `User` en la firma del método por el DTO dedicado `RegisterRequestDto`, el cual contiene únicamente los campos `email`, `password`, `name` y `phone`. En la capa de servicio se fija obligatoriamente el rol `CLIENTE` en el servidor: `user.setRole("CLIENTE")`.

---

### CR-006: Manipulación de precio unitario por aceptación de valores arbitrarios del cliente
- **Categoría:** Validación de entradas / Lógica de negocio
- **Archivo:Línea:** `api_cloud/src/main/java/cl/duoc/api/OrdersController.java:135`
- **Severidad:** **Crítica**
- **Observación del Reviewer:**
  > En la creación de órdenes, el BFF solo consulta el catálogo de productos si el ítem no trae `precioUnitario`. Si el cliente envía en el JSON `{"productoId": 1, "precioUnitario": 1.0}`, el BFF respeta dicho precio y lo retransmite al microservicio de órdenes. Esto permite a compradores pagar \$1 por cafés que cuestan \$10.000.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta la gravedad del hallazgo. Se eliminó la condición permisiva y se hizo mandatoria la consulta a `ms-catalog` para cada producto; el BFF ahora sobreescribe de forma determinista la descripción y el `precioUnitario` con los datos oficiales de base de datos antes de enviar la orden al microservicio de órdenes.

---

### CR-007: Exposición de `passwordHash` en respuestas JSON de la API
- **Categoría:** Exposición de información / Fuga de credenciales
- **Archivo:Línea:** `api_cloud/src/main/java/cl/duoc/api/auth/User.java:18`
- **Severidad:** **Alta**
- **Observación del Reviewer:**
  > El campo `passwordHash` no cuenta con la directiva `@JsonIgnore`. Las respuestas de los endpoints `/api/auth/register` y `/api/auth/me` devuelven el hash BCrypt del usuario en texto plano en la respuesta HTTP, facilitando ataques de descifrado offline (fuerza bruta de hashes con Hashcat/John the Ripper).
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta. Se agregó la anotación `@JsonIgnore` sobre el atributo `passwordHash` en `User.java`, suprimiendo su inclusión en cualquier respuesta JSON generada por el serializador Jackson.

---

### CR-008: Inyección SQL nativa en endpoint de búsqueda de catálogo
- **Categoría:** Validación de entradas / SQL Injection
- **Archivo:Línea:** `ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java:38`
- **Severidad:** **Alta**
- **Observación del Reviewer:**
  > La consulta en `searchProducts` concatena directamente el parámetro `q` en un query nativo: `String sql = "SELECT * FROM products WHERE name ILIKE '%" + q + "%'"; entityManager.createNativeQuery(sql, ...)`. Al ingresar `' OR '1'='1`, la consulta altera su lógica booleana retornando todos los registros del sistema.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta. Se reescribió la consulta nativa utilizando parámetros nombrados (`WHERE name ILIKE :q`) y se ligó el valor de forma segura mediante `.setParameter("q", "%" + q + "%")`, impidiendo la alteración de la sintaxis SQL.

---

### CR-009: Omisión de Bean Validation en DTOs de ítems de órdenes
- **Categoría:** Validación de entradas / Consistencia de negocio
- **Archivo:Línea:** `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/dto/OrderItemRequestDto.java:23, 28`
- **Severidad:** **Media**
- **Observación del Reviewer:**
  > Las anotaciones `@Min(1)` para `cantidad` y `@Positive` para `precioUnitario` estaban comentadas. Esto permite enviar solicitudes de compra con cantidades negativas (ej. `-5` cafés) o precios en cero, provocando inconsistencias severas en el cálculo de totales y stock.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta el reporte. Se restablecieron de forma activa las anotaciones de validación `@Min(value = 1)` y `@Positive`, asegurando que el framework rechace peticiones malformadas con `HTTP 400 Bad Request`.

---

### CR-010: Almacenamiento y renderizado de HTML sin sanitizar (Stored DOM XSS) en frontend React
- **Categoría:** Validación de entradas / Cross-Site Scripting (XSS)
- **Archivo:Línea:** `frontend/src/App.tsx:353, 355`
- **Severidad:** **Alta**
- **Observación del Reviewer:**
  > El componente React utiliza `dangerouslySetInnerHTML={{ __html: p.name }}` y `dangerouslySetInnerHTML={{ __html: p.description }}` para mostrar la información del catálogo. Si un producto contiene una carga útil como `<img src=x onerror=alert(document.cookie)>`, el script se ejecuta en el contexto del navegador del usuario.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta. Se eliminaron todas las ocurrencias de `dangerouslySetInnerHTML` en `App.tsx`, reemplazándolas por interpolación estándar JSX `{p.name}` y `{p.description}`, lo cual aplica el escape automático y codificación de entidades nativo de React.

---

### CR-011: Secretos criptográficos y credenciales de base de datos predecibles en archivos de configuración
- **Categoría:** Secretos y credenciales / Hardcoded Credentials
- **Archivo:Línea:** `docker-compose.yml:103, 147, 169` y `application.yml`
- **Severidad:** **Alta**
- **Observación del Reviewer:**
  > Los archivos de orquestación y configuración definen valores por defecto en texto plano débiles: `JWT_SECRET=changeme`, `POSTGRES_PASSWORD=postgres` y `RABBITMQ_DEFAULT_PASS=guest`. Un adversario con acceso al repositorio público puede firmar tokens JWT válidos o vulnerar la base de datos en entornos desplegados.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta. Se purgaron las credenciales predeterminadas en `docker-compose.yml`, sustituyéndolas por referencias seguras a variables de entorno y claves criptográficas robustas de alta entropía.

---

### CR-012: Configuración CORS permisiva con comodín (`*`) y ausencia de Content-Security-Policy
- **Categoría:** Configuración de seguridad / CORS & Headers
- **Archivo:Línea:** `api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java:73`
- **Severidad:** **Media**
- **Observación del Reviewer:**
  > La configuración CORS define `config.setAllowedOrigins(List.of(..., "*"))`. El uso del wildcard `*` autoriza a cualquier origen externo a realizar peticiones cruzadas hacia la API desde el navegador de la víctima. Además, faltan cabeceras de seguridad fundamentales como `Content-Security-Policy` y `X-Frame-Options: DENY`.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta. Se eliminó el wildcard `*`, limitando los orígenes autorizados exclusivamente a los dominios oficiales del frontend (`http://localhost:5173`, `http://localhost:3000`, `http://localhost:8080`). Se agregaron las directivas `contentSecurityPolicy()`, `frameOptions().deny()` y `contentTypeOptions()`.

---

### CR-013: Exposición de puertos de persistencia y broker de mensajería hacia el host
- **Categoría:** Configuración de seguridad / Aislamiento de red
- **Archivo:Línea:** `docker-compose.yml:143, 164`
- **Severidad:** **Alta**
- **Observación del Reviewer:**
  > Los servicios `postgres-db` (puerto 5432), `rabbitmq` (5672 y 15672), `ms-catalog` (8086) y `ms-orders` (8087) tienen mapeo directo de puertos hacia el host anfitrión. Esto permite que atacantes en la red local se conecten directamente a la base de datos o al broker sin pasar por el BFF ni registrarse en los logs de seguridad.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta. Se eliminaron las directivas `ports:` de los microservicios internos y componentes de base de datos en `docker-compose.yml`. Todos ellos se comunican de forma aislada a través de la red puente interna `pedidos360-net`.

---

### CR-014: Ejecución de contenedor web como superusuario (`root`)
- **Categoría:** Configuración de seguridad / Hardening de contenedores
- **Archivo:Línea:** `frontend/Dockerfile:10-14`
- **Severidad:** **Media**
- **Observación del Reviewer:**
  > La imagen final de Nginx en `frontend/Dockerfile` no especifica la directiva `USER`, ejecutando el proceso de servidor web como `root` (UID 0), violando el principio de privilegio mínimo en entornos de contenedores.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta. Se agregó la directiva `USER nginx` previo a la instrucción `CMD`, garantizando que el contenedor ejecute con un usuario sin privilegios administrativos.

---

### CR-015: Manejo de excepciones con filtración de mensajes internos en respuestas HTTP 500
- **Categoría:** Manejo de errores / Fuga de información técnica
- **Archivo:Línea:** `ms-pedidos360-orders/.../GlobalExceptionHandler.java:71` y `api_cloud/.../OrdersController.java:294`
- **Severidad:** **Baja**
- **Observación del Reviewer:**
  > Los manejadores de excepciones retornan directamente `ex.getMessage()` o `e.getMessage()` en el cuerpo de la respuesta HTTP de error. Esto expone nombres de tablas, sintaxis SQL, trazas de drivers JDBC o detalles de implementación interna a los clientes externos.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta. Se sanitizaron las respuestas de error 500 para devolver mensajes genéricos de fallo ("Ocurrió un error interno al procesar la solicitud"), enviando los detalles técnicos únicamente a los logs internos del servidor.

---

### CR-016: Registro en log de cuerpos de mensajes completos con datos sensibles en microservicio de notificaciones
- **Categoría:** Logging y auditoría / Fuga de PII en bitácoras
- **Archivo:Línea:** `ms-pedidos360-notify/.../EmailListener.java:91`
- **Severidad:** **Media**
- **Observación del Reviewer:**
  > El listener de RabbitMQ imprime en consola `log.info("Procesando evento de correo: {}", payload.getBody())`, volcando datos personales del cliente (nombre, dirección, correo, RUT) y detalles de compra en los logs del contenedor en texto plano.
- **Respuesta del Desarrollador:** **Fixed**
- **Justificación y Acción:**
  Se acepta. Se refactorizó la sentencia de logging para registrar únicamente identificadores de transacción y metadatos operativos (`orderId`, `tipoNotificacion`), suprimiendo el volcado de payloads completos con PII.

---

## 3. Matriz de Resumen de Revisión Cruzada

| ID Hallazgo | Componente | Categoría | Severidad | Estado | Decisión del Desarrollador |
|---|---|---|:---:|:---:|:---:|
| **CR-001** | `api_cloud` | Controles ausentes (Fuerza bruta) | Alta | **Fixed** | Accepted / Implementado bloqueo progresivo (5 intentos, 15 min) |
| **CR-002** | `api_cloud` | Malas prácticas (Vigencia JWT 30d) | Media | **Fixed** | Accepted / Reducido token de acceso a 1 hora |
| **CR-003** | `ms-orders` | Errores de autorización (BOLA/IDOR) | Alta | **Fixed** | Accepted / Filtrado `findByClienteId` y control de titularidad |
| **CR-004** | `api_cloud` | Errores de autorización (`X-User-Role`) | Crítica | **Fixed** | Accepted / Desacople de cabecera; rol resuelto desde JWT |
| **CR-005** | `api_cloud` | Validación de entradas (Mass Assignment) | Alta | **Fixed** | Accepted / DTO estricto `RegisterRequestDto` sin campo `role` |
| **CR-006** | `api_cloud` | Lógica de negocio (Manipulación de precio) | Crítica | **Fixed** | Accepted / Consulta mandatoria al catálogo oficial |
| **CR-007** | `api_cloud` | Exposición de información (`passwordHash`) | Alta | **Fixed** | Accepted / Directiva `@JsonIgnore` en entidad `User` |
| **CR-008** | `ms-catalog` | Validación de entradas (Inyección SQL) | Alta | **Fixed** | Accepted / Parametrización con `:q` y `.setParameter()` |
| **CR-009** | `ms-orders` | Validación de entradas (Bean Validation) | Media | **Fixed** | Accepted / Restaurado `@Min(1)` y `@Positive` en DTO |
| **CR-010** | `frontend` | Validación de entradas (Stored DOM XSS) | Alta | **Fixed** | Accepted / Eliminado `dangerouslySetInnerHTML`; uso de JSX seguro |
| **CR-011** | `docker-compose` | Secretos y credenciales (Claves débiles) | Alta | **Fixed** | Accepted / Sustitución por variables y claves robustas |
| **CR-012** | `api_cloud` | Configuración segura (CORS y CSP) | Media | **Fixed** | Accepted / Eliminado comodín `*`; cabeceras CSP y HSTS añadidas |
| **CR-013** | `docker-compose` | Configuración segura (Puertos expuestos) | Alta | **Fixed** | Accepted / Aislamiento total en red interna `pedidos360-net` |
| **CR-014** | `frontend` | Configuración segura (Contenedor root) | Media | **Fixed** | Accepted / Instrucción `USER nginx` en Dockerfile |
| **CR-015** | `ms-orders` | Manejo de errores (Fuga en HTTP 500) | Baja | **Fixed** | Accepted / Mensajes genéricos de error al cliente |
| **CR-016** | `ms-notify` | Logging y auditoría (Fuga de PII) | Media | **Fixed** | Accepted / Logging acotado a IDs operativos sin datos personales |
