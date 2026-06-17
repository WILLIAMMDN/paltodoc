from kivymd.app import MDApp
from kivymd.uix.screen import MDScreen
from kivymd.uix.dialog import MDDialog
from kivymd.uix.button import MDRaisedButton, MDFlatButton
from kivy.lang import Builder
from kivy.clock import Clock
import numpy as np
import tensorflow as tf
import cv2

# --- DISEÑO VISUAL (KV) ---
KV_DESIGN = '''
MDScreen:
    md_bg_color: 0.95, 0.98, 0.95, 1
    MDTopAppBar:
        title: "Doctor Palta AI"
        pos_hint: {"top": 1}
        elevation: 4
        md_bg_color: 0.2, 0.5, 0.2, 1
        specific_text_color: 1, 1, 1, 1
    MDCard:
        size_hint: 0.9, 0.6
        pos_hint: {"center_x": 0.5, "center_y": 0.55}
        elevation: 3
        radius: [20, 20, 20, 20]
        padding: "5dp"
        Camera:
            id: camera_widget
            resolution: (640, 480)
            play: True
            allow_stretch: True
            keep_ratio: True
    MDSpinner:
        id: loading_spinner
        size_hint: None, None
        size: dp(46), dp(46)
        pos_hint: {'center_x': .5, 'center_y': .55}
        active: False
        palette: [[0.2, 0.5, 0.2, 1]]
    MDFloatingActionButtonExtended:
        text: "ANALIZAR HOJA"
        icon: "camera-iris"
        pos_hint: {"center_x": 0.5, "y": 0.1}
        size_hint_x: 0.8
        elevation: 4
        md_bg_color: 0.2, 0.5, 0.2, 1
        text_color: 1, 1, 1, 1
        on_release: app.iniciar_analisis()
'''

class DoctorPaltaApp(MDApp):
    def build(self):
        self.theme_cls.theme_style = "Light"
        self.theme_cls.primary_palette = "Green"
        screen = Builder.load_string(KV_DESIGN)
        self.camera = screen.ids.camera_widget
        self.spinner = screen.ids.loading_spinner
        self.cargar_modelo()
        return screen

    def cargar_modelo(self):
        try:
            self.interpreter = tf.lite.Interpreter(model_path="model_palta.tflite")
            self.interpreter.allocate_tensors()
            self.input_details = self.interpreter.get_input_details()
            self.output_details = self.interpreter.get_output_details()
            # IMPORTANTE: El orden debe coincidir con tu entrenamiento
            self.classes = ['Arañita Roja', 'Rocha', 'Sana'] 
        except:
            self.mostrar_resultado("Error", 0, "Sistema", 0)

    def iniciar_analisis(self):
        self.spinner.active = True
        Clock.schedule_once(self.analizar_imagen, 0.1)

    def analizar_imagen(self, dt):
        if not self.camera.texture: 
            self.spinner.active = False
            return
            
        # 1. Preparar imagen
        img_np = np.frombuffer(self.camera.texture.pixels, dtype=np.uint8)
        img_np = img_np.reshape(self.camera.texture.size[1], self.camera.texture.size[0], 4)
        img_rgb = cv2.cvtColor(img_np, cv2.COLOR_RGBA2RGB)
        img_resized = cv2.resize(img_rgb, (256, 256))
        input_data = np.expand_dims(img_resized, axis=0).astype(np.float32)

        # 2. Inferencia IA
        self.interpreter.set_tensor(self.input_details[0]['index'], input_data)
        self.interpreter.invoke()
        output = self.interpreter.get_tensor(self.output_details[0]['index'])[0]
        
        idx = np.argmax(output)
        prediccion = self.classes[idx]
        confianza = output[idx] * 100

        # 3. Severidad
        severidad = 0
        nivel = "Nulo"
        if prediccion != 'Sana':
            severidad = self.calcular_severidad(img_rgb)
            if severidad < 5: nivel = "Leve"
            elif severidad < 20: nivel = "Moderado"
            else: nivel = "Crítico"

        self.spinner.active = False
        self.mostrar_resultado(prediccion, confianza, nivel, severidad)

    def calcular_severidad(self, img):
        hsv = cv2.cvtColor(img, cv2.COLOR_RGB2HSV)
        # Detectar tonos rojos/marrones
        mask1 = cv2.inRange(hsv, np.array([0, 40, 40]), np.array([20, 255, 255]))
        mask2 = cv2.inRange(hsv, np.array([160, 40, 40]), np.array([180, 255, 255]))
        ratio = cv2.countNonZero(mask1 | mask2) / (img.shape[0] * img.shape[1])
        return ratio * 100

    def mostrar_resultado(self, enf, conf, niv, sev):
        color = [0.2, 0.6, 0.2, 1] if enf == 'Sana' else [0.8, 0.2, 0.2, 1]
        msg = f"Certeza IA: {conf:.1f}%\n\nNivel Daño: {niv}\nÁrea Afectada: {sev:.1f}%"
        
        MDDialog(
            title=f"Diagnóstico: {enf}",
            text=msg,
            buttons=[MDRaisedButton(text="OK", md_bg_color=color, on_release=lambda x: x.parent.parent.parent.dismiss())]
        ).open()

if __name__ == "__main__":
    DoctorPaltaApp().run()