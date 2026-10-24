# MSealk

Aplicación Android para consultar cortes programados de energía de SEAL y actualización automática de datos. **by Majuel20**.

![MSealk](android/app/src/main/res/drawable-nodpi/logo_msealk.png)

## Contenido

- `android/`: proyecto completo para descargar, abrir y compilar en Android Studio. No se compila ni ejecuta en GitHub Actions.
- `actualizador/`: extractor de SEAL y publicación de resultados validados.
- `datos/`: archivos JSON, CSV, TXT y Excel consultables.
- `.github/workflows/actualizar_cortes.yml`: única automatización; actualiza los datos.

## Android

Descarga el repositorio y abre la carpeta `android` en Android Studio. También puedes copiar esa carpeta a otra ubicación y renombrarla `MSealk`: es independiente del resto del repositorio. Sigue su [README](android/README.md).

## Activar la actualización

Sube el contenido de este paquete a la raíz de `Majuel20/Msealk`, rama `main`, incluida `.github`. No subas la carpeta contenedora «Subir GITHUB».

En Actions selecciona **Actualizar cortes programados → Run workflow → main**. Deja `permitir_vacio` desmarcado salvo que hayas confirmado manualmente que no hay cortes en el rango. Se programa diariamente a las 05:17 de Perú; GitHub puede retrasar la ejecución.

El workflow declara `contents: write` para guardar los resultados. No necesita claves personales. Si SEAL no responde o la extracción no se puede validar, falla y conserva los datos anteriores. Un resultado vacío no demuestra que no existan cortes.

La aplicación consulta:
https://raw.githubusercontent.com/Majuel20/MSealk/main/datos/cortes_programados.json

Las rutas `/home/runner/work/...` y `/tmp/...` que aparecen en Actions pertenecen al equipo temporal de GitHub. El código calcula sus rutas desde la ubicación del proyecto, sin depender de esas carpetas.

## Sustituir una publicación anterior

Al actualizar un repositorio existente, elimina también los archivos antiguos que ya no estén en este paquete. Subir archivos nuevos no elimina los anteriores. En particular, elimina `Nueva carpeta/`, `documentacion/`, `.github/workflows/comprobar_android.yml`, `actualizador/test_publicar_datos.py`, los logos alternativos y la documentación antigua de Android.

No elimines el repositorio `App-seal-data` mientras existan versiones anteriores de la app que lo consulten.
