# Cami LookUp — Alcance acordado

Mod para **Minecraft Forge 1.21.11** que permite inspeccionar bloques y entidades con un cursor libre.

## General
- Nombre visible: **Cami LookUp** · mod id: `camilookup` · paquete / group: `io.github.marcofallasu.camilookup`.
- Se instala en **cliente + servidor**. Si el servidor no tiene el mod, funciona en modo degradado (solo la información que el cliente conoce: nombre, estado del bloque, vida visible, ticks del repetidor, etc.).
- Idiomas: `es_es`, `es_mx`, `en_us`.
- Licencia: **todos los derechos reservados**, con permiso explícito para usar la API y crear addons.

## Modo cursor
- Tecla por defecto **ALT**, reasignable en Controles.
- Modo configurable: **mantener presionada** (por defecto) o **alternar (toggle)**.
- La cámara queda fija, pero el jugador **puede moverse** (WASD, saltar, etc.).
- No abre ninguna interfaz; solo libera el cursor sobre la pantalla.
- El modo cursor se cancela si la ventana pierde el foco (evita problemas con Alt+Tab).
- Distancia de inspección **configurable**, con un máximo impuesto por el servidor.

## Recuadro (tooltip)
- Estilo del **tooltip vanilla** de items y sigue al cursor.
- **Resumen** al pasar el cursor; **detalle completo** con SHIFT o en un recuadro fijado.
- Contenedores: **cuadrícula de iconos** con cantidades (compacta en el resumen, completa con slots vacíos en el detalle). En un recuadro fijado, pasar el cursor sobre un item muestra su tooltip normal.
- Vida: **corazones**, y número (`150/200 ❤`) si son demasiados.

## Recuadros abiertos y ventanas fijadas
- **Mantener abierto** (click izquierdo sobre el objetivo): el recuadro queda flotando junto al bloque o sobre la entidad, la sigue y se actualiza en vivo. Se achica con la distancia para no tapar la pantalla. Muestra el resumen (detalle con SHIFT al pasar el cursor encima). Otro click **sobre el recuadro o sobre el objetivo** lo cierra. Se oculta si el objetivo sale de la vista y se cierra si desaparece o queda fuera de alcance. Funciona aunque el servidor no tenga el mod.
- **Fijar** (botón 📌 del recuadro abierto, o CTRL + click sobre el objetivo): el recuadro pasa a ser una **ventana en la pantalla**, como una ventana de Windows: se arrastra por su cabecera, se cierra con ✕, muestra el detalle completo y se actualiza en vivo; si el objetivo deja de estar disponible conserva la última información con un aviso. **Solo disponible si el servidor tiene el mod.**
- **Sin límite** de recuadros abiertos ni de ventanas; siguen visibles al salir del modo cursor.
- **Click derecho**: acción registrada por un addon (por ejemplo, abrir la interfaz de otro mod). Sin addon, no hace nada.
- **Rueda del mouse**: desplaza el contenido de un recuadro abierto o de una ventana.

## Información vanilla
- **Básico**: nombre, icono, mod de origen y propiedades del estado del bloque.
- **Técnica** (activable en la configuración): dureza, herramienta necesaria y si puede romperse, resistencia a explosiones, nivel de luz, bioma y coordenadas.
- **Contenedores**: cofres, barriles, shulkers, tolvas, carros de mina con cofre, inventario de burros y llamas.
- **Redstone**: repetidor (ticks, bloqueado), comparador (modo y señal), fuerza de señal, observador, pistón, sensor de luz, tolva (activa o bloqueada).
- **Granja**: crecimiento de cultivos en % (sin caña ni cactus), panal o colmena (abejas y miel), compostador, retoños, huevos de tortuga o sniffer.
- **Utilidad**: atril (página actual), letrero (texto de ambos lados), tocadiscos (disco), bloque musical (nota e instrumento), estantería cincelada, faro (nivel y efectos), caldero (nivel y líquido).
- **Procesamiento**: horno, alto horno y ahumador (entrada, combustible, salida y progreso), soporte de pociones, fogata.
- **Avanzados**: crafter, trial spawner, vault, vasija decorada, ancla de reaparición.
- **Otros**: sensor sculk, marco del portal del End, arena o grava sospechosa (item oculto; **desactivado por defecto** en el servidor).
- Sin información especial: maceta, spawner, cama, cuadros.
- **Entidades**: vida, armadura, efectos, equipamiento, edad o cooldown de reproducción; mascotas y monturas (dueño, sentado, estadísticas de caballo, inventario, collar); aldeanos (profesión, nivel, intercambios, cama y lugar de trabajo); items en el suelo (cantidad y tiempo para desaparecer); marcos de items; soportes de armadura.
- **Jugadores**: por defecto solo lo visible (nombre, vida, armadura y equipamiento); el servidor decide si muestra más.

## Servidor
- Configuración del servidor: distancia máxima, categorías permitidas, etc. No se exige línea de visión: para el jugador técnico, una tolva o entidad detrás de bloques se sigue viendo (por ejemplo, en una ventana fijada) mientras esté dentro de la distancia máxima.
- Los addons pueden agregar sus propias reglas de restricción.

## API para addons (Java)
- Proveedores de información en el cliente y en el servidor, acciones de click derecho y reglas de restricción.
- Registro mediante interfaces públicas y un evento de Forge.
- La información vanilla del mod usa esta misma API.

## Configuración del cliente
- Pantalla propia en el juego (botón *Config* en la lista de mods) además del archivo `.toml`.

## Fases
1. **Fase 1**: modo cursor, raycast desde el cursor, tooltip, recuadros abiertos y ventanas fijadas, API, sincronización con el servidor, información básica, de contenedores y de entidades.
2. **Fase 2**: resto de bloques y entidades especiales, restricciones del servidor completas y pantalla de configuración.

## Notas de desarrollo
- Commits a nombre de Marco Fallas Umaña `<marco.fallas.umana@est.una.ac.cr>`, sin línea de co-autor.
- La compilación requiere acceso de red a `maven.minecraftforge.net`, `*.mojang.com`, `libraries.minecraft.net` y `resources.download.minecraft.net`.
