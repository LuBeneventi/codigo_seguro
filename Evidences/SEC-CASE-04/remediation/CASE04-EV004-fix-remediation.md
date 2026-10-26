# CASE04 — Validación y Sanitización de Entradas
## Reporte de Remediación y Fix Aplicado (Estado B)

**Fecha:** 2026-10-17  
**Caso de Seguridad:** SEC-CASE-04  
**Requisitos cubiertos:** SEC-REQ-009 / 008 continuo (Parametrización SQL y DOM XSS), SEC-REQ-010 / 009 continuo (Bean Validation en ítems)  
**Estado:** ✅ **FIXED**

---

### 1. Resumen de Cambios Aplicados

#### A. SEC-REQ-010a (SEC-REQ-009): Parametrización de consultas SQL en búsqueda
- **Archivo:** `ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java`
- **Diagnóstico previo:** Se concatenaba directamente el string `q` en la cláusula `WHERE name ILIKE '%" + q + "%'`, permitiendo SQL Injection.
- **Implementación del Control:**
  - Se modificó la consulta a sintaxis parametrizada: `SELECT * FROM products WHERE name ILIKE :q`.
  - Se inyectó el parámetro de forma segura con `.setParameter("q", "%" + q + "%")`.

#### B. SEC-REQ-010b: Validaciones Bean Validation en `OrderItemRequestDto`
- **Archivo:** `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/dto/OrderItemRequestDto.java`
- **Diagnóstico previo:** Las anotaciones `@Min(1)` y `@Positive` habían sido removidas.
- **Implementación del Control:**
  - Se reactivó `@Min(value = 1)` en el campo `cantidad`.
  - Se reactivó `@Positive` en el campo `precioUnitario`.

#### C. SEC-REQ-009: Eliminación de `dangerouslySetInnerHTML` en Frontend React
- **Archivo:** `frontend/src/App.tsx`
- **Diagnóstico previo:** Se renderizaban nombres, descripciones y direcciones usando `dangerouslySetInnerHTML`, expuesto a Stored DOM XSS.
- **Implementación del Control:**
  - Se reemplazaron todas las directivas de HTML crudo por interpolación estándar JSX `{expr}`, aprovechando el escape automático de React contra XSS.

---

### 2. Diff de Código Aplicado

```diff
--- a/ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java
+++ b/ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java
@@ -35,5 +35,6 @@
     @GetMapping("/search")
     public ResponseEntity<?> searchProducts(@RequestParam(value = "q", required = false, defaultValue = "") String q) {
-        String sql = "SELECT * FROM products WHERE name ILIKE '%" + (q != null ? q : "") + "%'";
-        List<?> results = entityManager.createNativeQuery(sql, Product.class).getResultList();
+        String sql = "SELECT * FROM products WHERE name ILIKE :q";
+        List<?> results = entityManager.createNativeQuery(sql, Product.class)
+                .setParameter("q", "%" + (q != null ? q : "") + "%")
+                .getResultList();
```

```diff
--- a/ms-pedidos360-orders/src/main/java/com/pedidos360/orders/dto/OrderItemRequestDto.java
+++ b/ms-pedidos360-orders/src/main/java/com/pedidos360/orders/dto/OrderItemRequestDto.java
@@ -23,4 +23,6 @@
+    @Min(value = 1, message = "La cantidad mínima debe ser 1")
     private Integer cantidad;
 
+    @Positive(message = "El precio unitario debe ser mayor a 0")
     private BigDecimal precioUnitario;
```
