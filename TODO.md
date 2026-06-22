# TODO - Pagos Recepcionista

- [ ] Corregir diseño del modal de “Registrar Pago” (HTML/CSS) para que quede profesional.
- [x] Mejorar `src/main/resources/templates/Recepcionista/pagos.html` (modal más pro, feedback, enctype para voucher).
- [x] Mejorar `src/main/resources/static/css/pagos.css` (estética profesional y consistencia).
- [ ] Añadir soporte para mostrar información de cita al registrar pago (fecha/hora y monto o estado de pago), porque `Cita` no tiene monto.
- [ ] Implementar endpoint `GET` para obtener info de cita/pago asociado (fechaHora + monto/estado desde `Pago`).
- [ ] Actualizar `src/main/resources/static/js/pagos.js` para autollenar campos del modal con base en `citaId`.
- [ ] Actualizar el modal (`pagos.html`) con un bloque de “Info de Cita” que se llene al seleccionar `citaId`.


