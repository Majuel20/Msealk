# MSealk 3.0.0 — by Majuel20

Proyecto fuente independiente para Android Studio. Android 10 o superior; paquete `com.msealk`.

## Abrir y compilar

1. Descarga y descomprime el repositorio.
2. Copia esta carpeta si deseas trabajar fuera del repositorio; puedes renombrarla `MSealk`.
3. En Android Studio selecciona **Open** y abre esta carpeta, donde están `settings.gradle.kts`, `gradlew` y `app/`.
4. Usa el JDK integrado de Android Studio e instala Android SDK 36 cuando se solicite. Espera la sincronización de Gradle.
5. Ejecuta la aplicación en tu dispositivo o usa **Build → Generate Signed App Bundle / APK** para generar una versión firmada con tu propia clave.

El wrapper de Gradle está incluido. Android Studio genera `local.properties` con el SDK de cada equipo; no se distribuyen rutas del equipo del autor, claves ni resultados de compilación.

## Fuente de datos

`app/src/main/java/com/msealk/data/AppSealRepository.kt` define la URL:
https://raw.githubusercontent.com/Majuel20/MSealk/main/datos/cortes_programados.json

La app necesita Internet para actualizar los cortes. No necesita las carpetas `actualizador` ni `datos` junto al proyecto Android. Conserva una copia local de los datos descargados.

## Identidad

Logo definitivo: `app/src/main/res/drawable-nodpi/logo_msealk.png`. El alias `app_logo` se utiliza en la cabecera, inicio e icono del lanzador. Incluye el crédito **by Majuel20** y compartir el enlace de GitHub.
