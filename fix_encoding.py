import os
import re

def to_unicode_escape(char):
    return f"\\u{ord(char):04x}"

def fix_non_ascii(text):
    # This regex matches any non-ASCII character
    return re.sub(r'[^\x00-\x7F]', lambda m: to_unicode_escape(m.group(0)), text)

def fix_file(path):
    with open(path, 'rb') as f:
        content = f.read()
    
    try:
        text = content.decode('utf-8')
    except UnicodeDecodeError:
        try:
            text = content.decode('iso-8859-1')
        except:
            return
    
    # 1. Specialized fixes for known corruptions
    text = text.replace("â‚¬", "\\u20AC")
    text = text.replace("â˜…", "\\u2605")
    
    # 2. General non-ASCII conversion for all files to be safe
    if path.endswith(".java") or path.endswith(".properties") or path.endswith(".fxml"):
        text = fix_non_ascii(text)
    
    with open(path, 'w', encoding='ascii', newline='') as f:
        f.write(text)
    print(f"Fixed {path}")

root_dir = r"c:\Users\practicassoftware1\Documents\NetBeansProjects\VentaControlFX\src\main"
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith((".java", ".properties", ".fxml")):
            fix_file(os.path.join(root, file))
