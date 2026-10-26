# CASE05 — Configuración Segura
## Reporte de Remediación y Fix Aplicado (Estado B)

**Fecha:** 2026-10-17  
**Caso de Seguridad:** SEC-CASE-05  
**Requisitos cubiertos:** SEC-REQ-012 / 011 continuo (Secretos débiles), SEC-REQ-013 / 012 continuo (CORS y CSP) y SEC-REQ-014 / 013 continuo (Aislamiento de puertos y contenedor no-root)  
**Estado:** ✅ **FIXED**

---

### 1. Resumen de Cambios Aplicados

#### A. SEC-REQ-013: Restricción estricta de política CORS y cabeceras
- **Archivo:** `api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java`
- **Diagnóstico previo:** Se permitía el comodín `*` en `allowedOrigins` y se carecía de Content-Security-Policy y `X-Frame-Options`.
- **Implementación del Control:**
  - Se eliminó el comodín wildcard `*` y se limitaron los orígenes a las URLs del frontend (`http://localhost:5173`, `http://localhost:3000`, `http://localhost:8080`).
  - Se añadieron directivas de `frameOptions().deny()`, `contentTypeOptions()` y `contentSecurityPolicy()`.

#### B. SEC-REQ-012: Eliminación de contraseñas y secretos débiles por defecto
- **Archivo:** `docker-compose.yml`
- **Diagnóstico previo:** Presencia de valores por defecto predecibles en texto claro (`changeme`, `guest`, `postgres`).
- **Implementación del Control:** Se sustituyeron por variables de entorno y secretos robustos de alta entropía.

#### C. SEC-REQ-014a: Aislamiento de puertos de microservicios internos y bases de datos
- **Archivo:** `docker-compose.yml`
- **Diagnóstico previo:** Puertos internos 8086 (`ms-catalog`), 8087 (`ms-orders`), 5432 (`postgres-db`), 5672 y 15672 (`rabbitmq`) estaban expuestos en la interfaz de loopback del host anfitrión.
- **Implementación del Control:** Se removieron los mapeos `ports:` de estos servicios, forzando toda comunicación interna exclusivamente a través de la red privada Docker `pedidos360-net`.

#### D. SEC-REQ-014b: Ejecución de contenedores con usuario no privilegiado
- **Archivo:** `frontend/Dockerfile`
- **Diagnóstico previo:** El contenedor Nginx del frontend ejecutaba el proceso con el usuario `root` (UID 0).
- **Implementación del Control:** Se declaró la instrucción `USER nginx` en el Dockerfile final.

---

### 2. Diff de Código Aplicado

```diff
--- a/frontend/Dockerfile
+++ b/frontend/Dockerfile
@@ -11,5 +11,6 @@
 COPY --from=build /app/dist /usr/share/nginx/html
 COPY nginx.conf /etc/nginx/conf.d/default.conf
 EXPOSE 80
+USER nginx
 CMD ["nginx", "-g", "daemon off;"]
```

```diff
--- a/api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java
+++ b/api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java
@@ -73,2 +73,2 @@
-        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "*"));
+        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "http://localhost:8080"));
```
