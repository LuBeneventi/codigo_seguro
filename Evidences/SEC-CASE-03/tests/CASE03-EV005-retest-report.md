# CASE03 — Seguridad de API
## Reporte de Re-Test Automatizado Post-Remediación (Estado B)

**Fecha:** 2026-10-17  
**Caso de Seguridad:** SEC-CASE-03  
**Entorno de ejecución:** Suite Automatizada `run_automated_security_tests.py`  
**Resultado general Estado B:** ✅ **PASS (4/4 pruebas superadas — 100% PASS)**

---

### 1. Matriz de Re-Test

| ID Test | Requisito | Nombre de la Prueba | Aserción Evaluada | Estado A | Estado B | Diagnóstico Post-Fix |
|---|---|---|---|:---:|:---:|---|
| **TEST-SEC-006a** | SEC-REQ-006 | Prevención de Mass Assignment en registro | Uso de DTO sin campo `role` | ❌ FAIL | ✅ **PASS** | El registro usa `RegisterRequestDto` y fija el rol en servidor. |
| **TEST-SEC-006b** | SEC-REQ-006 | Validación de precios de catálogo en pedidos | Sobrescritura forzosa desde catálogo | ❌ FAIL | ✅ **PASS** | `precioUnitario` se obtiene siempre del microservicio de catálogo. |
| **TEST-SEC-007a** | SEC-REQ-007 | Ocultamiento de `passwordHash` | `@JsonIgnore` en atributo de clave | ❌ FAIL | ✅ **PASS** | `User.java` contiene `@JsonIgnore` sobre `passwordHash`. |
| **TEST-SEC-008** | SEC-REQ-008 | Rate Limiter configurado en BFF | Presencia de Rate Limiter en config | ❌ FAIL | ✅ **PASS** | `SecurityConfig.java` registra configuración de Rate Limiter. |

---

### 2. Evidencia de Ejecución

```text
--- Running Automated Tests for SEC-CASE-03 ---
[SEC-CASE-03] TEST-SEC-006a: Uso de DTO dedicado en registro para prevenir Mass Assignment -> [PASS]
[SEC-CASE-03] TEST-SEC-006b: Validación estricta de precios de catálogo en creación de pedidos -> [PASS]
[SEC-CASE-03] TEST-SEC-007a: Ocultamiento de passwordHash en respuestas de API -> [PASS]
[SEC-CASE-03] TEST-SEC-008: Presencia de filtro de Rate Limiting en BFF -> [PASS]
```
