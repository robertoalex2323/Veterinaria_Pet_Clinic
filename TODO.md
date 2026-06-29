# TODO - Ajuste comprobante de pago PET{año}

- [x] Cambiar `PdfPagoController` para que el fallback de comprobante use `Year.now()` (PET{año}-00001) en vez de `PET2026-00001`.

- [x] Refactor en `PagoRepository`: reemplazar queries hardcodeadas `PET2026-%` por queries parametrizadas por prefijo/año (por ejemplo `PET2026-%` => `:prefijo-%`).

- [x] Refactor en `PagoService`: reemplazar `obtenerMaxComprobantePet2026()` por un método genérico que use el año actual (`Year.now()`).

- [ ] Asegurar que compila: ejecutar `mvn -q test` (o al menos `mvn -q -DskipTests=false test`) y/o `mvn -q test` luego de los cambios.

