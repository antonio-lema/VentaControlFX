import os
import re

def fix_mojibake(text):
    def replace_match(match):
        s = match.group()
        escapes = re.findall(r'\\u([0-9a-fA-F]{4})', s)
        try:
            byte_data = bytearray()
            for e in escapes:
                char = chr(int(e, 16))
                try:
                    # Windows-1252 is the most likely source of mojibake in this environment
                    byte_data.extend(char.encode('cp1252'))
                except:
                    # Direct byte fallback for characters like \u008d (latin1)
                    byte_data.append(ord(char) & 0xFF)
            
            decoded = byte_data.decode('utf-8')
            
            def to_java(c):
                code = ord(c)
                if code < 128: return c
                return f'\\u{code:04x}'
            
            return "".join(map(to_java, decoded))
        except Exception:
            return s

    # Match 2 to 4 consecutive \uXXXX escapes
    return re.sub(r'(\\u[0-9a-fA-F]{4}){2,4}', replace_match, text)

def fix_fxml_mojibake(text):
    text = re.sub(r'&#x([0-9a-fA-F]{4});', r'\\u\1', text)
    text = fix_mojibake(text)
    def escape_to_xml(match):
        h = match.group(1)
        code = int(h, 16)
        if code < 128: return chr(code)
        return f'&#x{h};'
    text = re.sub(r'\\u([0-9a-fA-F]{4})', escape_to_xml, text)
    return text

def process_file(path):
    try:
        with open(path, 'r', encoding='ascii') as f:
            content = f.read()
    except:
        return
    initial = content
    if path.endswith('.fxml'):
        content = fix_fxml_mojibake(content)
    else:
        # Standardize to lowercase hex for matching
        content = re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: f"\\u{m.group(1).lower()}", content)
        content = fix_mojibake(content)
    if content != initial:
        with open(path, 'w', encoding='ascii') as f:
            f.write(content)
        return True
    return False

root_dir = r"c:\Users\practicassoftware1\Documents\NetBeansProjects\VentaControlFX\src\main"
count = 0
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(('.java', '.properties', '.fxml')):
            if process_file(os.path.join(root, file)):
                count += 1
print(f"Super Fix completed. Fixed {count} files.")
