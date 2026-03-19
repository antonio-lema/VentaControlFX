# 🚀 Guía de Uso: Skills y MCP Framework

Este framework permite interactuar con el sistema de ventas de forma estructurada y eficiente.

## Cómo Invocar una Skill
Simplemente menciona la acción que deseas realizar. Antigravity interpretará el JSON por ti.

**Ejemplo:**
> "Busca el producto 'coca cola' usando la skill `search_product`"

**Respuesta de Antigravity (interna):**
```json
{
  "skill": "search_product",
  "payload": { "query": "coca cola", "limit": 5 }
}
```

## Recursos MCP (Estado Compartido)
Estos recursos guardan información persistente durante tu sesión. Puedes pedir consultarlos o actualizarlos.

- **`current_cart`**: Lo que hay en el carrito ahora mismo.
- **`current_client`**: Quién es el comprador actual.

## Ventajas
1. **Ahorro de Tokens:** No repetimos descripciones largas de funciones.
2. **Precisión:** Las entradas y salidas están tipadas.
3. **Persistencia:** Los recursos MCP mantienen el contexto sin que tengas que recordarlo en cada prompt.

---
*Ubicación de definiciones:* [definitions.json](file:///c:/Users/practicassoftware1/Documents/NetBeansProjects/VentaControlFX/.antigravity/skills/definitions.json)
