# 11. Registro obligatorio de evidencias

Cada análisis, prueba y remediación ejecutada sobre el proyecto genera evidencia técnica verificable y auditable. A continuación se presenta el registro consolidado de evidencias para los 5 casos de seguridad del proyecto **Pedidos360 Coffee Store**, estructurado de acuerdo con la nomenclatura oficial `CASE0X-EV00Y`.

---

## 1. Convención de Nomenclatura de Evidencias

Las evidencias se organizan jerárquicamente en el directorio `Evidences/SEC-CASE-0X/` bajo la siguiente estructura estandarizada:

| Prefijo / Sufijo | Etapa / Actividad | Descripción del Artefacto |
|---|---|---|
| `CASE0X-EV001` (SAST) | **SAST** | Reporte estático de vulnerabilidades detectadas en código fuente (Semgrep y marcadores `BASELINE-VULN`). |
| `CASE0X-EV001` (SCA) | **SCA** | Reporte de composición de software y análisis de dependencias (OSV Scanner / CVEs / GHSA). |
| `CASE0X-EV001` (DAST) | **DAST** | Reporte dinámico de transacciones HTTP y vectores de ataque API (OWASP WSTG / API Top 10). |
| `CASE0X-EV001` (Tests) | **Security Tests** | Reporte de ejecución de pruebas automatizadas sobre la línea base (Estado A - Baseline). |
| `CASE0X-EV002` | **Data Cruda / Specs** | Artefactos crudos JSON con hallazgos, especificaciones DAST o aserciones de tests. |
| `CASE0X-EV003` | **Test Baseline** | Captura o registro de ejecución individual de prueba que evidencia el fallo (**FAIL**) en Estado A. |
| `CASE0X-EV004` | **Fix / Remediation** | Reporte de remediación y diff de cambios de código aplicados para el Estado B. |
| `CASE0X-EV005` | **Re-Test** | Reporte de re-evaluación automatizada que certifica el resultado exitoso (**PASS**) en Estado B. |

---

## 2. Matriz Consolidada de Evidencias por Caso

### SEC-CASE-01: Autenticación y gestión de sesiones
| ID Evidencia | Etapa | Acción / Prueba | Resultado | Tipo de Artefacto | Ubicación del Archivo |
|---|---|---|:---:|---|---|
| **CASE01-EV001** | **SAST** | Análisis estático de login y ciclo de vida de tokens JWT | ❌ **FAIL** (2 Hallazgos) | Reporte Markdown | `Evidences/SEC-CASE-01/SAST/CASE01-EV001-sast-report.md` |
| **CASE01-EV002** | **SCA** | Análisis de dependencias (Tomcat 10.1.30, Spring Security) | ❌ **FAIL** (37 CVEs) | Dataset JSON / Reporte | `Evidences/SEC-CASE-01/SCA/CASE01-EV002-osv-vulns.json` |
| **CASE01-EV003** | **Test Baseline** | Ejecución de `TEST-SEC-001` (Fuerza bruta) y `TEST-SEC-003` (JWT 30d) | ❌ **FAIL** | Reporte / Aserciones | `Evidences/SEC-CASE-01/tests/CASE01-EV001-test-report.md` |
| **CASE01-EV004** | **Fix** | Implementación de `lockAccount` (5 intentos) y expiración JWT $\le 1\,\text{h}$ | ✅ **APPLIED** | Diff / Código fuente | `Evidences/SEC-CASE-01/remediation/CASE01-EV004-fix-remediation.md` |
| **CASE01-EV005** | **Re-Test** | Re-ejecución automatizada de `TEST-SEC-001` y `TEST-SEC-003` | ✅ **PASS** | Reporte de Re-test | `Evidences/SEC-CASE-01/tests/CASE01-EV005-retest-report.md` |

---

### SEC-CASE-02: Autorización y control de acceso
| ID Evidencia | Etapa | Acción / Prueba | Resultado | Tipo de Artefacto | Ubicación del Archivo |
|---|---|---|:---:|---|---|
| **CASE02-EV001** | **SAST** | Análisis estático de control de acceso en órdenes y roles | ❌ **FAIL** (5 Hallazgos) | Reporte Markdown | `Evidences/SEC-CASE-02/SAST/CASE02-EV001-sast-report.md` |
| **CASE02-EV002** | **SCA** | Detección de bypass de filtros en Tomcat y Spring Framework | ❌ **FAIL** (37 CVEs) | Dataset JSON / Reporte | `Evidences/SEC-CASE-02/SCA/CASE02-EV002-osv-vulns.json` |
| **CASE02-EV003** | **Test Baseline** | Ejecución `TEST-SEC-004a/b` (BOLA/IDOR) y `TEST-SEC-005a` (Rol por cabecera) | ❌ **FAIL** | Reporte / Aserciones | `Evidences/SEC-CASE-02/tests/CASE02-EV001-test-report.md` |
| **CASE02-EV004** | **Fix** | Filtrado `findByClienteId` y desacople total de cabecera `X-User-Role` | ✅ **APPLIED** | Diff / Código fuente | `Evidences/SEC-CASE-02/remediation/CASE02-EV004-fix-remediation.md` |
| **CASE02-EV005** | **Re-Test** | Re-ejecución automatizada de `TEST-SEC-004a`, `004b` y `005a` | ✅ **PASS** | Reporte de Re-test | `Evidences/SEC-CASE-02/tests/CASE02-EV005-retest-report.md` |

---

### SEC-CASE-03: Seguridad de API
| ID Evidencia | Etapa | Acción / Prueba | Resultado | Tipo de Artefacto | Ubicación del Archivo |
|---|---|---|:---:|---|---|
| **CASE03-EV001** | **SAST** | Análisis de Mass Assignment, fuga de contraseñas y Rate Limiting | ❌ **FAIL** (6 Hallazgos) | Reporte Markdown | `Evidences/SEC-CASE-03/SAST/CASE03-EV001-sast-report.md` |
| **CASE03-EV002** | **SCA** | Vulnerabilidades en Jackson Databind, Spring WebMVC y RabbitMQ | ❌ **FAIL** (22 CVEs) | Dataset JSON / Reporte | `Evidences/SEC-CASE-03/SCA/CASE03-EV002-osv-vulns.json` |
| **CASE03-EV003** | **Test Baseline** | Ejecución `TEST-SEC-006a/b` (Precios), `007a` (Hash) y `008` (Rate Limit) | ❌ **FAIL** | Reporte / Aserciones | `Evidences/SEC-CASE-03/tests/CASE03-EV001-test-report.md` |
| **CASE03-EV004** | **Fix** | DTO estricto, fijación de precios en catálogo, `@JsonIgnore` y Rate Limiter | ✅ **APPLIED** | Diff / Código fuente | `Evidences/SEC-CASE-03/remediation/CASE03-EV004-fix-remediation.md` |
| **CASE03-EV005** | **Re-Test** | Re-ejecución automatizada de `TEST-SEC-006a/b`, `007a` y `008` | ✅ **PASS** | Reporte de Re-test | `Evidences/SEC-CASE-03/tests/CASE03-EV005-retest-report.md` |

---

### SEC-CASE-04: Validación y sanitización de entradas
| ID Evidencia | Etapa | Acción / Prueba | Resultado | Tipo de Artefacto | Ubicación del Archivo |
|---|---|---|:---:|---|---|
| **CASE04-EV001** | **SAST** | Análisis de inyección SQL, Bean Validation y DOM XSS | ❌ **FAIL** (Semgrep + 5) | Reporte Markdown | `Evidences/SEC-CASE-04/SAST/CASE04-EV001-sast-report.md` |
| **CASE04-EV002** | **SCA** | Vulnerabilidades en PostgreSQL JDBC Driver y Vite dev server | ❌ **FAIL** (47 CVEs) | Dataset JSON / Reporte | `Evidences/SEC-CASE-04/SCA/CASE04-EV002-osv-vulns.json` |
| **CASE04-EV003** | **Test Baseline** | Ejecución `TEST-SEC-010a` (SQLi), `010b` (@Min/@Positive) y `009` (DOM XSS) | ❌ **FAIL** | Reporte / Aserciones | `Evidences/SEC-CASE-04/tests/CASE04-EV001-test-report.md` |
| **CASE04-EV004** | **Fix** | Parametrización JPA (`:q`), restauración Bean Validation y reemplazo JSX | ✅ **APPLIED** | Diff / Código fuente | `Evidences/SEC-CASE-04/remediation/CASE04-EV004-fix-remediation.md` |
| **CASE04-EV005** | **Re-Test** | Re-ejecución automatizada de `TEST-SEC-010a`, `010b` y `009` | ✅ **PASS** | Reporte de Re-test | `Evidences/SEC-CASE-04/tests/CASE04-EV005-retest-report.md` |

---

### SEC-CASE-05: Configuración segura
| ID Evidencia | Etapa | Acción / Prueba | Resultado | Tipo de Artefacto | Ubicación del Archivo |
|---|---|---|:---:|---|---|
| **CASE05-EV001** | **SAST** | Análisis de Dockerfiles (root), secretos débiles y CORS wildcard | ❌ **FAIL** (9 Semgrep + 5) | Reporte Markdown | `Evidences/SEC-CASE-05/SAST/CASE05-EV001-sast-report.md` |
| **CASE05-EV002** | **SCA** | Análisis de componentes base de imágenes y librerías de infraestructura | ❌ **FAIL** (57 CVEs) | Dataset JSON / Reporte | `Evidences/SEC-CASE-05/SCA/CASE05-EV002-osv-vulns.json` |
| **CASE05-EV003** | **Test Baseline** | Ejecución `TEST-SEC-013` (CORS), `012` (Secretos), `014a` (Puertos) y `014b` (Root) | ❌ **FAIL** | Reporte / Aserciones | `Evidences/SEC-CASE-05/tests/CASE05-EV001-test-report.md` |
| **CASE05-EV004** | **Fix** | Orígenes específicos CORS, CSP, hardening de puertos, secretos y `USER nginx` | ✅ **APPLIED** | Diff / Código fuente | `Evidences/SEC-CASE-05/remediation/CASE05-EV004-fix-remediation.md` |
| **CASE05-EV005** | **Re-Test** | Re-ejecución automatizada de `TEST-SEC-013`, `012`, `014a` y `014b` | ✅ **PASS** | Reporte de Re-test | `Evidences/SEC-CASE-05/tests/CASE05-EV005-retest-report.md` |

---

## 3. Resumen de Ejecución y Trazabilidad de Evidencias

- **Total de Evidencias Documentadas:** 25 artefactos trazables (5 por cada uno de los 5 casos de seguridad).
- **Cobertura del Ciclo DevSecOps:**
  1. Detección Estática (**SAST**) $\rightarrow$ `EV-001`
  2. Análisis de Dependencias (**SCA**) $\rightarrow$ `EV-002`
  3. Demostración de Falla en Baseline (**Test Baseline**) $\rightarrow$ `EV-003`
  4. Implementación y Parche de Seguridad (**Fix**) $\rightarrow$ `EV-004`
  5. Verificación de Eficacia Post-Remediación (**Re-Test**) $\rightarrow$ `EV-005`
- **Tasa de Éxito en Estado B:** **100% PASS** (16/16 pruebas superadas).
