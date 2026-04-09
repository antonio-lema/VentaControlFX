import os
import re

# Mapping of double-encoded sequences (UTF-8 read as ISO-8859-1 then escaped to \uXXXX)
# Using lowercase hex as previous script used lowercase.
DECORRUPT_MAP = {
    '\\u00c3\\u00a1': 'á',
    '\\u00c3\\u00a9': 'é',
    '\\u00c3\\u00ad': 'í',
    '\\u00c3\\u00b3': 'ó',
    '\\u00c3\\u00ba': 'ú',
    '\\u00c3\\u00b1': 'ñ',
    '\\u00c3\\u0081': 'Á',
    '\\u00c3\\u0089': 'É',
    '\\u00c3\\u008d': 'Í',
    '\\u00c3\\u0093': 'Ó',
    '\\u00c3\\u009a': 'Ú',
    '\\u00c3\\u0091': 'Ñ',
    '\\u00c2\\u00bf': '¿',
    '\\u00c2\\u00a1': '¡'
}

def escape_to_xml(char):
    code = ord(char)
    if code < 128: return char
    return f'&#x{code:04x};'

def escape_to_java(char):
    code = ord(char)
    if code < 128: return char
    return f'\\u{code:04x}'

def process_file(path):
    try:
        with open(path, 'r', encoding='ascii') as f:
            content = f.read()
    except Exception:
        return

    # 1. Reverse the double encoding
    for corrupted, fixed in DECORRUPT_MAP.items():
        content = content.replace(corrupted, fixed)
        # Also try uppercase just in case
        content = content.replace(corrupted.upper(), fixed)
    
    # 2. Convert all literal non-ascii back to escapes based on file type
    # Standardize to lowercase hex for consistency
    if path.endswith('.fxml'):
        content = re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: chr(int(m.group(1), 16)), content)
        content = "".join(map(escape_to_xml, content))
    else:
        content = re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: chr(int(m.group(1), 16)), content)
        content = "".join(map(escape_to_java, content))

    with open(path, 'w', encoding='ascii') as f:
        f.write(content)

root_dir = r"c:\Users\practicassoftware1\Documents\NetBeansProjects\VentaControlFX\src\main"
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(('.java', '.properties', '.fxml')):
            process_file(os.path.join(root, file))
print("Final Remediation Complete.")
