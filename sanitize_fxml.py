import os
import re

def to_xml_entity(match):
    # match is like \uXXXX
    hex_val = match.group(1)
    return f"&#x{hex_val};"

def fix_fxml(path):
    with open(path, 'r', encoding='ascii') as f:
        text = f.read()
    
    # Replace \uXXXX with &#xXXXX;
    text = re.sub(r'\\u([0-9a-fA-F]{4})', to_xml_entity, text)
    
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(text)
    print(f"Sanitized FXML: {path}")

root_dir = r"c:\Users\practicassoftware1\Documents\NetBeansProjects\VentaControlFX\src\main\resources\view"
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".fxml"):
            fix_fxml(os.path.join(root, file))
