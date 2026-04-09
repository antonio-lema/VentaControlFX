import os
import re

patterns = [
    r'\\u0f1', # ñ
    r'\\u0f3', # ó
    r'\\u0e9', # é
    r'\\u0ed', # í
    r'\\u0e1', # á
    r'\\u0fa', # ú
    r'\\u0bf', # ¿
    r'\\u0a1', # ¡
    r'\\u0d1', # Ñ
    r'\\u0d3', # Ó
    r'\\u0c9', # É
    r'\\u0cd', # Í
    r'\\u0c1', # Á
    r'\\u0da', # Ú
]

def scan_files(root_dir):
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith(('.java', '.fxml', '.properties', '.css')):
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        content = f.read()
                        for p in patterns:
                            if re.search(p, content):
                                print(f"Found {p} in {path}")
                                # Print context
                                for match in re.finditer(p + r'.', content):
                                    print(f"  Context: {content[max(0, match.start()-10):min(len(content), match.end()+10)]}")
                except:
                    pass

scan_files('c:\\Users\\practicassoftware1\\Documents\\NetBeansProjects\\VentaControlFX\\src')
