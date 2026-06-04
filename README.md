# 🐾 Veterinaria Pet Clinic - Módulo Recepcionista
**Sistema Inteligente de Gestión y Monitoreo Veterinario**

![Spring Boot](https://img.shields.io/badge/Spring_Boot-F27338?style=for-the-badge&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-%23005C00.svg?style=for-the-badge&logo=Thymeleaf&logoColor=white)

---

## 🎤 Descripción del Proyecto
Este sistema está diseñado para modernizar la atención veterinaria mediante un **sistema inteligente de monitoreo**. El **Módulo de Recepcionista** es el núcleo operativo que gestiona la experiencia del cliente desde su ingreso, asegurando que el flujo de datos clínicos sea preciso para el análisis posterior con Machine Learning.

---

## 📌 Funcionalidades Clave (Recepcionista)
| Proceso | Descripción |
| :--- | :--- |
| **🔐 Seguridad y Acceso** | Control de autenticación mediante roles con `SecurityConfig`. |
| **👥 Gestión de Clientes** | Registro completo, actualización y visualización detallada de propietarios. |
| **🐕 Control de Mascotas** | Gestión de historias clínicas vinculadas a cada cliente. |
| **📅 Agenda y Citas** | Programación dinámica de consultas y visualización del calendario médico. |
| **🔔 Notificaciones Real-time** | Sistema de alertas con WebSockets (STOMP), sonidos de notificación y persistencia local. |
| **💳 Módulo de Pagos** | Procesamiento de pagos con soporte visual para **Yape y Plin** mediante QR. |

---

## 🏗️ Arquitectura del Sistema
El proyecto sigue un patrón **MVC (Model-View-Controller)** estructurado de la siguiente manera:

### 📂 Backend (Java & Spring Boot)
* **`config/`**: Configuración de seguridad, WebSockets (STOMP) y propiedades de la app.
* **`controller/`**: Gestión de rutas. Destaca `RecepcionistaController.java` para toda la lógica de flujo.
* **`model/`**: Entidades core (Cita, Mascota, Pago, Usuario, etc.).
* **`repository/`**: Interfaces de comunicación con PostgreSQL vía JPA.
* **`service/`**: Lógica de negocio, validación de horarios en `AgendaService` y envío de notificaciones (Email y WebSocket).

### 📂 Frontend (Thymeleaf & Static Assets)
* **`templates/`**: Vistas modulares segmentadas para el recepcionista (Dashboard, Diagnóstico, Formularios).
* **`static/css/`**: Estilos independientes para cada módulo (Sidebar, Pagos, Perfil).
* **`static/js/`**: Lógica de cliente. Destaca `notificaciones-ui.js` para la gestión de alertas en tiempo real.
* **`static/audio/`**: Recursos sonoros para alertas del sistema.

---

## 🗺️ Roadmap del Proyecto (Entregables)

### 🗓️ Unidad 1: Cimientos y Módulo Base (Semanas 1-6)
- [x] **Configuración:** Repositorio GitHub y arquitectura del proyecto.
- [x] **Seguridad:** Implementación de Login y roles de usuario.
- [x] **Módulo Recepcionista (Base):** Gestión de Clientes y Mascotas.
- [x] **APF1:** Presentación de arquitectura y prototipo inicial.

### 🗓️ Unidad 2: Inteligencia y Notificaciones (Semanas 7-11)
- [ ] **Módulo de Citas:** Lógica de `AgendaService`, generación de slots y validación de horarios.
- [ ] **Notificaciones:** Alertas en tiempo real con WebSockets, historial persistente y sonidos.
- [ ] **APF2:** Despliegue en la nube e integración de logs.

### 🗓️ Unidad 3: Finalización y Dashboard (Semanas 12-18)
- [ ] **Módulo de Pagos:** Integración total de facturación y códigos QR.
- [ ] **Dashboard Final:** Visualización de métricas de pacientes atendidos.
- [ ] **PROYECTO FINAL:** Entrega del sistema completo e inteligente.

---

## 🛠️ Stack Tecnológico
* **Backend:** Java 21, Spring Boot, Spring Security, Spring WebSocket (STOMP).
* **Base de Datos:** PostgreSQL.
* **Frontend:** HTML5, CSS3, JavaScript, Thymeleaf.
* **Herramientas:** Git, Maven, Docker.

---

## 🚀 Instalación y Uso
1. **Clonar repositorio:**
   ```bash
   git clone [https://github.com/robertoalex2323/Veterinaria_Pet_Clinic.git](https://github.com/robertoalex2323/Veterinaria_Pet_Clinic.git)
   
2. **Configurar DB: Ajustar application.properties con tus credenciales locales.**

3. **Ejecutar: ./mvnw spring-boot:run**
