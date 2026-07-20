# Veterinaria Pet Clinic

Sistema web para la gestion integral de una clinica veterinaria, construido con Spring Boot, Java, Thymeleaf y PostgreSQL.

## Modulos integrados

- Administrador: panel general, usuarios, clientes, mascotas, citas, agenda, pagos y metricas.
- Recepcionista: registro de clientes y mascotas, agenda, citas, pagos, comprobantes y notificaciones.
- Veterinaria: dashboard clinico, pacientes, triaje, historial, signos vitales, alertas, vacunas y reportes.
- Farmaceutico: inventario, proveedores, recetas, ventas, comprobantes y reportes.
- Vendedor: ventas, boletas y mejoras de dashboard/reportes del modulo de ventas.
- Chatbot: widget y endpoints de apoyo para consultas del sistema.

## Arquitectura

El proyecto sigue el patron MVC:

- `config/`: seguridad, WebSockets, propiedades e inicializacion de datos.
- `controller/`: rutas web y APIs de los modulos.
- `model/`: entidades JPA del dominio.
- `repository/`: interfaces Spring Data JPA.
- `service/`: logica de negocio, reportes, notificaciones y reglas de agenda.
- `templates/`: vistas Thymeleaf por modulo.
- `static/`: CSS, JavaScript, imagenes y audio.

## Stack

- Backend: Java 21, Spring Boot, Spring Security.
- Base de datos: PostgreSQL.
- Frontend: Thymeleaf, HTML, CSS y JavaScript.
- Construccion: Maven.

## Ejecucion local

1. Clonar el repositorio.
2. Verificar que Java este instalado. El proyecto esta configurado para Java 21.
3. Para correr rapido sin configurar PostgreSQL, usar el perfil `local` con H2 en memoria:

```powershell
.\run-local.ps1
```

Si PowerShell bloquea scripts, usar:

```bat
run-local.bat
```

Tambien se puede ejecutar directamente:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Abrir la aplicacion en:

```text
http://localhost:8080
```

Usuarios iniciales:

| Rol | Usuario | Password |
| --- | --- | --- |
| Admin | `admin` | `admin_pet_clinic` |
| Recepcionista | `recepcionista` | `recep_pet_clinic` |
| Veterinario | `veterinario` | `veterinaria_pet_clinic` |
| Vendedor | `vendedor` | `vendedor_pet_clinic` |
| Farmaceutico | `farmaceutico` | `farmaceutico_pet_clinic` |

Nota: con el perfil `local` la base es en memoria; los datos creados manualmente se pierden al detener la app.

## Ejecucion con PostgreSQL

1. Crear una base de datos local llamada `veterinaria_pet_clinic`.
2. Configurar `src/main/resources/application.properties` con las credenciales locales de PostgreSQL. Por defecto viene asi:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/veterinaria_pet_clinic
spring.datasource.username=postgres
spring.datasource.password=12345
```

3. Ejecutar:

```bash
./mvnw spring-boot:run
```

En Windows tambien se puede usar:

```bash
mvnw.cmd spring-boot:run
```
