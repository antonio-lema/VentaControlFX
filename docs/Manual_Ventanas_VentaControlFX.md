# Manual Detallado por Pantallas: VentaControlFX

Este documento desglosa paso a paso las funcionalidades que el usuario encontrará en cada una de las ventanas e interfaces de la aplicación VentaControlFX.

---

## 1. Inicio y Autenticación (Login)
**Ventana principal de acceso al sistema.**
*   **Qué hace el usuario:** Introduce sus credenciales (usuario y contraseña) para iniciar sesión.
*   **Qué hace el sistema:** Verifica las credenciales, identifica el rol del usuario (SuperAdmin, Encargado, Vendedor) y registra la hora de inicio de su turno (fichaje). Si las credenciales son correctas, desbloquea los módulos permitidos para ese usuario.

---

## 2. Panel Principal (Dashboard)
**El centro de control "home" de la aplicación.**
*   **Qué hace el usuario:** Visualiza de un vistazo un resumen del estado del negocio. Puede navegar al resto de los módulos mediante el menú lateral (Sidebar).
*   **Qué hace el sistema:** Muestra métricas clave en tiempo real, como el número de ventas del día, el total facturado, avisos de stock bajo y gráficos circulares/barras con tendencias de ventas.

---

## 3. Módulo de Venta (TPV / Cart)
**La pantalla con más uso del sistema, donde se procesan las ventas.**
*   **Zona de Productos (Grid):** El usuario busca productos por nombre o navega por categorías (mediante botones "chips"). Al hacer clic en un producto, se añade al carrito.
*   **Panel del Carrito (Cart Panel):** El usuario ve la lista de artículos a comprar. Puede modificar cantidades, eliminar líneas o aplicar descuentos a un producto específico.
*   **Suspensión de Ventas:** El usuario puede aparcar (suspender) un carrito temporalmente para atender a otro cliente y recuperarlo más tarde mediante la ventana de "Carritos Suspendidos".
*   **Ventana de Pago (Payment Dialog):** Al pulsar "Cobrar", se abre esta ventana. El usuario selecciona el método de pago (Efectivo, Tarjeta), introduce el importe entregado por el cliente, y el sistema calcula el cambio. Finaliza la venta, generando el ticket y abriendo opcionalmente el cajón portamonedas.
*   **Ventana de Clientes:** Permite asociar la venta actual a un cliente específico registrado en la base de datos (útil para informes de fidelización o facturas a nombre).

---

## 4. Gestión de Productos e Inventario
**Módulo para administrar el catálogo.**
*   **Ventana de Productos (Listado):** Muestra una tabla con todos los artículos. El usuario puede filtrar por stock, buscar por código/nombre, o ver el estado general del inventario.
*   **Añadir/Editar Producto:** Se abre un formulario completo donde el usuario define el código de barras, nombre, precio base, coste, IVA aplicado, categoría y cantidad de stock. 
*   **Gestión de Categorías:** Pantalla sencilla para crear, editar o eliminar los "departamentos" o familias que agrupan los productos en el TPV.

---

## 5. Tarifas y Promociones
**Control avanzado de la política de precios.**
*   **Gestión de Tarifas (Price Lists):** Muestra "tarjetas" con las diferentes listas de precios (General, Mayorista, etc.). El usuario puede establecer cuál es la tarifa por defecto.
*   **Visor de Precios de Tarifa:** Al abrir una tarifa, se ve la tabla de productos con sus precios específicos para esa tarifa.
*   **Clonador de Tarifas (Clone Dialog):** Ventana especializada donde el usuario selecciona una tarifa origen, introduce un nombre nuevo y un porcentaje (ej. +10%). El sistema genera una nueva tarifa recalculando todos los precios automáticamente.
*   **Promociones:** Pantalla para programar ofertas temporales (ej. 2x1, o 20% de descuento en una fecha concreta).

---

## 6. Gestión de Caja y Cierres (Closure)
**Control financiero del flujo de efectivo diario.**
*   **Apertura de Caja:** Al inicio de jornada (o tras un cierre), se pide al usuario confirmar el importe de "cambio" inicial (Fondo de caja).
*   **Ingresos y Retiradas (Cash Entry/Withdraw):** Ventanas emergentes (popups) donde el empleado registra si entra dinero (ej. cambio) o sale dinero (ej. pago a un proveedor en efectivo) del cajón, indicando un motivo.
*   **Cierre de Caja (Cash Closure):** Al final del día, el usuario cuenta las monedas y billetes reales y los introduce. El sistema compara el valor real con el esperado (Ventas + Entradas - Salidas) y muestra si hay Descuadre (sobrante o faltante). Registra el cierre en la base de datos.
*   **Historial de Cierres (Closure History):** Pantalla donde un administrador puede revisar y auditar todos los cierres de días o meses anteriores.

---

## 7. Informes y Auditoría (Reports)
**Ventanas analíticas y estadísticas.**
*   **Informe de Ventas:** Buscador avanzado de operaciones. Permite ver todos los tickets emitidos, filtrando por fecha, cajero, estado. También incluye una ventana de "Devolución" (Return Dialog) para anular líneas de un ticket pasado.
*   **Reporte por Cliente (Client Report):** Permite ver cuánto ha comprado un cliente específico a lo largo del tiempo, sus productos favoritos y generar un PDF con su historial.
*   **Reporte de Empleados (Seller Report / Punctuality Audit):** Pantalla para que el administrador revise la productividad de cada vendedor (total facturado) y la auditoría de puntualidad (fichajes de entrada y salida de turnos).

---

## 8. Usuarios y Personal (User Management)
**Administración de recursos humanos.**
*   **Gestión de Usuarios:** Tabla con todos los empleados. Permite dar de alta a un nuevo trabajador, bloquearle el acceso o reiniciar su contraseña.
*   **Gestión de Roles:** Define qué puede hacer cada perfil. Por ejemplo, el SuperAdmin marca casillas para decidir si el rol "Vendedor" puede o no hacer devoluciones o abrir el cajón sin venta.
*   **Calendario y Turnos (Shift / Special Days):** Pantallas para definir el horario comercial, configurar qué días son festivos (para que los informes no cuenten esos días como laborables) y gestionar la planificación del personal.

---

## 9. Configuración y Estética (Customization)
**Ajustes técnicos y visuales de VentaControlFX.**
*   **Configuración de Venta (Sale Config):** Ventana donde se ajustan parámetros legales y técnicos, como el tipo de moneda (€, $, etc.), cómo se calculan los impuestos (IVA incluido o no) y configuración de las impresoras de red/locales.
*   **Panel de Estética (Customization Panel):** Una ventana interactiva donde el usuario altera el diseño visual del TPV. 
    * Puede cambiar la paleta de colores corporativa mediante un selector de colores (ej. pasar el botón primario de azul a rojo).
    * Puede alternar entre Modo Claro y Modo Oscuro con un solo clic.
    * Los cambios se aplican en vivo sin necesidad de reiniciar la aplicación, afectando a la barra superior, menú lateral y botones principales.
