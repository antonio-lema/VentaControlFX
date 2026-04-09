import os
import re

def to_java_surrogate(code):
    if code <= 0xFFFF:
        return f'\\u{code:04x}'
    code -= 0x10000
    hi = 0xD800 + (code >> 10)
    lo = 0xDC00 + (code & 0x3FF)
    return f'\\u{hi:04x}\\u{lo:04x}'

def fix_bad_u_escapes(text):
    # Matches \uXXXXX (5 digits)
    def replace_bad(match):
        code = int(match.group(1), 16)
        return to_java_surrogate(code)
    # Be careful not to match \uXXXX\uXXXX (standard surrogate pairs)
    # We only match \u followed by AT LEAST 5 hex digits if it's not immediately preceded by another \u
    return re.sub(r'(?<!\\u[0-9a-fA-F]{4})\\u([0-9a-fA-F]{5,6})', replace_bad, text)

def fix_broken_xml_entities(text):
    # Fixes &#xXXXX;Y where Y was meant to be the 5th digit
    # Pattern: &#x([0-9a-f]{4});([0-9a-f]) -> &#x\1\2;
    return re.sub(r'&#x([0-9a-fA-F]{4});([0-9a-fA-F])', r'&#x\1\2;', text)

def process_file(path):
    try:
        with open(path, 'r', encoding='ascii') as f:
            content = f.read()
    except:
        return
    
    initial = content
    
    # 1. Repair the 5-digit escapes in both Java/Properties and FXML (if they exist)
    content = fix_bad_u_escapes(content)
    
    # 2. Repair truncated FXML entities
    if path.endswith('.fxml'):
        content = fix_broken_xml_entities(content)
    
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
print(f"Repair completed. Fixed {count} files.")
