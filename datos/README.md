# Datos publicados

En esta carpeta publico los resultados validados del actualizador:

| Archivo | Uso |
| --- | --- |
| [cortes_programados.json](cortes_programados.json) | Datos que consulta la aplicación. |
| [cortes_programados.csv](cortes_programados.csv) | Tabla para importar o analizar. |
| [cortes_programados.txt](cortes_programados.txt) | Avisos en texto. |
| [cortes_programados.xlsx](cortes_programados.xlsx) | Tabla en formato Excel. |

El JSON contiene `metadata`, `total` y `cortes`. La fecha de extracción figura en `metadata.fecha_ejecucion`.

Cada ejecución correcta del [actualizador](../actualizador/README.md) reemplaza estos archivos. Si la extracción falla, se conservan los anteriores: su presencia no significa que se hayan actualizado hoy.

La app utiliza el [enlace directo al JSON](https://raw.githubusercontent.com/Majuel20/Msealk/main/datos/cortes_programados.json).

[Volver al README principal](../README.md)
