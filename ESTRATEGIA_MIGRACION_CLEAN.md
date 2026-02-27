# Estrategia de Migración Inteligente a Clean Architecture

Esta guía establece el plan de refactorización para transicionar el proyecto actual hacia una Arquitectura Limpia (Clean Architecture) de manera progresiva y segura, preservando la funcionalidad existente y justificando las decisiones técnicas con un estándar académico (apto para defensa de TFG).

---

## 🔹 Paso 1: Eliminar duplicidad DAO vs Persistence

En la arquitectura actual, conviven paquetes `dao/` (herencia de un enfoque clásico) e `infrastructure/persistence/` (enfoque más moderno). Esto genera confusión, duplicidad y acoplamiento.

### Cómo unificar sin romper dependencias:
1. **Definir Contratos en el Dominio (Interfaces):**
   Asegúrate de que cada entidad del dominio tenga su interfaz `IRepository` en `domain/repository/` (ej. `IProductRepository`). Esto implementa el **Patrón Repository**, que abstrae el origen de los datos.

2. **Mover Implementaciones a `infrastructure/persistence/`:**
   Toma la lógica de los antiguos `DAOs` (consultas JDBC nativas) y muévela a clases como `JdbcProductRepository` dentro de `infrastructure/persistence/` que implementen las interfaces del dominio. El **Patrón Adapter** interviene aquí: estas clases *adaptan* la tecnología (JDBC) al contrato esperado por el dominio.

3. **Inyección de Dependencias:**
   Reemplaza la instanciación manual o llamadas estáticas a los DAOs por inyección de las interfaces a través del `ServiceContainer`.

### Riesgos y Mitigación:
- **Riesgo:** Consultas rotas o errores de SQL al copiar/pegar.
- **Mitigación:** Hacer la migración entidad por entidad. No borrar el DAO original hasta que el nuevo `JdbcRepository` esté integrado y testeado manualmente en el flujo de la aplicación.
- **Riesgo:** Pérdida de rendimiento.
- **Mitigación:** Mantener el uso de `PreparedStatement` y `ResultSet`. El patrón Repository no obliga a usar ORMs, permite esconder JDBC puro detrás de una interfaz.

---

## 🔹 Paso 2: Convertir Mega-Services en Use Cases

Los clásicos "Mega-Services" (como `SaleService`) suelen acumular lógica de negocio, acoplamiento técnico (envío de correos, transacciones SQL explícitas) y estado.

### Identificar Lógica de Negocio vs Lógica Técnica:
- **Lógica de Negocio (Use Case):** Validaciones complejas (ej. "No vender si no hay stock"), cálculos de impuestos, orquestación (ej. Guardar Venta -> Restar Stock -> Notificar).
- **Lógica Técnica (Service en Infrastructure):** Cómo conectarse a la impresora térmica, cómo enviar un e-mail (`SmtpEmailAdapter`), manipulación pura de JavaFX.

### Cómo dividir un "Mega-Service":
En lugar de un `SaleService` con 20 métodos, divídelo por responsabilidad (*Single Responsibility Principle*):
- `ProcessSaleUseCase`: Orquesta el cierre de una venta.
- `ReturnSaleUseCase`: Orquesta una devolución parcial.
- `SaleHistoryUseCase`: Obtiene el historial (puede ser solo lectura).

### Reorganización de Dependencias:
1. El `Controller` orquesta la UI e invoca al `UseCase`.
2. El `UseCase` contiene la regla de negocio e invoca al `Repository` (por su interfaz).
3. El `Repository` ejecuta el SQL.

---

## 🔹 Paso 3: Garantizar la pureza del dominio

El dominio (`domain/`) es el corazón de la aplicación. Su regla de oro es: **Cero dependencias hacia afuera.**

### Tareas de auditoría:
1. **Revisar importaciones:** Si en alguna clase del paquete `domain/` hay un import que empiece por `javafx.*`, `java.sql.*`, `javax.mail.*`, o cualquier librería externa, **está contaminado**.
2. **Tipos de datos puros:** El dominio debe usar clases estándar de Java (`String`, `List`, `LocalDate`, `Double`). Si necesitas un `ResultSet` o un objeto JSON, eso pertenece a infraestructura.

### Señales de contaminación y cómo arreglarlas:
- *Señal:* Hay un `java.sql.SQLException` propagado en las interfaces del dominio (ej. `throws SQLException`).
- *Solución:* Lo ideal en Clean Architecture estricta es crear excepciones propias del dominio (ej. `DomainException` o `RepositoryException`), atrapando la SQLException en el bloque de persistencia y lanzando la excepción pura hacia el Use Case. Sin embargo, por pragmatismo inicial puede tolerarse temporalmente si aislarla supone demasiado coste, aunque en un TFG se valora altamente la abstracción pura.

---

## 🧠 Entregable para el Tribunal (Defensa del TFG)

### 1. Plan paso a paso (Hoja de ruta)
- **Fase 1: Abstracción de Datos.** Renombramiento mental de DAOs a Repositories, creando las interfaces del core y consolidando implementaciones en el paquete de infraestructura.
- **Fase 2: Refactorización de Intermediarios.** Fragmentación sistemática de los grandes Services en Casos de Uso atómicos. Reemplazo escalonado en los controladores de la UI mediante el ServiceContainer.
- **Fase 3: Limpieza Final (Auditoría de Dominio).** Asegurar la total independencia tecnológica del dominio, eliminando clases y paquetes huérfanos (borrando el paquete `dao/` completamente).

### 2. Ejemplos Prácticos (El "Antes" y el "Después")
**Antes (Mega-Service acoplado a DAO directamente):**
```java
// SaleService.java
public void processSale(Cart cart) {
    SaleDAO dao = new SaleDAO();
    dao.insertSale(cart); // Direct dependency
    JavaMailSender.send(); // Technical logic mixed
}
```

**Después (Clean Architecture):**
```java
// ProcessSaleUseCase.java (Application Layer)
public class ProcessSaleUseCase {
    private ISaleRepository saleRepo; // Interface de abstracción
    private IEmailSender emailSender; // Interface de servicio externa

    public ProcessSaleUseCase(ISaleRepository saleRepo, IEmailSender emailSender) {
        this.saleRepo = saleRepo;
        this.emailSender = emailSender;
    }
    
    public void execute(Sale sale) {
        // Lógica de negocio orquestada
        saleRepo.save(sale); 
        emailSender.sendReceipt(sale);
    }
}
```

### 3. Diagrama (Simplificado) de Dependencias
```text
[ Presentation Layer ] (JavaFX Controllers, Views)
       |
       | llama a
       v
[ Application Layer ] (Use Cases: ProcessSaleUseCase)
       |
       | depende de (interfaces)
       v
[ Domain Layer ] (Entities: Sale, Product | Interfaces: ISaleRepository)
       ^
       | implementa interfaces del Domain
       |
[ Infrastructure Layer ] (JdbcSaleRepository, DAOs unificados, SmtpEmailAdapter)
```
*(Nota importante: Las flechas siempre apuntan hacia adentro, hacia el Dominio).*

### 4. Posibles errores comunes (y cómo evaluarlos académicamente)
- **Anemic Domain Model:** Entidades que son solo Getters/Setters sin lógica. Justificación: En un CRUD inicial es pasable, pero la lógica de negocio real (ej. validaciones, cálculo de impuestos) debería residir en las propias entidades.
- **Fuga de Abstracción:** Exponer objetos JDBC (como `Connection` o `ResultSet`) a los Controladores o Use Cases.
- **"God Use Cases":** Mover un `SaleService` completo tal cual a `SaleUseCase` sin dividir responsabilidades, perdiendo la filosofía de SRP (Single Responsibility Principle).

### 5. Argumentario Técnico (Para la Defensa)

**Defensa del enfoque ante el tribunal:**
> *"La evolución de este proyecto partió de una arquitectura rápida, funcional pero fuertemente acoplada, típica de un diseño inicial MVC/Multicapa monolítico. La decisión técnica de refactorizar hacia Arquitectura Limpia (Clean Architecture) se fundamenta en los principios de Diseño de Software Sostenible y abstracción tecnológica.*
>
> *Al establecer el Principio de Inversión de Dependencias (Dependency Inversion), el corazón de la aplicación (el dominio y los Use Cases) queda completamente agnóstico. El dominio no sabe, ni le importa, si operamos sobre JDBC, PostgreSQL, un framework ORM complejo, o si la interfaz es JavaFX o web. Esta decisión técnica no solo incrementa drásticamente la testabilidad del sistema mediante mocks de las interfaces, sino que reduce el impacto cognitivo y de regresión al añadir nuevas funcionalidades.*
>
> *Mantuvimos JDBC nativo en la capa de persistencia en lugar de optar por un ORM completo pesados (como Hibernate). Esta fue una decisión arquitectónica consciente y pragmática para garantizar un control absoluto sobre el rendimiento y mantener un footprint de memoria mínimo, especialmente crítico en entornos de terminales punto de venta (TPVs) con hardware limitado. Este enfoque demuestra la robustez del Patrón Repository: una barrera protectora donde la 'falta' de un ORM mágico no ensucia nuestro dominio, probando empíricamente que Clean Architecture nos otorga el control, y no al revés."*
