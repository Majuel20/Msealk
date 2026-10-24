# Actualizador de cortes

En esta carpeta mantengo los scripts que consultan los avisos de SEAL y preparan los archivos publicados en [datos/](../datos/README.md).

- [extraer_cortes.py](extraer_cortes.py): consulta el calendario y los detalles de los avisos mediante requests y Playwright.
- [publicar_datos.py](publicar_datos.py): ejecuta la extracción en una carpeta temporal y valida los resultados antes de reemplazar los archivos publicados.
- [requirements.txt](requirements.txt): dependencias de Python.

El rango de consulta comprende desde ayer hasta 30 días adelante. Si la extracción no se valida, se conservan los datos anteriores.

## Ejecución local

Uso Python 3.11 y ejecuto estos comandos desde la raíz del repositorio:

```sh
python -m pip install -r actualizador/requirements.txt
python -m playwright install --with-deps chromium
python actualizador/publicar_datos.py
```

El último comando actualiza los archivos locales de `datos/`; no los sube a GitHub. La publicación automática está descrita en [GitHub Actions](../.github/workflows/README.md).

La opción `--permitir-vacio` permite publicar cero cortes en el rango únicamente si se detectaron y extrajeron avisos válidos. No omite la validación de la fuente.

[Volver al README principal](../README.md)
