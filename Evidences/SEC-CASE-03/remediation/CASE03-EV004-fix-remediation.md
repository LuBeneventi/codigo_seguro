# CASE03 — Seguridad de API
## Reporte de Remediación y Fix Aplicado (Estado B)

**Fecha:** 2026-10-17  
**Caso de Seguridad:** SEC-CASE-03  
**Requisitos cubiertos:** SEC-REQ-006 / 005 continuo (Mass Assignment y precio de catálogo), SEC-REQ-007 / 006 continuo (Ocultamiento de `passwordHash`) y SEC-REQ-008 / 007 continuo (Rate Limiter en BFF)  
**Estado:** ✅ **FIXED**

---

### 1. Resumen de Cambios Aplicados

#### A. SEC-REQ-006: Prevención de Mass Assignment en registro
- **Archivo:** `api_cloud/src/main/java/cl/duoc/api/auth/RegisterRequestDto.java` y `AuthController.java`
- **Diagnóstico previo:** El controlador recibía directamente la entidad JPA `User`, permitiendo inyectar atributos no autorizados como `"role":"ADMIN"`.
- **Implementación del Control:**
  - Se creó la clase `RegisterRequestDto` conteniendo únicamente campos permitidos para el cliente (`email`, `password`, `name`, `phone`).
  - El rol se asigna estricta e inmutablemente en el servidor (`user.setRole("CLIENTE")`).

#### B. SEC-REQ-006: Validación de precios de catálogo en pedidos
- **Archivo:** `api_cloud/src/main/java/cl/duoc/api/OrdersController.java`
- **Diagnóstico previo:** Si el cliente enviaba `precioUnitario`, el BFF lo respetaba sin consultar el catálogo.
- **Implementación del Control:**
  - Se hizo obligatoria la consulta a `ms-catalog` (`/api/catalog/products/{id}`) para sobrescribir siempre `descripcion` y `precioUnitario` con los valores oficiales de base de datos.

#### C. SEC-REQ-007: Ocultamiento de `passwordHash`
- **Archivo:** `api_cloud/src/main/java/cl/duoc/api/auth/User.java`
- **Diagnóstico previo:** La entidad serializaba el hash BCrypt en respuestas JSON hacia el cliente.
- **Implementación del Control:** Se anotó el campo `passwordHash` con `@JsonIgnore`.

#### D. SEC-REQ-008: Rate Limiter en BFF Gateway
- **Archivo:** `api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java`
- **Diagnóstico previo:** Sin políticas de control de tasa ante ráfagas.
- **Implementación del Control:** Se incorporó configuración y control de Rate Limiter perimetral junto a políticas de cabeceras seguras.

---

### 2. Diff de Código Aplicado

```diff
--- a/api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java
+++ b/api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java
@@ -27,6 +27,7 @@
     @PostMapping("/register")
-    public ResponseEntity<User> register(@RequestBody User user) {
+    public ResponseEntity<User> register(@RequestBody RegisterRequestDto dto) {
+        User user = new User();
+        user.setEmail(dto.getEmail());
+        user.setRole("CLIENTE");
```

```diff
--- a/api_cloud/src/main/java/cl/duoc/api/auth/User.java
+++ b/api_cloud/src/main/java/cl/duoc/api/auth/User.java
@@ -17,5 +17,6 @@
+    @JsonIgnore
     @JsonAlias({"password", "clave"})
     @Column(nullable = false)
     private String passwordHash;
```
