import os
import re

# Mapping of corrupted 5-digit-like escapes (where one 0 was lost and next char was swallowed)
# Format: Current (swallowed) -> Fixed (00 + char + restored swallowed)
replacements = {
    r'\\u0f1([0-9a-fA-F])': r'\\u00f1\1',   # ñ + char
    r'\\u0f3([0-9a-fA-F])': r'\\u00f3\1',   # ó + char
    r'\\u0e9([0-9a-fA-F])': r'\\u00e9\1',   # é + char
    r'\\u0ed([0-9a-fA-F])': r'\\u00ed\1',   # í + char
    r'\\u0e1([0-9a-fA-F])': r'\\u00e1\1',   # á + char
    r'\\u0fa([0-9a-fA-F])': r'\\u00fa\1',   # ú + char
    r'\\u0bf([0-9a-fA-F])': r'\\u00bf\1',   # ¿ + char
    r'\\u0a1([0-9a-fA-F])': r'\\u00a1\1',   # ¡ + char
    r'\\u0d1([0-9a-fA-F])': r'\\u00d1\1',   # Ñ + char
    r'\\u0d3([0-9a-fA-F])': r'\\u00d3\1',   # Ó + char
    r'\\u0c9([0-9a-fA-F])': r'\\u00c9\1',   # É + char
    r'\\u0cd([0-9a-fA-F])': r'\\u00cd\1',   # Í + char
    r'\\u0c1([0-9a-fA-F])': r'\\u00c1\1',   # Á + char
    r'\\u0da([0-9a-fA-F])': r'\\u00da\1',   # Ú + char
    r'\\u0fc([0-9a-fA-F])': r'\\u00fc\1',   # ü + char
}

# Special case for those that are 3-digits ONLY (no swallow or swallowed non-hex)
replacements_short = {
    r'\\u0f1([^0-9a-fA-F])': r'\\u00f1\1',
    r'\\u0f3([^0-9a-fA-F])': r'\\u00f3\1',
    r'\\u0e9([^0-9a-fA-F])': r'\\u00e9\1',
    r'\\u0ed([^0-9a-fA-F])': r'\\u00ed\1',
    r'\\u0e1([^0-9a-fA-F])': r'\\u00e1\1',
    r'\\u0fa([^0-9a-fA-F])': r'\\u00fa\1',
    r'\\u0bf([^0-9a-fA-F])': r'\\u00bf\1',
    r'\\u0a1([^0-9a-fA-F])': r'\\u00a1\1',
}

def fix_file(path):
    try:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        new_content = content
        for pattern, replacement in replacements.items():
            new_content = re.sub(pattern, replacement, new_content)
        
        for pattern, replacement in replacements_short.items():
            new_content = re.sub(pattern, replacement, new_content)
            
        if new_content != content:
            with open(path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Fixed {path}")
    except Exception as e:
        print(f"Error fixing {path}: {e}")

def main():
    root = 'src/main'
    for base, dirs, files in os.walk(root):
        for f in files:
            if f.endswith(('.java', '.fxml', '.properties', '.css')):
                fix_file(os.path.join(base, f))

if __name__ == "__main__":
    main()
