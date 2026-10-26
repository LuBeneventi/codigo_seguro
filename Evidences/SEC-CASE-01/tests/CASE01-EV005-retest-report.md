# CASE01 — Autenticación y Gestión de Sesiones
## Reporte de Re-Test Automatizado Post-Remediación (Estado B)

**Fecha:** 2026-10-17  
**Caso de Seguridad:** SEC-CASE-01  
**Entorno de ejecución:** Suite Automatizada `run_automated_security_tests.py`  
**Resultado general Estado B:** ✅ **PASS (2/2 pruebas superadas — 100% PASS)**

---

### 1. Matriz de Re-Test

| ID Test | Requisito | Nombre de la Prueba | Aserción Evaluada | Estado A | Estado B | Diagnóstico Post-Fix |
|---|---|---|---|:---:|:---:|---|
| **TEST-SEC-001** | SEC-REQ-001 | Bloqueo tras intentos fallidos de login | Mecanismo de contador / lockout implementado | ❌ FAIL | ✅ **PASS** | El controlador cuenta con `maxAttempts` (5) y `lockAccount` (15 min) respondiendo HTTP 429. |
| **TEST-SEC-003** | SEC-REQ-003 | Expiración de token JWT <= 1 hora | Expiración fijada en horas o minutos | ❌ FAIL | ✅ **PASS** | El token se genera con `plus(1, ChronoUnit.HOURS)`, cumpliendo el límite seguro. |

---

### 2. Evidencia de Ejecución

```text
--- Running Automated Tests for SEC-CASE-01 ---
[SEC-CASE-01] TEST-SEC-001: Verificación de bloqueo tras intentos fallidos de login -> [PASS]
[SEC-CASE-01] TEST-SEC-003: Expiración de token JWT de acceso <= 1 hora -> [PASS]
```
