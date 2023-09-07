import kivymd
from kivymd.app import MDApp
from kivy.lang import Builder
from kivymd.uix.button import MDIconButton
from kivymd.uix.screen import MDScreen
from kivymd.uix.label import MDLabel

KV = '''
MDScreen:

    MDLabel:
        text: "Test"

    MDIconButton:
        icon: "language-python"
        pos_hint: {"center_x": .5, "center_y": .5}
'''

class test(MDApp):
    def build(self):
        self.theme_cls.theme_style = "Dark"
        self.theme_cls.primary_palette = "Blue"
        return Builder.load_string(KV)
    
test().run()