# Actualizador de cortes SEAL

`extraer_cortes.py` consulta el calendario y los detalles de los avisos de SEAL usando requests y Chromium mediante Playwright. `publicar_datos.py` ejecuta la extracción en una carpeta temporal, valida el resultado y prepara los cuatro archivos de `datos/`. Es código de producción, necesario para no sustituir datos válidos por una extracción fallida.

## Ejecución manual

Desde la raíz del repositorio, con Python 3.11:

```sh
python -m pip install -r actualizador/requirements.txt
python -m playwright install --with-deps chromium
python actualizador/publicar_datos.py
```

El rango es desde ayer hasta 30 días adelante. Las rutas se calculan desde el archivo Python, no dependen del usuario ni del sistema operativo. GitHub Actions ejecuta el mismo publicador y guarda los resultados en la rama de ejecución.

Si SEAL devuelve errores, bloquea el acceso o no se detectan avisos, se conservan los archivos publicados y se imprime el diagnóstico. `--permitir-vacio` solo permite una lista vacía tras detectar y extraer avisos válidos fuera del rango; no omite los controles de acceso a la fuente.
