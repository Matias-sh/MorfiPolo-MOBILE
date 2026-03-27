# 08 - Conventions And Patterns

## Convenciones reales observadas
- **Naming de paquetes por responsabilidad**: `ui`, `data`, `domain`, `util`.
- **Factories de ViewModel inline** por pantalla (sin DI framework).
- **Estados sellados** para representar loading/success/error.
- **Mensajes de error en español** orientados a usuario final.
- **Ventana horaria de negocio hardcodeada** (08:00-11:00) repetida en varios puntos.

## Organización y estilo de implementación
- UI basada en XML + ViewBinding, sin Compose.
- Navegación por Navigation Component con graph central.
- Repositorio como capa de orquestación de datos y fallback local.
- SharedPreferences usadas para sesión, alarmas y configuración.
- Código con fuerte instrumentación de logs en zonas complejas.

## Patrones repetidos que hoy son norma
- `ViewModel` + `StateFlow`.
- `Result<T>` para operaciones de repositorios.
- `try/catch` extensivo con mapeo manual de errores por código HTTP.
- Broadcast interno para sincronización (`MENU_UPDATED`).
- Workarounds explícitos cuando backend no responde de forma esperada.

## Múltiples estilos coexistentes
- **CONFIRMADO POR CÓDIGO**:
  - estilo "clean-ish" en parte de repos + VM.
  - estilo "imperativo defensivo" en widget/alarms/notifications.
- **INFERIDO POR ESTRUCTURA**:
  - evolución incremental por urgencias funcionales, no por homogeneidad arquitectónica.

## Decisiones que deben respetarse en cambios futuros
- Mantener `MorfipoloApplication` como punto de wiring hasta migrar DI de forma explícita.
- No romper contrato de estados UI sellados por pantalla.
- No introducir nuevas reglas de horario dispersas; centralizar si se toca.
- Preservar fallback local de menús y manejo de `TemporaryError` en auth.
- Tratar widget y alarmas como zonas de alto riesgo con pruebas manuales obligatorias.
