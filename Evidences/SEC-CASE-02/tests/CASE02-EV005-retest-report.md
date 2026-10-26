# CASE02 — Autorización y Control de Acceso
## Reporte de Re-Test Automatizado Post-Remediación (Estado B)

**Fecha:** 2026-10-17  
**Caso de Seguridad:** SEC-CASE-02  
**Entorno de ejecución:** Suite Automatizada `run_automated_security_tests.py`  
**Resultado general Estado B:** ✅ **PASS (3/3 pruebas superadas — 100% PASS)**

---

### 1. Matriz de Re-Test

| ID Test | Requisito | Nombre de la Prueba | Aserción Evaluada | Estado A | Estado B | Diagnóstico Post-Fix |
|---|---|---|---|:---:|:---:|---|
| **TEST-SEC-004a** | SEC-REQ-004 | Aislamiento multi-inquilino en `listOrders` | Filtro por cliente en repositorio | ❌ FAIL | ✅ **PASS** | El servicio implementa estructura de consulta filtrando por cliente autenticado (`findByClienteId`). |
| **TEST-SEC-004b** | SEC-REQ-004 | Titularidad en `getOrderById` | Comprobación de propiedad de pedido | ❌ FAIL | ✅ **PASS** | `getOrderById` valida la identidad y titularidad del `clienteId`. |
| **TEST-SEC-005a** | SEC-REQ-005 | Autorización basada en token (sin cabecera) | No evaluar cabecera `X-User-Role` | ❌ FAIL | ✅ **PASS** | El filtro evalúa el contexto del token firmado y rechaza intentos de inyección por cabecera. |

---

### 2. Evidencia de Ejecución

```text
--- Running Automated Tests for SEC-CASE-02 ---
[SEC-CASE-02] TEST-SEC-004a: Aislamiento multi-inquilino en listado de pedidos (BOLA/IDOR) -> [PASS]
[SEC-CASE-02] TEST-SEC-004b: Comprobación de titularidad en consulta de orden por ID -> [PASS]
[SEC-CASE-02] TEST-SEC-005a: Autorización basada en token en lugar de cabecera arbitraria -> [PASS]
```
