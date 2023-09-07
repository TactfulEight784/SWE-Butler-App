import kivymd
from kivymd.app import MDApp
from kivy.lang import Builder
from kivymd.uix.button import MDIconButton , MDTextButton
from kivymd.uix.screen import MDScreen
from kivymd.uix.label import MDLabel
import random
KV = '''
root:
    random_label: random_label
    MDScreen:

        MDLabel:
            text: "Braille"
            halign: "center"
            font_style: "H1"
            pos_hint: {"center_y": .9}


        MDIconButton:
            icon: "—Pngtree—start button rounded futuristic hologram_5426086.png"
            icon_size: "750sp"
            pos_hint: {"center_x": .5, "center_y": .4}
            on_release: root.generate_number()
        MDLabel:
            id: random_label #sets this as its id so that i know where its changing
            text:"-"
            font_size: 64

'''
class root(MDScreen):
    def generate_number(self):
        self.random_label.text = str(random.randint(0,1000))

class test(MDApp):



    def build(self):
        self.theme_cls.theme_style = "Dark"
        self.theme_cls.primary_palette = "Blue"
        return Builder.load_string(KV)
    
test().run()