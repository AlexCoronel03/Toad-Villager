# Toad Villagers (Fabric, Minecraft 1.21.1)

Un mod de Fabric que añade a **Toad** como un aldeano completo: modelo 3D propio, texturas propias, seis colores de
sombrero, sonidos propios y comportamiento de aldeano de vanilla (oficios, comercio, camas, pánico, aldeas).

> Proyecto de aficionado, no oficial. El personaje Toad pertenece a Nintendo; este mod no está afiliado ni
> respaldado por Nintendo ni por Mojang.

## Características

- **Entidad propia** `toadmod:toad`, subclase de `Villager`: hereda el cerebro, el horario, las camas, los puntos de
  interés, el pánico, el chisme, la cría y el comercio de vanilla.
- **Oficios como un aldeano normal.** Nace sin empleo, y si hay una estación de trabajo libre cerca (composter,
  atril, mesa de herrería...) toma el oficio vanilla y sus intercambios. Pierde el oficio si le quitas la estación
  antes de haber comerciado, igual que un aldeano.
- **Seis colores** del parche del sombrero (rojo, verde, magenta, azul, lima, amarillo), elegidos al azar con la misma
  probabilidad al aparecer. Se guardan en NBT (`CapColor`) y se sincronizan con los clientes.
- **Modelo 3D** en formato Blockbench/Bedrock: [`toad.geo.json`](src/main/resources/assets/toadmod/models/entity/toad.geo.json).
  El juego carga ese mismo archivo, así que lo que editas en Blockbench es lo que se renderiza.
- **Animaciones** procedurales: reposo, caminar, huir, comerciar, daño y muerte.
- **Sonidos propios**: caminar, daño, muerte, aceptar/rechazar trato, y "sin trabajo" al interactuar sin oficio.
- **Aparición**: 1-3 toads al generarse por primera vez una aldea, más una aparición natural ocasional dentro de
  aldeas (máximo 3 cerca). Huevo de aparición y `/summon toadmod:toad`.
- Los aldeanos de vanilla no se tocan.

## Instalación

Requisitos: Minecraft Java **1.21.1**, [Fabric Loader](https://fabricmc.net/use/) ≥ 0.16 y
[Fabric API](https://modrinth.com/mod/fabric-api).

1. Copia `toadmod-<versión>.jar` y Fabric API a la carpeta `mods`.
2. **Clientes:** el mod es necesario en cada cliente (modelo, texturas y sonidos).
3. **Servidor dedicado:** copia el mismo `.jar` y Fabric API a `mods` del servidor. No carga clases de cliente.

## Compilar

Requiere JDK 21.

```bash
./gradlew build
```

El JAR queda en `build/libs/toadmod-<versión>.jar`.

## Editar el modelo y las texturas

- Modelo: abre `src/main/resources/assets/toadmod/models/entity/toad.geo.json` en Blockbench (Bedrock Entity).
- Textura base: `tools/source/toad_base_red.png` (128×128, variante roja). Las otras cinco se generan recoloreando
  solo los píxeles rojos del parche:

```bash
python tools/generate_textures.py   # requiere Pillow
```

- Sonidos: los originales están en `sound-sources/`; el juego usa los `.ogg` mono de
  `assets/toadmod/sounds/entity/toad/`.

## Verificación visual (solo desarrollo)

`ToadPreview` es un arnés opcional que no hace nada salvo que se arranque el cliente con `-PtoadPreview`:

```bash
./gradlew runClient -PtoadPreview          # capturas, comercio, animaciones
./gradlew runClient -PtoadPreview=village  # aparición en una aldea real
./gradlew runClient -PtoadPreview=job      # toma y pérdida de oficio
```

## Estado y limitaciones

Probado en el cliente de desarrollo (1.21.1): renderizado de los seis colores, intercambio completo, persistencia
del color y de las ofertas por NBT, caminar/huir con IA real, aparición en una aldea nueva, toma y pérdida de oficio
y los sonidos.

**No probado todavía:** servidor dedicado, multijugador real.

Limitaciones conocidas:

- Los toads solo aparecen en aldeas **generadas después** de instalar el mod; en las existentes usa el huevo.
- Un aldeano zombi curado vuelve a ser un aldeano vanilla, no un Toad.
- Las animaciones son procedurales (código), no archivos de animación.
- No hay sonido de ambiente propio: el Toad está en silencio en reposo.
- No hay captura de pantalla de la interfaz de comercio en este repositorio.
