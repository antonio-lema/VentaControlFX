# Análisis Arquitectónico Detallado - VentaControlFX

Este documento proporciona una justificación arquitectónica exhaustiva de la estructura de VentaControlFX. El diseño del sistema ha evolucionado estructurando sus capas para garantizar un alto nivel de mantenibilidad, testabilidad y escalabilidad, adaptándose a los estándares empresariales del desarrollo de software.

## 1. Patrones Arquitectónicos y Dirección de Dependencias

La arquitectura del proyecto se fundamenta en los principios de diseño de **Clean Architecture** (propuesta por Robert C. Martin) e integra conceptos de la **Arquitectura Hexagonal** o "Puertos y Adaptadores" (definida por Alistair Cockburn).

### Dirección de Dependencias (La Regla de Oro)
El pilar fundamental de este diseño no es solo la separación en carpetas, sino la estricta **dirección de las dependencias**, las cuales siempre apuntan hacia el interior, hacia el Dominio:
- `Presentation` → `Application` → `Domain`
- `Infrastructure` → `Domain`
*(La capa de Dominio no tiene dependencias de ninguna otra capa. No sabe que existe una base de datos ni una interfaz de usuario).*

---

## 2. Raíz y Archivos de Configuración Global

- **`pom.xml`**: Archivo de configuración de Maven. Estandariza la resolución de dependencias (ej. `mysql-connector-j`, `fontawesomefx`) y el ciclo de vida de construcción (empaquetado del Fat JAR), evitando la gestión manual de binarios y asegurando la reproducibilidad del entorno.
- **`README.md` & `ESTRUCTURA_PROYECTO.md`**: Documentación técnica esencial para el "onboarding" de desarrolladores e instrucciones de despliegue.
- **`run.bat` & `nbactions.xml`**: Scripts de automatización para la ejecución del entorno de desarrollo y definición de perfiles de ejecución en el IDE.

---

## 3. Código Fuente (`src/main/java/com/mycompany/ventacontrolfx`)

### 3.1. `App.java` (Entry Point)
Punto de entrada de la aplicación. Su responsabilidad se limita a inicializar el entorno de JavaFX, arrancar el contenedor de Inversión de Control (IoC) y delegar el control a la primera vista del flujo de navegación.

### 3.2. Capa `domain` (El Núcleo del Negocio)
Es la capa más interna. Contiene las reglas empresariales puras y es completamente agnóstica a frameworks externos o mecanismos de persistencia.
*   **`model`**: Entidades del dominio (ej. `Product`, `Sale`). Son estructuras de datos (POJOs/Records) con alta cohesión que representan el estado interno.
*   **`repository`**: Define los puertos de salida (Interfaces como `IProductRepository`). Establece los contratos que la infraestructura debe cumplir para persistir datos, aplicando el Principio de Inversión de Dependencias (D en SOLID).
*   **`service`**: Contiene reglas de negocio complejas (`ProductValidator`) que no pertenecen a una única entidad pero que son intrínsecas al dominio.

### 3.3. Capa `application` (Casos de Uso)
Actúa como orquestador del flujo de datos hacia y desde las entidades del dominio.
*   **`usecase`**: Clases encapsuladas (ej. `CashClosureUseCase`) que exponen las intenciones concretas del sistema (la lógica de la aplicación). Coordinan repositorios y entidades sin conocer detalles de la UI.

### 3.4. Capa `infrastructure` (Adaptadores Secundarios)
Maneja los detalles técnicos, persistencia externa y frameworks perimetrales.
*   **`persistence`**: Implementación concreta de los repositorios del dominio (ej. `JdbcProductRepository`). Actúan como *Adaptadores* que traducen las llamadas del dominio a consultas JDBC/SQL para MySQL.
*   **`config` (`ServiceContainer.java` & `Injectable.java`)**: Actúa como un contenedor de Inversión de Control (IoC) simplificado y manual. Resuelve dependencias en tiempo de inicialización de la aplicación y evita la instanciación directa (`new`) entre capas. Esta aproximación reduce el acoplamiento y prepara el terreno para una eventual migración a frameworks más complejos como Spring o Dagger.
*   **`email`**: Adaptadores para servicios externos, manejando el protocolo SMTP (`SmtpEmailAdapter`).

### 3.5. Capa `presentation` (Adaptadores Primarios)
Componentes del paradigma MVC específicos de JavaFX que interactúan con el usuario.
*   **`controller`**: Reciben los eventos de la vista (UI), extraen los parámetros, invocan los casos de uso correspondientes en la capa de `application` y actualizan la respuesta en la vista. No contienen lógica de negocio.
*   **`renderer`**: Optimizadores gráficos para componentes de UI complejos (ListCells, TableCells), separando la lógica de "dibujado" de los controladores de vista.

### 3.6. Módulos Auxiliares y en Transición (Legacy)
El sistema actual se encuentra en un proceso de evolución arquitectónica.
*   **`dao` y `service` (Legacy)**: Se mantiene una arquitectura híbrida como estrategia de migración incremental. Componentes heredados conviven con la nueva estructura Clean Architecture para evitar una reescritura completa y abrupta (Big Bang rewrite), lo que mitiga el riesgo de introducir regresiones críticas en un sistema productivo.
*   **`util`**: Funciones de conveniencia puras y transversales (ej. `ValidationUtil`).

### 3.7. Módulos Transversales (`shared`, `component`, `control`)
*   **`shared/bus` (`GlobalEventBus.java`)**: Implementa un mecanismo de comunicación desacoplada basado en el patrón *Pub/Sub*. Permite la notificación de eventos entre componentes distantes sin generar dependencias directas.
*   **`shared/async` (`AsyncManager.java`)**: Gestión controlada de hilos (thread pool) para delegar tareas de I/O bloqueantes fuera del JavaFX Application Thread.
*   **`component` / `control`**: Controles de UI personalizados extendiendo los nodos nativos de JavaFX.

---

## 4. Recursos del Sistema (`src/main/resources/...`)

La separación entre el código de comportamiento (`.java`) y los recursos de configuración o maquetación constituye una práctica estándar.

*   **`config/db.properties`**: Externalización de las credenciales de la base de datos. Una práctica fundamental de seguridad y operaciones (SecOps/DevOps) para evitar la filtración de variables de entorno sensibles en el repositorio de código.
*   **`view/*.fxml`**: Maquetación declarativa. Permite el diseño independiente bajo el patrón Modelo-Vista-Controlador de JavaFX.
*   **`styles/` (Modern Modular CSS)**: Centralización de los tokens de diseño (variables semánticas) en `variables.css` y modularización por componentes (`botones.css`, `tablas.css`, etc.). Este enfoque modular garantiza la trazabilidad y la coherencia visual, permitiendo configuraciones dinámicas y escalables (como el intercambio de temas claro/oscuro) de forma mucho más eficiente que un único archivo monolítico.

---

## 5. Análisis de Trade-offs (Decisiones de Diseño)

Toda arquitectura conlleva compromisos. La implementación de Clean/Hexagonal en este proyecto asume los siguientes *trade-offs*:

### Ventajas (Pros)
- **Alta testabilidad**: El aislamiento del dominio permite pruebas unitarias sin depender de la base de datos o de JavaFX.
- **Sustitución de tecnologías**: Se puede cambiar MySQL por PostgreSQL, o JavaFX por una API REST, sin modificar el código de negocio.
- **Mantenibilidad a largo plazo**: Las responsabilidades segregadas previenen el patrón "God Object".

### Desventajas y Costes (Cons)
- **Mayor complejidad inicial**: Requiere definir interfaces y adaptadores, lo que introduce un nivel de abstracción que retrasa las primeras entregas.
- **Overhead de clases y boilerplates**: Mayor proliferación de archivos (Modelos, Casos de Uso, Interfaces de Repositorios, Implementaciones, Controladores).
- **Curva de aprendizaje superior**: Requiere que los nuevos integrantes del equipo entiendan fuertemente los flujos de Inversión de Dependencias (IoC) y Polimorfismo.
- **Posible Sobre-Ingeniería (Overkill)**: Para módulos extremadamente simples (ej. un CRUD básico), recorrer las 4 capas estrictas puede resultar excesivo en comparación con una arquitectura monolítica tradicional de 2 capas.
