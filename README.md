# Cami LookUp

Mod para **Minecraft Forge 1.21.11** que permite inspeccionar bloques y entidades con un cursor libre.
El alcance acordado está en [`docs/ALCANCE.md`](docs/ALCANCE.md).

## Uso

- Mantén **ALT** (reasignable en *Controles*) para liberar el cursor; la cámara queda fija pero puedes moverte.
- Pasa el cursor sobre un bloque o entidad para ver el **resumen** (los bloques se resaltan con un contorno y las
  entidades con el efecto de brillo vanilla);
  mantén **SHIFT** para el **detalle completo**.
- **Click izquierdo** sobre el objetivo lo **mantiene abierto**: el recuadro flota junto al bloque o sobre la entidad
  y se achica con la distancia. Otro click sobre el recuadro o sobre el objetivo lo cierra.
- El botón **📌** del recuadro abierto (o **CTRL + click** sobre el objetivo) lo **fija** como una ventana en la
  pantalla que se arrastra por su cabecera y se cierra con **✕**. Solo si el servidor tiene el mod.
- **Rueda del mouse** sobre un recuadro desplaza su contenido; **click derecho** ejecuta la acción de un addon.

## Compilación

Requiere los JDK **21** (compilación), **25** y **8** (herramientas de ForgeGradle 7) instalados localmente, y acceso de
red a `maven.minecraftforge.net`, `*.mojang.com`, `libraries.minecraft.net` y `resources.download.minecraft.net`.

ForgeGradle intenta descargar los JDK 25 y 8 por su cuenta. Si esa descarga falla (por ejemplo, con
`sha256 Invalid` al bajar el JDK 8), instala el JDK a mano y apunta la variable de entorno `JAVA_HOME_8_X64` (o
`JAVA_HOME_25_X64`) a su carpeta; después ejecuta `gradlew --stop` para que Gradle tome la variable.

```sh
./gradlew build            # genera build/libs/camilookup-<versión>.jar
./gradlew runClient        # cliente de desarrollo
./gradlew runServer        # servidor de desarrollo (carpeta run-server/)
./gradlew runClient -PquickPlay=localhost   # entra directo a un servidor
```

## Configuración

- Cliente (`config/camilookup-client.toml`): modo de activación (mantener/alternar), distancia de inspección
  e información técnica.
- Servidor (`serverconfig/camilookup-server.toml`): distancia máxima, categorías desactivadas,
  detalles de otros jugadores y límite de solicitudes por segundo.

## API para addons

Todos los derechos reservados; se permite explícitamente usar la API y crear addons. La información vanilla del mod
usa esta misma API (`io.github.marcofallasu.camilookup.api`).

```java
public MyAddon(FMLJavaModLoadingContext context) {
    RegisterLookUpEvent.BUS.addListener(event -> {
        LookUpRegistrar registrar = event.registrar();

        // Se ejecuta en el servidor; el resultado se envía al cliente.
        registrar.registerBlockProvider(ProviderSide.SERVER, id("energy"), id("energy"),
                (accessor, builder) -> {
                    if (accessor.blockEntity() instanceof MyMachine machine) {
                        builder.text(Component.literal("Energía: " + machine.energy()));
                    }
                });

        // Click derecho sobre un recuadro fijado.
        if (registrar.dist().isClient()) {
            registrar.registerPinAction(id("open_screen"), 0, new MyPinAction());
        }

        // Reglas del servidor: ocultar información a ciertos jugadores.
        registrar.registerRestrictionRule(id("no_spectators"), new IRestrictionRule() {
            @Override
            public boolean canInspect(ServerPlayer player, LookUpAccessor target) {
                return !player.isSpectator();
            }
        });
    });
}
```

- **Proveedores** (`IBlockInfoProvider`, `IEntityInfoProvider`): en el cliente (`ProviderSide.CLIENT`, siempre
  disponibles, incluso si el servidor no tiene el mod) o en el servidor (`ProviderSide.SERVER`).
- **Elementos** (`InfoBuilder`): texto, texto con icono, cuadrícula de items, fila de items y vida. Cada elemento
  puede mostrarse siempre, solo en el resumen o solo en el detalle (`Visibility`).
- **Categorías**: cada proveedor declara una; el servidor puede desactivarlas en su configuración.
