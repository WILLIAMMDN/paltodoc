<div align="center">
  <img src="./icon.png" width="120" alt="Paltodoc Logo" />
  <h1>🥑 Paltodoc</h1>
  <p><strong>Detección Temprana de Anomalías Foliares mediante Visión Artificial Nativa en el Dispositivo</strong></p>
  
  [![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![TensorFlow Lite](https://img.shields.io/badge/ML-TensorFlow%20Lite-FF6F00?logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)
</div>

---

## 🚀 Descripción del Proyecto

**Paltodoc** es una solución de software de agricultura de precisión desarrollada de manera nativa para el ecosistema Android. Su objetivo principal es empoderar a los productores de los valles interandinos de Apurímac mediante herramientas de diagnóstico fitosanitario inmediato. Utilizando Redes Neuronales Convolucionales (CNN) optimizadas para dispositivos móviles (*Edge AI*), la aplicación procesa capturas foliares en tiempo real para identificar ataques de la plaga **arañita roja** (*Oligonychus yothersi*) y la sintomatología compleja del **manchado solar** (*Avocado Sunblotch Viroid - ASBVd*).

<div align="center">
  <img src="./demo.gif" alt="Paltodoc Demo" width="280" />
  <br>
  <i>Inferencia local ejecutándose en tiempo real sobre el emulador Android.</i>
</div>

---

## 🛠️ Stack Tecnológico & Versiones

El desarrollo se encuentra desacoplado en dos componentes de ingeniería principales para maximizar el rendimiento computacional:

### 🧠 Entorno de Inteligencia Artificial (Core ML)
* **Lenguaje:** Python v3.10.
* **Frameworks:** TensorFlow v2.14.0 y API de Keras.
* **Arquitectura Base:** Red Neuronal Convolucional (CNN) optimizada de 4 capas con kernel adaptativo de `(5, 5)` e ingeniería de características en el espacio de color **HSV** (`HSVHighlightLayer`).
* **Optimización:** Cuantización y exportación estructurada a formato plano de TensorFlow Lite (`.tflite`).

### 📱 Aplicación Móvil (Cliente Nativo)
* **IDE:** Android Studio.
* **Lenguaje de Programación:** Kotlin v1.9 / Java.
* **Compilador de Entorno:** Gradle (Kotlin DSL - `build.gradle.kts`).
* **Motor de Inferencia Local:** TensorFlow Lite Android Task Library v2.14.0.

---

## 📊 Ingeniería de Características & Análisis Exploratorio (EDA)

El modelo mitiga los cambios bruscos de iluminación natural mediante la inyección en tiempo real de una máscara binaria basada en el espacio cromático **HSV**. Las firmas espectrales extraídas de nuestra investigación de campo justifican el aislamiento de las lesiones foliares:

<div align="center">
  <table>
    <tr>
      <td><img src="./docs/eda/hue_distribution.png" width="260" alt="Distribución Hue" /></td>
      <td><img src="./docs/eda/saturation_distribution.png" width="260" alt="Distribución Saturation" /></td>
      <td><img src="./docs/eda/value_distribution.png" width="260" alt="Distribución Value" /></td>
    </tr>
  </table>
  <p><i>Histogramas analíticos de distribución foliar para el aislamiento de clorosis y necrosis (origen: image_a6fddf.png e image_a6fdbb.png).</i></p>
</div>

---

## 🔬 Matriz de Control Fitosanitario

El modelo procesa las características morfológicas y cromáticas de las muestras foliares recopiladas en Kaquiabamba, Talavera y Chincheros, clasificándolas en tres estados biológicos estrictos:

<div align="center">
  <table>
    <tr>
      <td align="center"><strong>1. Estado Fitosanitario Óptimo (Sana)</strong></td>
      <td align="center"><strong>2. Ataque de Arañita Roja</strong></td>
      <td align="center"><strong>3. Infección por Manchado Solar</strong></td>
    </tr>
    <tr>
      <td align="center"><img src="./docs/samples/hoja_sana.jpg" width="220" alt="Hoja Sana" /></td>
      <td align="center"><img src="./docs/samples/hoja_aranita_roja.jpg" width="220" alt="Arañita Roja" /></td>
      <td align="center"><img src="./docs/samples/hoja_manchado_solar.jpg" width="220" alt="Manchado Solar" /></td>
    </tr>
    <tr>
      <td align="center">Coloración verde uniforme, textura brillante y elástica.</td>
      <td align="center">Punteaduras cloróticas evolucionadas a extensas áreas cobrizas.</td>
      <td align="center">Vetas y manchas polimórficas de color amarillo brillante.</td>
    </tr>
  </table>
</div>

---

## 📈 Resultados del Grid Search Científico

Se realizaron 12 experimentos variando la complejidad estructural de la red y el tamaño de sus campos receptivos para encontrar el balance óptimo entre peso y exactitud. Los datos crudos detallados se encuentran en el archivo adjunto **`Resultados.xlsx`**:

| Capas Convolucionales | Tamaño de Kernel | Exactitud (Test Accuracy) | Pérdida (Test Loss) | Estado Estructural |
| :---: | :---: | :---: | :---: | :--- |
| 2 Capas | 3x3 | 87.50% | 0.3086 | Sub-ajuste (Underfitting) |
| 2 Capas | 5x5 | 93.75% | 0.1636 | Capacidad insuficiente |
| 2 Capas | 7x7 | 62.50% | 0.7045 | Divergencia por ruido de fondo |
| 3 Capas | 3x3 | 95.31% | 0.1539 | Estabilidad local aceptable |
| 3 Capas | 5x5 | 90.62% | 0.3311 | Estancamiento en gradiente |
| 3 Capas | 7x7 | 92.19% | 0.2305 | Alto costo computacional |
| 4 Capas | 3x3 | 90.62% | 0.1599 | Características locales limitadas |
| 🥇 **4 Capas** | **5x5** | **96.88%** | **0.0947** | **Configuración Óptima Seleccionada** |
| 4 Capas | 7x7 | 85.94% | 0.3809 | Pérdida de resolución espacial |
| 5 Capas | 3x3 | 93.75% | 0.1027 | Complejidad estructural redundante |
| 5 Capas | 5x5 | 92.19% | 0.1638 | Inicio latente de sobreajuste |
| 5 Capas | 7x7 | 53.12% | 0.7668 | Sobreajuste crítico (Overfitting) |

### Metrics Report & Matriz de Confusión del Modelo Ganador

<div align="center">
  <table>
    <tr>
      <td><img src="./docs/metrics/confusion_matrix.png" width="360" alt="Confusion Matrix" /></td>
      <td><img src="./docs/metrics/accuracy_curve.png" width="400" alt="Training Graphs" /></td>
    </tr>
  </table>
</div>

* **Sensibilidad (Recall) Crítica:** Alcanza un **100% de efectividad en la detección de Arañita Roja** y un **100% de precisión en plantas Sanas**, mitigando el riesgo de falsos negativos y falsas alarmas que generen gastos innecesarios en pesticidas al agricultor.

---

## 💻 Guía de Implementación en Kotlin (Android Nativo)

### 1. Configurar dependencias (`build.gradle.kts`)
Agrega el motor de ejecución de TensorFlow Lite a nivel de módulo en tu aplicación:

```kotlin
dependencies {
    // Android Core & UI
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Motor de Inferencia TensorFlow Lite (Edge AI)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
}