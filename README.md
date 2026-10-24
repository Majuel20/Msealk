# MSealk · by Majuel20

Creé MSealk para consultar los cortes programados de energía de SEAL Arequipa, guardar zonas de interés y recibir recordatorios.

![Logo de MSealk](Android/Msealk/app/src/main/res/drawable-nodpi/logo_msealk.png)

## Descargar la app

El APK está disponible en el [Release v3.0.0](https://github.com/Majuel20/Msealk/releases/tag/v3.0.0).

[Descargar MSealk v3.0.0 para Android](https://github.com/Majuel20/Msealk/releases/download/v3.0.0/MSealk-v3.0.0.apk)

Requiere Android 10 o superior.

## Editar la app

Para editar la aplicación basta con descargar y descomprimir este repositorio, abrir Android Studio y seleccionar **Open → `Android/Msealk/`**. Después, espera la sincronización de Gradle.

En el [README del proyecto Android](Android/Msealk/README.md) explico cómo ejecutarlo y compilarlo. El proyecto Android es independiente del actualizador de datos.

## Organización del repositorio

| Carpeta | Contenido |
| --- | --- |
| [Android/Msealk/](Android/Msealk/) | Código fuente y recursos de la aplicación Android. |
| [actualizador/](actualizador/README.md) | Extracción y validación de los avisos de SEAL. |
| [datos/](datos/README.md) | Resultados publicados en JSON, CSV, TXT y Excel. |
| [.github/workflows/](.github/workflows/README.md) | Documentación de la actualización automática con GitHub Actions. |

La app consulta el [JSON publicado](https://raw.githubusercontent.com/Majuel20/Msealk/main/datos/cortes_programados.json). Los avisos describen horarios programados; no representan el estado del suministro en tiempo real.

## Autor y condiciones de uso

Desarrollo este proyecto como **Majuel20**. Las condiciones de uso están en [LICENSE](LICENSE) y los créditos en [NOTICE.md](NOTICE.md).
