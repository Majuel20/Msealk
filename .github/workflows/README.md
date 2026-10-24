# Actualización automática

Uso [actualizar_cortes.yml](actualizar_cortes.yml) para ejecutar el [actualizador](../../actualizador/README.md) y guardar los resultados validados en [datos/](../../datos/README.md). Este workflow no compila la aplicación Android.

## Cuándo se ejecuta

La programación diaria es a las **05:17 de Perú (10:17 UTC)**. GitHub puede retrasar el inicio.

Para iniciarlo manualmente, abro [Actualizar cortes programados en Actions](https://github.com/Majuel20/Msealk/actions/workflows/actualizar_cortes.yml), pulso **Run workflow** y selecciono la rama `main`.

Dejo `permitir_vacio` desmarcado salvo que haya comprobado que corresponde publicar cero cortes en el rango. Esta opción no evita las validaciones de la extracción.

## Qué publica

El workflow instala Python 3.11 y las dependencias, ejecuta el publicador y guarda los cuatro archivos `cortes_programados.*` en la rama de ejecución cuando existen cambios.

Declara el permiso `contents: write` para guardar los resultados con el token de GitHub Actions. Si la extracción o la validación falla, no publica los resultados y el registro de la ejecución muestra el diagnóstico.

[Volver al README principal](../../README.md)
