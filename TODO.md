# TODO - Recomendaciones exitosas

- [x] Crear entidad `Recomendacion` (producto, categoria, razon, vendedor, cliente, fecha).
- [x] Crear `RecomendacionRepository` con `countByFechaBetween`.

- [x] Implementar endpoint `POST /vendedor/recomendaciones/registrar` en `VendedorRecomendacionesController`.
  - [x] Asociar también con cliente (además de producto).

- [x] Actualizar `recomendaciones-vendedor.js` para renderizar botón “Marcar como exitosa”.
  - [x] Al hacer clic, llamar endpoint POST y guardar en BD.
- [x] Actualizar `VendedorController#dashboardMetrics` para usar el conteo real de hoy.

- [ ] (Opcional) Ajustar CSS/UX en tarjetas.
- [ ] Ejecutar `mvnw test` y validar en runtime Dashboard/Caja.

