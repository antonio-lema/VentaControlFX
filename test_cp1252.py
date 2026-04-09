try:
    print(f"CP1252 for \\u201d: {chr(0x201d).encode('cp1252').hex()}")
except Exception as e:
    print(f"Error: {e}")
