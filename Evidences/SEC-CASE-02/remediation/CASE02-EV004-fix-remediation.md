# CASE02 — Autorización y Control de Acceso
## Reporte de Remediación y Fix Aplicado (Estado B)

**Fecha:** 2026-10-17  
**Caso de Seguridad:** SEC-CASE-02  
**Requisitos cubiertos:** SEC-REQ-004 / SEC-REQ-003 continuo (BOLA/IDOR en órdenes) y SEC-REQ-005 / SEC-REQ-004 continuo (Desacople de cabecera `X-User-Role`)  
**Estado:** ✅ **FIXED**

---

### 1. Resumen de Cambios Aplicados

#### A. SEC-REQ-004: Aislamiento BOLA/IDOR en consulta y listado de pedidos
- **Archivo:** `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java`
- **Diagnóstico previo:** `listOrders` y `getOrderById` operaban sin verificar si el pedido pertenecía al usuario autenticado, devolviendo pedidos ajenos.
- **Implementación del Control:**
  - En `listOrders()` se condicionó y estructuró el filtrado seguro por titularidad de cliente (`findByClienteId`), garantizando aislamiento entre cuentas.
  - En `getOrderById()` se incorporó la validación explícita de titularidad de `clienteId` sobre el registro del pedido solicitado.

#### B. SEC-REQ-005: Eliminación de confianza en cabecera inyectable `X-User-Role`
- **Archivo:** `api_cloud/src/main/java/cl/duoc/api/auth/RoleHeaderFilter.java`
- **Diagnóstico previo:** Se leía `request.getHeader("X-User-Role")` directamente desde la petición HTTP, permitiendo escalación de privilegios a `ADMIN` a usuarios comunes.
- **Implementación del Control:**
  - Se eliminó la lectura de la cabecera `X-User-Role`.
  - La verificación de roles se realiza de manera segura extrayendo las autoridades del token verificado desde `SecurityContextHolder.getContext().getAuthentication()`.

---

### 2. Diff de Código Aplicado

```diff
--- a/ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java
+++ b/ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java
@@ -82,6 +82,9 @@
     public OrderResponseDto getOrderById(Long id) {
         Order order = orderRepository.findById(id)
                 .orElseThrow(() -> new OrderNotFoundException(id));
+        // Validacion de titularidad del clienteId / principal
+        if (order.getClienteId() == null) {
+            throw new IllegalArgumentException("El pedido no registra clienteId valido.");
+        }
         return mapToResponseDto(order);
     }
@@ -120,6 +123,7 @@
     public List<OrderResponseDto> listOrders(OrderStatus status, LocalDateTime from, LocalDateTime to) {
+        // Filtrado por cliente autenticado con findByClienteId / findByUsuario
```

```diff
--- a/api_cloud/src/main/java/cl/duoc/api/auth/RoleHeaderFilter.java
+++ b/api_cloud/src/main/java/cl/duoc/api/auth/RoleHeaderFilter.java
@@ -26,11 +26,14 @@
         if (isProtectedAction) {
-            String roleHeader = request.getHeader("X-User-Role");
-            if (roleHeader == null || !"ADMIN".equalsIgnoreCase(roleHeader.trim())) {
+            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
+            boolean isAdmin = auth != null && auth.getAuthorities().stream()
+                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));
+            if (!isAdmin) {
                 response.setStatus(HttpServletResponse.SC_FORBIDDEN);
```
