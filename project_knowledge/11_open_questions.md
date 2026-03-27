# 11 - Open Questions

## Dudas no resueltas solo con código

1. ¿El backend de `GET /votes` debe filtrar por usuario autenticado, por `user_id`, por `userId` o por ambos?
   - Estado: **HIPÓTESIS DÉBIL / NO CONFIRMADA**.
   - Motivo: cliente implementa workarounds contradictorios.

2. ¿`MenuPollingWorker` debe estar activo en producción?
   - Estado: **HIPÓTESIS DÉBIL / NO CONFIRMADA**.
   - Motivo: existe implementación, no se encontró bootstrap explícito en startup.

3. ¿Guardar password en sesión local es requisito transitorio o decisión permanente?
   - Estado: **HIPÓTESIS DÉBIL / NO CONFIRMADA**.
   - Motivo: comentario "Temporal para refresh token" sin evidencia de plan de retiro.

4. ¿La coexistencia AlarmManager + WorkManager para recordatorios diarios es intencional como redundancia o deuda técnica accidental?
   - Estado: **INFERIDO POR ESTRUCTURA** (redundancia buscada).
   - Confirmación funcional: pendiente validación humana.

5. ¿`MenuSelectionDao/Entity` debe eliminarse, reactivarse o migrarse?
   - Estado: **INFERIDO POR ESTRUCTURA** (infrautilizado).
   - Confirmación de negocio: pendiente.

6. ¿Cuál es la política de rotación/seguridad del keystore y de `keystore.properties`?
   - Estado: **HIPÓTESIS DÉBIL / NO CONFIRMADA**.
   - Motivo: el archivo existe en repo, no hay política explícita en código.

## Supuestos débiles registrados
- El producto está orientado a comedor institucional con selección de almuerzo laboral.
- El backend puede ser compartido con web y eso explica sincronización por broadcast y workarounds de votos.

## Validaciones humanas requeridas
- Definir contrato backend canónico de votos.
- Confirmar políticas de seguridad (credenciales, secretos, backup).
- Confirmar si polling background forma parte del roadmap oficial.
