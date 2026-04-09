import os

def sanitize_file(path):
    try:
        with open(path, 'rb') as f:
            content = f.read()
        if b'\x00' in content:
            print(f"Found NULL byte in {path}")
            new_content = content.replace(b'\x00', b'')
            with open(path, 'wb') as f:
                f.write(new_content)
            return True
    except Exception as e:
        print(f"Error processing {path}: {e}")
    return False

root_dir = r'c:\Users\practicassoftware1\Documents\NetBeansProjects\VentaControlFX'
files_to_check = []
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(('.java', '.fxml', '.css', '.properties')):
            files_to_check.append(os.path.join(root, file))

count = 0
for f in files_to_check:
    if sanitize_file(f):
        count += 1

print(f"Sanitized {count} files.")
