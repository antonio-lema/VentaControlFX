import os
import re

# Comprehensive mapping for corrupted Unicode escapes in this project.
# Cases: 
# 1. \u0XYZ (One zero lost, swallowed one char) -> \u00XY + Z
# 2. \uXYZW (Two zeros lost, swallowed two chars) -> \u00XY + ZW

def repair_content(content):
    # Pattern 1: One zero lost (e.g. \u0f1a -> \u00f1a)
    # Target common Spanish: 
    # ñ: 00f1, ó: 00f3, é: 00e9, í: 00ed, á: 00e1, ú: 00fa, ü: 00fc, ¿: 00bf, ¡: 00a1
    # Ñ: 00d1, Ó: 00d3, É: 00c9, Í: 00cd, Á: 00c1, Ú: 00da, Í: 00cd
    one_zero_targets = '[fecdab][139da1]'
    pattern_one = r'\\u0(' + one_zero_targets + r')([0-9a-fA-F])'
    content = re.sub(pattern_one, r'\\u00\1\2', content)
    
    # Pattern 2: Two zeros lost (e.g. \uf1ad -> \u00f1ad)
    # If we see \u[fecdab][139da][0-9a-f]{2}, it's highly likely a corrupted Spanish char
    two_zero_targets = '[fecdab][139da1]'
    pattern_two = r'\\u(' + two_zero_targets + r')([0-9a-fA-F]{2})'
    content = re.sub(pattern_two, r'\\u00\1\2', content)

    # Special case for those followed by non-hex (no swallow)
    pattern_short = r'\\u0(' + one_zero_targets + r')([^0-9a-fA-F])'
    content = re.sub(pattern_short, r'\\u00\1\2', content)

    return content

def main():
    root = 'src/main'
    for base, dirs, files in os.walk(root):
        for f in files:
            if f.endswith(('.java', '.fxml', '.properties', '.css')):
                path = os.path.join(base, f)
                try:
                    with open(path, 'r', encoding='utf-8') as file:
                        original = file.read()
                    
                    repaired = repair_content(original)
                    
                    if repaired != original:
                        with open(path, 'w', encoding='utf-8') as file:
                            file.write(repaired)
                        print(f"Repaired: {path}")
                except Exception as e:
                    print(f"Error in {path}: {e}")

if __name__ == "__main__":
    main()
