# Estructura del Proyecto VentaControlFX

Tu proyecto sigue una arquitectura **MVC (Modelo-Vista-Controlador)** con un patrón adicional de **Servicios y DAOs** para separar limpiamente la lógica de negocio del acceso a datos.

## 1. Arquitectura General

```mermaid
graph TD
    User[Interacción Usuario] --> View[Vista (FXML)]
    View --> Controller[Controlador (Java)]
    Controller --> Service[Servicio (Lógica Negocio)]
    Service --> DAO[DAO (Acceso a Datos)]
    DAO --> DB[(Base de Datos SQLite)]
    
    Controller -.-> Model[Modelo (Datos)]
    Service -.-> Model
    DAO -.-> Model
```

---

## 2. Desglose de Paquetes (`src/main/java`)

### 📦 `com.mycompany.ventacontrolfx`
Raíz del código fuente. Contiene `App.java`, que es el punto de entrada principal (`main`) que lanza la aplicación JavaFX.

### 📦 `controller` (Controladores)
**Función:** Manejan la lógica de la interfaz de usuario. Conectan las vistas FXML con el código Java.
*   **Ejemplos:**
    *   `ProductController.java`: Controla la vista de lista de productos (filtros, tabla, botones).
    *   `CategoryController.java`: Lo mismo para categorías.
    *   `MainController.java`: Controla la pantalla principal de ventas (carrito, grid de productos).
    *   `AddProductController.java`: Lógica del diálogo para añadir/editar productos.

### 📦 `model` (Modelos)
**Función:** Representan los objetos de datos puros (POJOs) que usa tu aplicación. Son clases simples con atributos, getters y setters.
*   **Ejemplos:**
    *   `Product.java`: Representa un producto (id, nombre, precio, categoría, etc.).
    *   `Category.java`: Representa una categoría.
    *   `CartItem.java`: Representa un ítem en el carrito de compras (producto + cantidad).

### 📦 `dao` (Data Access Object)
**Función:** Se encargan EXCLUSIVAMENTE de hablar con la base de datos (SQL). Aquí van los `SELECT`, `INSERT`, `UPDATE`, `DELETE`.
*   **Ejemplos:**
    *   `ProductDAO.java`: Ejecuta SQL para guardar o traer productos.
    *   `CategoryDAO.java`: Ejecuta SQL para categorías.

### 📦 `service` (Servicios)
**Función:** Actúan como intermediarios entre el Controlador y el DAO. Aquí va la lógica de negocio (validaciones, cálculos). El controlador llama al Servicio, y el Servicio llama al DAO.
*   **Ejemplos:**
    *   `ProductService.java`: Si el controlador pide "borrar producto", el servicio podría verificar primero si se puede borrar antes de llamar al DAO.

### 📦 `config` (Configuración)
**Función:** Configuraciones globales, principalmente la conexión a la base de datos.
*   **Ejemplo:** `DatabaseConnection.java` (o similar) que gestiona la conexión SQLite.

### 📦 `control` (Controles UI Personalizados)
**Función:** Componentes visuales creados a medida que no existen por defecto en JavaFX.
*   **Ejemplo:** `ToggleSwitch.java` (el interruptor moderno que implementamos para Favoritos/Visibilidad).

---

## 3. Recursos (`src/main/resources`)

### 📂 `view` (Vistas)
Contiene los archivos que definen la **apariencia** de la aplicación.
*   **`.fxml`**: Archivos XML que describen la estructura visual (ventanas, botones, tablas).
    *   `products.fxml`, `categories.fxml`, `main.fxml`, `add_product.fxml`.
*   **`.css`**: Archivos de estilos (colores, fuentes, bordes).
    *   `style.css`: Define el aspecto visual (tema oscuro, botones naranjas, esquinas redondeadas).

---

## Flujo de Ejemplo: "Guardar un Producto"

1.  **Vista (`add_product.fxml`)**: El usuario rellena el formulario y pulsa "Guardar".
2.  **Controlador (`AddProductController`)**:
    *   Recoge los datos de los campos de texto.
    *   Crea un objeto `Product` (**Modelo**).
    *   Llama a `productService.saveProduct(producto)`.
3.  **Servicio (`ProductService`)**:
    *   Podría validar que el precio no sea negativo.
    *   Llama a `productDAO.insert(producto)`.
4.  **DAO (`ProductDAO`)**:
    *   Abre conexión a la BD.
    *   Ejecuta `INSERT INTO products ...`.
    *   Cierra conexión.
