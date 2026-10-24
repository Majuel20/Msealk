# MSealk · by Majuel20

Creé MSealk para consultar los cortes programados de energía de SEAL y tener a mano los avisos que coinciden con mis zonas. En esta carpeta comparto el proyecto Android de la versión **3.0.0**, con el código y los recursos necesarios para abrirlo en Android Studio.

![Logo de MSealk](app/src/main/res/drawable-nodpi/logo_msealk.png)

[Mi perfil de GitHub](https://github.com/Majuel20) · [Repositorio](https://github.com/Majuel20/MSealk) · [Descargas](https://github.com/Majuel20/MSealk/releases)

## Cómo abro el proyecto

1. En Android Studio selecciono **Open** y abro esta carpeta, la que contiene `settings.gradle.kts`, `gradlew` y `app/`.
2. Uso el JDK integrado de Android Studio y dejo que Gradle sincronice las dependencias. El proyecto utiliza Android SDK 36; si falta, lo instalo desde el SDK Manager.
3. Conecto un teléfono o inicio un emulador con **Android 10 o superior**.
4. Selecciono el módulo `app` y pulso **Run**.

Incluí el wrapper de Gradle, así que no necesito instalar Gradle por separado. La primera sincronización requiere Internet para descargar las dependencias.

En mi computadora, `local.properties` indica dónde está instalado el SDK. Si llevo el proyecto a otro equipo, dejo que Android Studio genere ese archivo con la ruta correcta. No lo subo a GitHub. Las carpetas `.idea`, `.gradle`, `.kotlin` y `build` se generan al trabajar con el proyecto.

## Cómo uso la aplicación

### Inicio

Desde Inicio actualizo los datos y consulto el estado de mis zonas. Dejé un resumen compacto con las zonas guardadas y las coincidencias; al tocarlo abro Diagnóstico. Los filtros permiten ver mis coincidencias, las de hoy, las de los próximos siete días o todos los cortes descargados.

El estado se basa en los horarios publicados. No representa una medición en tiempo real del suministro eléctrico.

### Zonas y cortes

Guardo un nombre, un color y palabras clave para cada zona. Puedo buscar por distrito, urbanización, calle o subestación, y elegir si debe coincidir una palabra o todas. El color identifica tanto la zona como sus tarjetas de cortes coincidentes.

En cada corte muestro primero el circuito, después la descripción, las zonas afectadas y las subestaciones. Reduzco las repeticiones en la presentación y resumo los campos largos en tres líneas, con un **Ver más** independiente para cada uno. El contenido original permanece en los datos.

**Ver detalles** abre el contenido y **Ocultar detalles** aparece al final, justo antes de Calendario y Ver fuente. También puedo copiar el aviso, compartirlo o llevarlo al calendario.

### Alertas y navegación

La campana abre Alertas y se resalta mientras esa sección está visible. Si la pulso otra vez, regreso a la sección donde estaba. Desde Alertas reviso los permisos y puedo enviar una notificación de prueba. Al tocar el logo superior abro mi perfil de GitHub.

### Ajustes

Cuando cambio una opción aparece **Guardar cambios** encima de la barra inferior. Después de guardar desaparece. Para elegir una hora uso las ruedas de horas y minutos, en formato de 24 horas; con Cancelar conservo el valor anterior.

Puedo activar la actualización automática, elegir un intervalo de uno a siete días o revisar solo los lunes. Muestro la última descarga correcta y la próxima revisión estimada con día, fecha y hora. La próxima fecha procede del trabajo programado por Android; una actualización manual no reinicia un calendario automático que ya está activo.

La revisión predeterminada es a las 06:30. El workflow está programado desde las 05:17 de Perú, aunque GitHub puede retrasarlo; la app consulta el último archivo disponible y no espera una señal de finalización del workflow.

## Cómo funcionan los recordatorios

Además de los avisos de cortes nuevos o modificados, dejé dos recordatorios configurables:

- **Víspera:** aviso a la hora elegida del día anterior. Para una configuración nueva propongo las 20:00; conservo las horas que ya estuvieran guardadas.
- **Antes del corte:** uso la hora preferida del mismo día si es anterior al inicio. Si coincide con el inicio o es posterior, calculo el aviso 90 minutos antes de ese corte. En un corte de madrugada, el recordatorio puede pasar a la noche anterior.

Por ejemplo, con la hora preferida a las 06:00, un corte a las 10:00 se recuerda a las 06:00. Si comienza a las 05:00, el recordatorio se calcula para las 03:30. Hago ese cálculo por separado para cada coincidencia.

Si los datos llegan después de la hora prevista, recupero el recordatorio solo dentro de su fecha aplicable y cuando faltan al menos 15 minutos para el corte. Antes de entregar un recordatorio programado compruebo que la coincidencia siga vigente, que no haya cambiado y que el corte no haya empezado. El aviso de la víspera caduca al terminar ese día.

Las horas usan la zona horaria del teléfono. Android puede retrasar las tareas por ahorro de batería o conexión, por lo que no prometo una entrega exacta al minuto. Para recibir avisos necesito activar las alertas de la app y conceder el permiso de notificaciones.

## De dónde vienen los datos

La aplicación descarga el [JSON de cortes programados](https://raw.githubusercontent.com/Majuel20/MSealk/main/datos/cortes_programados.json) del repositorio MSealk. El extractor y el workflow se mantienen en GitHub; no forman parte de esta carpeta Android ni hacen falta para compilarla.

Defino esa dirección en `app/src/main/java/com/msealk/data/AppSealRepository.kt`. Cuando no puedo descargar datos, conservo la copia local y muestro el error para poder revisarlo.

## Cómo está organizada esta carpeta

| Ruta | Contenido |
| --- | --- |
| `app/src/main/java/com/msealk/` | Pantallas, datos, programación y notificaciones |
| `app/src/main/res/` | Logo definitivo, iconos, textos y temas |
| `app/src/main/AndroidManifest.xml` | Configuración y permisos de Android |
| `app/build.gradle.kts` | Versión, SDK y dependencias de la aplicación |
| `gradle/` y `gradlew*` | Catálogo de versiones y wrapper de Gradle |
| `settings.gradle.kts` | Módulos y repositorios de dependencias |
| `LICENSE` y `NOTICE.md` | Condiciones de uso y créditos |

Mantengo el paquete `com.msealk` y el logo definitivo `app/src/main/res/drawable-nodpi/logo_msealk.png`. No incluyo alternativas de logos, claves de firma ni APK de prueba como parte del código fuente.

## Cómo preparo una versión para distribuir

Desde Android Studio uso **Build → Generate Signed App Bundle / APK** y selecciono mi clave de firma. Para actualizar una instalación existente necesito mantener el identificador de la aplicación y la misma clave; al publicar una nueva versión incremento `versionCode` en `app/build.gradle.kts` y actualizo los números de versión correspondientes.

Conservo las claves fuera del repositorio. Si comparto esta carpeta, excluyo `local.properties`, las cachés y los resultados de compilación mediante `.gitignore`.

## Créditos y condiciones de uso

Desarrollo MSealk como **Majuel20**. Utilizo Kotlin, Jetpack Compose, Material 3, AndroidX y WorkManager. Los avisos proceden de SEAL y puedo consultar su publicación original desde cada corte.

Las condiciones del proyecto están en [LICENSE](LICENSE). Las bibliotecas y herramientas externas conservan sus propias licencias y créditos, descritos de forma general en [NOTICE.md](NOTICE.md).
