package org.tesis.paltodoc

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private var layoutDiagnostico: LinearLayout? = null
    private var layoutBiblioteca: ScrollView? = null
    private var layoutNosotros: LinearLayout? = null
    private var bottomNav: BottomNavigationView? = null
    private var imageView: ImageView? = null
    private var btnCapture: Button? = null
    private var btnGallery: Button? = null

    // Configuración VGG16
    val imageSize = 224
    val CAMERA_REQUEST = 100
    val GALLERY_REQUEST = 101
    val MODEL_NAME = "modelo_palta_vgg16.tflite"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            layoutDiagnostico = findViewById(R.id.layout_diagnostico)
            layoutBiblioteca = findViewById(R.id.layout_biblioteca)
            layoutNosotros = findViewById(R.id.layout_nosotros)
            bottomNav = findViewById(R.id.bottom_navigation)
            imageView = findViewById(R.id.imageView)
            btnCapture = findViewById(R.id.btnCapture)
            btnGallery = findViewById(R.id.btnGallery)

            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        bottomNav?.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_diagnostico -> showLayout(layoutDiagnostico)
                R.id.nav_biblioteca -> showLayout(layoutBiblioteca)
                R.id.nav_equipo -> showLayout(layoutNosotros)
            }
            true
        }

        btnCapture?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
            }
        }

        btnGallery?.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Selecciona una foto"), GALLERY_REQUEST)
        }
    }

    private fun showLayout(viewToShow: View?) {
        layoutDiagnostico?.visibility = View.GONE
        layoutBiblioteca?.visibility = View.GONE
        layoutNosotros?.visibility = View.GONE
        viewToShow?.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            try {
                var imageBitmap: Bitmap? = null

                if (requestCode == CAMERA_REQUEST) {
                    imageBitmap = data?.extras?.get("data") as Bitmap
                } else if (requestCode == GALLERY_REQUEST) {
                    val uri = data?.data
                    if (uri != null) {
                        val inputStream = contentResolver.openInputStream(uri)
                        val options = BitmapFactory.Options()
                        options.inSampleSize = 2
                        imageBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    }
                }

                if (imageBitmap != null) {
                    imageView?.setImageBitmap(imageBitmap)
                    imageView?.setPadding(0,0,0,0)

                    // CORRECCIÓN: Quitamos los bordes negros.
                    // Estiramos la imagen directamente a 224x224 (Igual que Keras en Colab)
                    val scaledImage = Bitmap.createScaledBitmap(imageBitmap, imageSize, imageSize, true)

                    // Pasamos directo al clasificador (quitamos el filtro de hoja por ahora para probar puro)
                    classifyImage(scaledImage)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun classifyImage(image: Bitmap) {
        try {
            val model = loadModelFile()
            val options = Interpreter.Options()
            val interpreter = Interpreter(model, options)

            val inputBuffer = ByteBuffer.allocateDirect(1 * imageSize * imageSize * 3 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val `val` = intValues[pixel++]

                    // === MATEMÁTICA VGG16 (SEGÚN TU CÓDIGO PYTHON) ===
                    // 1. Extraer RGB (0-255)
                    val r = (`val` shr 16 and 0xFF).toFloat()
                    val g = (`val` shr 8 and 0xFF).toFloat()
                    val b = (`val` and 0xFF).toFloat()

                    // 2. Orden BGR (No RGB)
                    // 3. Restar Media ImageNet (Sin dividir entre 255)
                    inputBuffer.putFloat(b - 103.939f)
                    inputBuffer.putFloat(g - 116.779f)
                    inputBuffer.putFloat(r - 123.68f)
                }
            }

            val outputBuffer = Array(1) { FloatArray(3) }
            interpreter.run(inputBuffer, outputBuffer)

            val classes = arrayOf("Arañita Roja", "Manchado Solar", "Sana")

            val probabilities = outputBuffer[0]
            var maxPos = 0
            var maxConfidence = 0.0f

            for (i in probabilities.indices) {
                if (probabilities[i] > maxConfidence) {
                    maxConfidence = probabilities[i]
                    maxPos = i
                }
            }

            mostrarResultadoDialog(classes[maxPos], maxConfidence * 100)
            interpreter.close()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error IA: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = assets.openFd(MODEL_NAME)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength)
    }

    private fun mostrarResultadoDialog(enfermedad: String, confianza: Float) {
        try {
            val dialog = android.app.Dialog(this)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.sheet_resultado)

            val txtTitulo = dialog.findViewById<TextView>(R.id.txtTituloEnfermedad)
            val txtConfianza = dialog.findViewById<TextView>(R.id.txtHeaderConfianza)
            val txtCuerpo = dialog.findViewById<TextView>(R.id.txtCuerpoInforme)
            val btnCerrar = dialog.findViewById<Button>(R.id.btnEntendido)

            txtTitulo.text = enfermedad.uppercase()
            txtConfianza.text = "Certeza: ${String.format("%.1f", confianza)}%"

            var info = ""
            if (enfermedad == "Arañita Roja") {
                info = "• SÍNTOMAS:\nColor bronceado/rojizo en el haz.\n\n" +
                        "• QUÍMICO:\nAbamectina (1.8% EC) o Azufre.\n\n" +
                        "• CULTURAL:\nLavado a presión y aumentar humedad."
            } else if (enfermedad == "Manchado Solar") {
                info = "• CAUSA:\nViroide (ASBVd) o Daño Abiótico.\n\n" +
                        "• MANEJO:\nNO TIENE CURA QUÍMICA. Desinfectar herramientas.\n\n" +
                        "• PREVENCIÓN:\nUsar plantas certificadas."
            } else {
                info = "• ESTADO:\nSaludable.\n\n" +
                        "• ACCIÓN:\nNinguna. Continuar monitoreo."
            }

            txtCuerpo.text = info

            btnCerrar.setOnClickListener { dialog.dismiss() }

            dialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            dialog.show()

        } catch (e: Exception) {
            Toast.makeText(this, "Resultado: $enfermedad", Toast.LENGTH_LONG).show()
        }
    }
}