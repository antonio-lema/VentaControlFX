import re

def fix_mojibake(text):
    def replace_match(match):
        s = match.group()
        # Find all \uXXXX
        escapes = re.findall(r'\\u([0-9a-fA-F]{4})', s)
        try:
            # Map each escape back to the byte it represents in Windows-1252
            byte_data = bytearray()
            for e in escapes:
                char = chr(int(e, 16))
                byte_data.extend(char.encode('cp1252'))
            
            # Decode the whole thing as UTF-8
            decoded = byte_data.decode('utf-8')
            # If successful, return the literals. 
            # Wait! If it's a Java file, we might want to re-escape them?
            # Yes, because we want to stay in ASCII.
            def to_java(c):
                code = ord(c)
                if code < 128: return c
                return f'\\u{code:04x}'
            
            return "".join(map(to_java, decoded))
        except Exception:
            # If it's not valid UTF-1252/UTF-8 mojibake, leave it alone
            return s

    # Match 2 to 4 consecutive \uXXXX escapes
    return re.sub(r'(\\u[0-9a-fA-F]{4}){2,4}', replace_match, text)

# Tests
test_cases = [
    (r"podr\u00c3\u00adamos", r"podr\u00edamos"),
    (r"\u00f0\u0178\u201c\u0160", r"\u1f4ca"), # 📊 is U+1F4CA
    (r"\u00f0\u0178\u2019\u00b0", r"\u1f4b0"), # 💰 is U+1F4B0
    (r"\u00c2\u00a0", " "), # NBSP
]

for t, expected in test_cases:
    result = fix_mojibake(t)
    print(f"Input: {t} -> Result: {result} (Expected: {expected})")
