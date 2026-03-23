# 📜 Reglas de Comportamiento: Antigravity Framework

Este archivo contiene las instrucciones maestras para Antigravity en este proyecto. Deben seguirse en cada interacción.

## 1. Uso de Skills y MCP
- **Referencia Obligatoria:** Consultar siempre `.antigravity/skills/definitions.json` antes de procesar acciones de negocio.
- **Formato de Salida:** Priorizar el uso de JSON para las skills (name, payload, result) cuando se trate de operaciones de base de datos o lógica de carrito.
- **Ahorro de Tokens:** Mantener los payloads compactos y evitar explicaciones redundantes si la skill ya lo describe.

## 2. Estética y Diseño (WOW Effect)
- **Paleta de Colores:** Usar tonos modernos, gradientes suaves y dark mode por defecto si se genera UI.
- **Tipografía:** Preferir fuentes de Google Fonts (Inter, Roboto, Outfit).
- **Componentes:** Los componentes deben sentirse premium, con bordes redondeados (border-radius: 12px+) y sombras sutiles.
- **Micro-animaciones:** Incluir transiciones CSS para hovers y cambios de estado.

## 3. Contexto del Negocio
- **Proyecto:** Bazar Tecnológico (TPV Bazar).
- **Tono:** Profesional, técnico y proactivo.
- **Datos Reales:** No inventar productos. Si no se conoce un dato, realizar una búsqueda en la DB o archivos de volcado previos.

## 4. Estructura de Proyecto
- Mantener la arquitectura **Clean Architecture** (domain, application, infrastructure, presentation).
- No modificar archivos CSS globales sin verificar colisiones de selectores.

---
*Si estas reglas entran en conflicto con una petición directa del usuario, prevalecerá la petición del usuario, pero se debe informar si rompe la consistencia del framework.*
