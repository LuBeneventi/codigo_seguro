# CASE05 — Configuración Segura
## Reporte de Re-Test Automatizado Post-Remediación (Estado B)

**Fecha:** 2026-10-17  
**Caso de Seguridad:** SEC-CASE-05  
**Entorno de ejecución:** Suite Automatizada `run_automated_security_tests.py`  
**Resultado general Estado B:** ✅ **PASS (4/4 pruebas superadas — 100% PASS)**

---

### 1. Matriz de Re-Test

| ID Test | Requisito | Nombre de la Prueba | Aserción Evaluada | Estado A | Estado B | Diagnóstico Post-Fix |
|---|---|---|---|:---:|:---:|---|
| **TEST-SEC-013** | SEC-REQ-013 / 012 | Restricción estricta de orígenes CORS | Ausencia de comodín `*` en CORS | ❌ FAIL | ✅ **PASS** | Orígenes limitados al frontend; sin comodines. |
| **TEST-SEC-012** | SEC-REQ-012 / 011 | Ausencia de contraseñas y secretos débiles | Sin `changeme`, `postgres`, `guest` | ❌ FAIL | ✅ **PASS** | `docker-compose.yml` libre de credenciales por defecto predecibles. |
| **TEST-SEC-014a** | SEC-REQ-014 / 013 | Aislamiento de puertos de microservicios | Sin puertos 8086, 8087, 5432, 5672 | ❌ FAIL | ✅ **PASS** | Puertos de persistencia y servicios internos aislados en la red Docker. |
| **TEST-SEC-014b** | SEC-REQ-014 / 013 | Instrucción USER no-root en contenedores | `USER` presente en todos los Dockerfiles | ❌ FAIL | ✅ **PASS** | Todos los microservicios y el frontend declaran usuario no-root. |

---

### 2. Evidencia de Ejecución

```text
--- Running Automated Tests for SEC-CASE-05 ---
[SEC-CASE-05] TEST-SEC-013: Restricción estricta de orígenes CORS -> [PASS]
[SEC-CASE-05] TEST-SEC-012: Ausencia de contraseñas y secretos por defecto débiles -> [PASS]
[SEC-CASE-05] TEST-SEC-014a: Aislamiento de puertos de microservicios y bases de datos -> [PASS]
[SEC-CASE-05] TEST-SEC-014b: Instrucción USER no-root en contenedores Docker -> [PASS]
```
