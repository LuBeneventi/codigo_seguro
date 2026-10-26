# 16. DevSecOps Challenge — Integración de Controles en Pipeline CI/CD

**Proyecto:** Pedidos360 Coffee Store  
**Fecha:** 2026-10-19  
**Objetivo:** Automatizar la verificación de seguridad incorporando controles defensivos directos en el pipeline de Integración Continua y Despliegue Continuo (CI/CD), estableciendo **Security Gates** infranqueables para proteger el paso hacia producción.

---

## 1. Arquitectura Conceptual del Pipeline DevSecOps

El pipeline sigue el principio de **Shift-Left Security**, detectando vulnerabilidades en las fases más tempranas del ciclo de desarrollo antes de que el código sea compilado o desplegado:

```text
 ┌─────────────┐
 │  Developer  │
 └──────┬──────┘
        │ git commit (Pre-commit hooks: Gitleaks / Linting)
        ▼
 ┌─────────────┐
 │ Repository  │ (GitHub / GitLab / Bitbucket)
 └──────┬──────┘
        │ Trigger: Push / Pull Request a rama main
        ▼
 ┌─────────────┐
 │    Build    │ Compilación de módulos Maven (Java 17) & Frontend (React/Vite)
 └──────┬──────┘
        │
   ┌────┴────────────────────────┐
   ▼                             ▼
┌───────┐                   ┌────────┐
│ SAST  │ (Semgrep)         │  SCA   │ (Trivy / OSV Scanner)
└───┬───┘                   └───┬────┘
    └──────────────┬────────────┘
                   ▼
       ┌───────────────────────┐
       │     Security Tests    │ Suite automatizada (run_automated_security_tests.py)
       └───────────┬───────────┘
                   ▼
       ═════════════════════════  [ SECURITY GATES ]  ═════════════════════════
                   │
         ¿Todos los Gates Aprobados?
                   ├───────────► [ NO ] ───►  🚨 BLOCK (Falla el Pipeline / PR cerrado)
                   ▼ [ SÍ ]
       ┌───────────────────────┐
       │    Build / Deploy     │ Empaquetado Docker y Despliegue a Producción
       └───────────────────────┘
```

---

## 2. Definición y Justificación de los Security Gates

Los **Security Gates** son políticas automatizadas que evalúan la severidad y el tipo de hallazgo para determinar si el pipeline tiene autorización para continuar o debe abortar de inmediato (*Build Failure*):

| Condición del Hallazgo | Acción del Security Gate | Justificación Técnica de la Decisión |
|---|:---:|---|
| **Secrets Detected** (Gitleaks) | 🚨 **BLOCK** | **Tolerancia Cero:** El compromiso de una clave privada (`JWT_SECRET`, passwords de base de datos) invalida inmediatamente todas las defensas perimetrales y permite acceso administrativo sin restricciones. Nunca debe ingresar al historial de Git. |
| **Critical Finding** (SAST/SCA) | 🚨 **BLOCK** | Hallazgos con criticidad CVSS $\ge 9.0$ (ej. Inyección SQL directa, bypass de autenticación o RCE) exponen la base de datos o el servidor a toma de control inmediata. No existe justificación operativa para desplegar vulnerabilidades críticas conocidas. |
| **High Finding** (SAST/SCA) | ⚠️ **BLOCK / Review** | Hallazgos de severidad alta CVSS 7.0–8.9 (ej. BOLA/IDOR, Stored XSS, CORS wildcard `*`). Bloquean el despliegue a producción de forma predeterminada salvo que exista un ticket formal de excepción aprobado y firmado por el oficial de seguridad con un control compensatorio documentado. |
| **Security Tests FAIL** | 🚨 **BLOCK** | Si cualquiera de las 16 pruebas automatizadas de regresión de seguridad (`run_automated_security_tests.py`) falla, significa que se introdujo una regresión en el código (ej. alguien removió Bean Validation o reintrodujo `dangerouslySetInnerHTML`). Bloqueo inmediato. |
| **Medium / Low Finding** | ℹ️ **WARN / Track** | Hallazgos informativos o de bajo impacto (ej. cabeceras secundarias ausentes). Generan advertencias en el log del pipeline y se registran automáticamente como issues en el backlog de seguridad sin interrumpir el despliegue urgente. |

---

## 3. Implementación Práctica del Pipeline (GitHub Actions)

El pipeline automatizado se encuentra implementado en el repositorio en el archivo:  
[`D:\Escritorio\CloudComputing_Seguro\.github\workflows\devsecops.yml`](file:///D:/Escritorio/CloudComputing_Seguro/.github/workflows/devsecops.yml)

### Estructura de las Etapas en el Manifiesto YAML:

1. **Escaneo de Secretos (`gitleaks`):**  
   Ejecuta `gitleaks-action` sobre el árbol completo del commit antes de compilar.
   ```yaml
   - name: Run Gitleaks Scanner
     uses: gitleaks/gitleaks-action@v2
   ```

2. **Compilación y Build (`build`):**  
   Verifica la integridad de compilación con Maven (`mvn -q -DskipTests compile`) en los 4 microservicios backend y `npm run build` en el frontend.

3. **Análisis Estático SAST (`sast`):**  
   Ejecuta Semgrep con rulesets de OWASP Top 10 y auditoría de seguridad:
   ```yaml
   - name: Run Semgrep SAST
     uses: returntocorp/semgrep-action@v1
     with:
       config: p/default p/owasp-top-ten p/security-audit
   ```

4. **Análisis de Dependencias SCA (`sca`):**  
   Ejecuta Trivy para detectar CVEs en dependencias Maven y paquetes npm, configurado con `exit-code: 1` ante severidades `CRITICAL`:
   ```yaml
   - name: Run Trivy Vulnerability Scanner
     uses: aquasecurity/trivy-action@master
     with:
       scan-type: 'fs'
       severity: 'CRITICAL,HIGH'
       exit-code: '1' # SECURITY GATE: Falla el pipeline ante vulnerabilidades críticas
   ```

5. **Pruebas Automatizadas de Regresión (`security-tests`):**  
   Ejecuta la suite en Python y valida que el total de fallas sea estrictamente `0`:
   ```yaml
   - name: Execute Security Regression Test Suite
     run: |
       python run_automated_security_tests.py
       python -c "
       import json, sys
       with open('automated-security-test-results.json') as f:
           data = json.load(f)
       fails = [t for v in data.values() for t in v if t['status'] != 'PASS']
       if fails:
           print(f'SECURITY GATE ERROR: {len(fails)} tests fallaron.')
           sys.exit(1)
       "
   ```

6. **Despliegue Autorizado (`deploy`):**  
   Solo se desbloquea si **todos** los jobs anteriores concluyeron con estado exitoso.

---

## 4. Guía de Ejecución Local (Para Desarrolladores)

Si no se cuenta con un runner de GitHub Actions activo o se desea verificar el pipeline antes de hacer push al repositorio remoto:

1. **Pre-commit Scan de Secretos:**
   ```bash
   # Escaneo local con Gitleaks
   gitleaks detect --source . -v
   ```

2. **Verificación SAST Local:**
   ```bash
   semgrep scan --config p/owasp-top-ten --error
   ```

3. **Verificación SCA Local:**
   ```bash
   python osv_scanner.py
   ```

4. **Ejecución de la Suite de Security Gates:**
   ```bash
   python run_automated_security_tests.py
   ```
   *Criterio de aprobación local:* **Total Tests: 16 | Fails: 0 | Passes: 16**.
