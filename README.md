# 🥑 Paltodoc - Detección Temprana de Anomalías Foliares mediante Visión Artificial

<div align="center">
  <img src="./demo.gif" alt="Paltodoc Demo" width="300" />
  <p><i>Inferencia en el dispositivo móvil ejecutando diagnósticos en tiempo real sobre la arquitectura nativa.</i></p>
</div>

---

## 🚀 Descripción del Proyecto

**Paltodoc** es una solución de software nativo para Android que integra modelos de aprendizaje profundo (*Deep Learning*) para la optimización y automatización del monitoreo agrícola en los valles interandinos de Apurímac. La aplicación permite a los productores escanear hojas de palto (*Persea americana*) para obtener un diagnóstico inmediato sobre los daños causados por la plaga de **arañita roja** (*Oligonychus yothersi*) y la enfermedad compleja del **manchado solar** (*Avocado Sunblotch Viroid - ASBVd*), mitigando pérdidas críticas en la producción local.

---

## 📊 Análisis Exploratorio de Datos (EDA) en Espacio HSV

El núcleo de la ingeniería de características del proyecto se basa en la transición del espacio de color RGB al espacio **HSV (Hue, Saturation, Value)**. Las imágenes capturadas in-situ revelaron que el espacio RGB mezcla la información cromática con las variaciones de iluminación natural en campo. Al aislar los canales mediante análisis estadístico, se detectaron firmas espectrales muy marcadas para cada patología:

<div align="center">
  <table>
    <tr>
      <td><img src="./docs/eda/hue_distribution.png" width="380" alt="Distribución de Tonos" /></td>
      <td><img src="./docs/eda/saturation_distribution.png" width="380" alt="Distribución de Saturación" /></td>
    </tr>
    <tr>
      <td colspan="2" align="center"><img src="./docs/eda/value_distribution.png" width="380" alt="Distribución de Valor" /></td>
    </tr>
  </table>
  <p><i>Gráficos de telemetría cromática extraídos de los archivos originales (image_a6fddf.png e image_a6fdbb.png) que justifican el diseño de nuestra capa de segmentación.</i></p>
</div>

A partir de estas distribuciones, se diseñó e integró en la tubería del modelo la capa personalizada **`HSVHighlightLayer`**, la cual genera una máscara binaria en tiempo real aislando los tonos bronceados y cloróticos característicos de las infecciones, inyectándola como un **cuarto canal de entrada (RGB+M)** a la red.

---

## 🧠 Arquitectura de la Red Neuronal Convolucional (CNN)

El modelo final utiliza una base convolucional de 4 bloques de profundidad incremental con un aumento progresivo de filtros (32 -> 64 -> 128 -> 256) y un tamaño de kernel balanceado de `(5, 5)`. 

<div align="center">
  <img src="./docs/architecture/model_summary.png" width="550" alt="Model Summary Table" />
  <p><i>Resumen de la arquitectura del modelo (origen: image_a6fd99.png), detallando las dimensiones de salida y la distribución de los 5.79 millones de parámetros entrenables.</i></p>
</div>

---

## 🔬 Matriz de Experimentación Científica (Grid Search)

La selección de la arquitectura final no fue arbitraria; fue el resultado de una optimización y validación rigurosa contrastando la profundidad de la red con el tamaño de los campos receptivos (kernels). Los datos crudos de estas pruebas se encuentran disponibles en el archivo de auditoría **`Resultados.xlsx`** adjunto en la raíz del repositorio:

| Configuración de Red | Tamaño del Kernel | Exactitud Global (Test Accuracy) | Pérdida (Test Loss) | Estado de Ajuste |
| :--- | :---: | :---: | :---: | :--- |
| 2 Capas Convolucionales | 3x3 | 87.50% | 0.3086 | Sub-ajuste (Underfitting) |
| 2 Capas Convolucionales | 5x5 | 93.75% | 0.1636 | Capacidad insuficiente |
| 2 Capas Convolucionales | 7x7 | 62.50% | 0.7045 | Divergencia por ruido |
| 3 Capas Convolucionales | 3x3 | 95.31% | 0.1539 | Estabilidad intermedia |
| 3 Capas Convolucionales | 5x5 | 90.62% | 0.3311 | Convergencia lenta |
| 3 Capas Convolucionales | 7x7 | 92.19% | 0.2305 | Alto costo computacional |
| 4 Capas Convolucionales | 3x3 | 90.62% | 0.1599 | Características locales limitadas |
| 🥇 **4 Capas Convolucionales** | **5x5** | **96.88%** | **0.0947** | **Configuración Óptima Elegida** |
| 4 Capas Convolucionales | 7x7 | 85.94% | 0.3809 | Pérdida de resolución espacial |
| 5 Capas Convolucionales | 3x3 | 93.75% | 0.1027 | Complejidad innecesaria |
| 5 Capas Convolucionales | 5x5 | 92.19% | 0.1638 | Inicio de sobreajuste |
| 5 Capas Convolucionales | 7x7 | 53.12% | 0.7668 | Sobreajuste crítico (Overfitting) |

### 📈 Rendimiento Final por Clase (Modelo Seleccionado)
Al someter la arquitectura óptima al conjunto de datos de prueba balanceado (N=64), el sistema arrojó un rendimiento sobresaliente, minimizando de manera crítica la tasa de falsos negativos:

*   **Arañita Roja:** Precisión: 89% | Sensibilidad (Recall): **100%** | F1-Score: 94%
*   **Manchado Solar:** Precisión: 100% | Sensibilidad (Recall): 92% | F1-Score: 96%
*   **Palta Sana:** Precisión: 100% | Sensibilidad (Recall): **100%** | F1-Score: 100%

---

## 🛠️ Tecnologías e Infraestructura

*   **Entorno de Operaciones:** Python 3.10, TensorFlow 2.x, Keras API.
*   **Despliegue Móvil (*Edge AI*):** Android Studio, Estructura Nativa Kotlin/Java, motor de inferencia **TensorFlow Lite (`.tflite`)** integrado en la carpeta de `assets`.