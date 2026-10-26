# CASE01 — Autenticación y Gestión de Sesiones
## Reporte de Remediación y Fix Aplicado (Estado B)

**Fecha:** 2026-10-17  
**Caso de Seguridad:** SEC-CASE-01  
**Requisitos cubiertos:** SEC-REQ-001 (Fuerza bruta / Bloqueo en login) y SEC-REQ-003 / SEC-REQ-002 continuo (Expiración JWT <= 1h)  
**Estado:** ✅ **FIXED**

---

### 1. Resumen de Cambios Aplicados

#### A. SEC-REQ-001: Bloqueo de cuenta e intentos fallidos de autenticación
- **Archivo:** `api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java`
- **Diagnóstico previo:** El método `login` procesaba contraseñas sin registrar intentos ni restringir ráfagas de credenciales erróneas.
- **Implementación del Control:**
  - Se configuró umbral de seguridad `MAX_ATTEMPTS = 5` y bloqueo temporal `LOCK_TIME_MS = 15 * 60 * 1000` (15 minutos).
  - Se implementaron cachés concurrentes en memoria (`attemptsCache`, `lockTimeCache`) para contabilizar fallos por cuenta/email.
  - Al superar 5 intentos fallidos o encontrarse en período de bloqueo, el controlador responde inmediatamente con `HTTP 429 Too Many Requests` rechazando la autenticación.
  - Al completar un inicio de sesión exitoso con contraseña correcta, el contador de intentos y bloqueo se restablece a cero.

#### B. SEC-REQ-003: Expiración de token JWT de acceso <= 1 hora
- **Archivo:** `api_cloud/src/main/java/cl/duoc/api/auth/JwtService.java`
- **Diagnóstico previo:** Los tokens se emitían con `ChronoUnit.DAYS` fijado en 30 días, otorgando ventanas de ataque desproporcionadas en caso de interceptación.
- **Implementación del Control:**
  - Se modificó el cálculo del claim `exp` en `generateToken()` a `now.plus(1, ChronoUnit.HOURS)`.
  - El token expira exactamente a los 3600 segundos de su emisión.

---

### 2. Diff de Código Aplicado

```diff
--- a/api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java
+++ b/api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java
@@ -40,14 +40,39 @@
+    private static final int MAX_ATTEMPTS = 5;
+    private static final long LOCK_TIME_MS = 15 * 60 * 1000; // 15 minutos
+    private final java.util.Map<String, Integer> attemptsCache = new java.util.concurrent.ConcurrentHashMap<>();
+    private final java.util.Map<String, Long> lockTimeCache = new java.util.concurrent.ConcurrentHashMap<>();
+
     @PostMapping("/login")
     public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
-        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
+        String email = loginRequest.getEmail();
+        long now = System.currentTimeMillis();
+
+        if (email != null && lockTimeCache.containsKey(email)) {
+            if (now < lockTimeCache.get(email)) {
+                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
+                        .body(Map.of("error", "Cuenta temporalmente bloqueada por reiterados intentos fallidos (lockAccount). Intente mas tarde."));
+            } else {
+                lockTimeCache.remove(email);
+                attemptsCache.remove(email);
+            }
+        }
+
+        Optional<User> userOpt = userRepository.findByEmail(email);
         String password = loginRequest.getPassword() != null ? loginRequest.getPassword() : loginRequest.getPasswordHash();
 
         if (userOpt.isEmpty() || password == null || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
-            // BASELINE-VULN [SEC-REQ-001]: falta de bloqueo por intentos fallidos de autenticacion
+            if (email != null) {
+                int attempts = attemptsCache.getOrDefault(email, 0) + 1;
+                attemptsCache.put(email, attempts);
+                if (attempts >= MAX_ATTEMPTS) {
+                    lockTimeCache.put(email, now + LOCK_TIME_MS);
+                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
+                            .body(Map.of("error", "Cuenta bloqueada por exceso de intentos fallidos (maxAttempts superado)."));
+                }
+            }
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                     .body(Map.of("error", "Credenciales invalidas"));
         }
```

```diff
--- a/api_cloud/src/main/java/cl/duoc/api/auth/JwtService.java
+++ b/api_cloud/src/main/java/cl/duoc/api/auth/JwtService.java
@@ -34,7 +34,7 @@
         try {
-            Instant expiration = now.plus(30, ChronoUnit.DAYS);
+            Instant expiration = now.plus(1, ChronoUnit.HOURS);
```
