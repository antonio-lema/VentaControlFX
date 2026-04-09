import os
import re

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
    '\\u00c2\\u00a1': '¡',
    '\\u00f0\\u0178\\u2019\\u00a1': '💡',
    '\\u00e2\\u20AC\\u201c': '—',
    '\\u00e2\\u20AC\\u00a2': '•'
}

def escape_to_java(char):
    code = ord(char)
    if code < 128: return char
    return f'\\u{code:04x}'

def fix_file(path):
    with open(path, 'r', encoding='ascii') as f:
        content = f.read()
    
    initial_content = content
    content = re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: f"\\u{m.group(1).lower()}", content)
    
    for corrupted, fixed in DECORRUPT_MAP.items():
        if corrupted.lower() in content:
            content = content.replace(corrupted.lower(), fixed)
    
    if content != initial_content:
        content = re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: chr(int(m.group(1), 16)), content)
        content = "".join(map(escape_to_java, content))
        with open(path, 'w', encoding='ascii') as f:
            f.write(content)
        return True
    return False

root_dir = r"c:\Users\practicassoftware1\Documents\NetBeansProjects\VentaControlFX\src\main"
count = 0
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(('.java', '.properties')):
            if fix_file(os.path.join(root, file)):
                count += 1
print(f"Fixed {count} files.")
