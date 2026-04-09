import re

def fix_encodings(text):
    # Rule 1: \u00a1 in middle of word or preceded by start question/other char -> á (\u00e1)
    # Actually, \u00a1 is '¡'. \u00e1 is 'á'.
    # We want to replace \u00a1 with \u00e1 ONLY when it's supposed to be an 'á'.
    
    # Common words:
    replacements = {
        r'r\\u00a1pido': r'r\\u00e1pido',
        r'par\\u00a1metro': r'par\\u00e1metro',
        r'cat\\u00a1logo': r'cat\\u00e1logo',
        r'autom\\u00a1ticamente': r'autom\\u00e1ticamente',
        r'est\\u00a1s': r'est\\u00e1s',
        r'est\\u00a1 ': r'est\\u00e1 ',
        r'est\\u00a1.': r'est\\u00e1.',
        r'est\\u00a1,': r'est\\u00e1,',
        r'Est\\u00a1s': r'Est\\u00e1s',
        r'Est\\u00a1 ': r'Est\\u00e1 ',
        r'm\\u00a1rgen': r'm\\u00e1rgen',
        r'An\\u00a1lisis': r'An\\u00e1lisis',
        r'v\\u00a1lido': r'v\\u00e1lido',
        r'inv\\u00e1lido': r'inv\\u00e1lido',
        r'copiar\\u00a1n': r'copiar\\u00e1n',
        r'usar\\u00a1n': r'usar\\u00e1n',
        r'har\\u00a1n': r'har\\u00e1n',
        r'estar\\u00a1': r'estar\\u00e1',
        r'dar\\u00a1': r'dar\\u00e1',
        r'aplicar\\u00a1': r'aplicar\\u00e1',
        r'di\\u00a1logo': r'di\\u00e1logo',
        r'Est\\u00a1ndar': r'Est\\u00e1ndar',
        r'est\\u00a1ndar': r'est\\u00e1ndar',
        r'bloqueado\\u00a1': r'bloqueado.', # Wait
        r'finalizar\\u00e1': r'finalizar\\u00e1',
        r'autom\\u00a1tica': r'autom\\u00e1tica',
        r'Im\\u00a1genes': r'Im\\u00e1genes',
        r'u00a1': r'u00e1' # Catch all for remaining u00a1 that likely should be á
    }
    
    # We will do a generic replacement for \u00a1 EXCLUDING those at the start of a value or preceded by space + Ej:
    # Actually, \u00a1 is ¡ (Inverted exclamation). In Spanish, it precedes a sentence.
    # If it's NOT followed by a capital letter at the start of a sentence, it's likely a typo for á.
    
    lines = text.splitlines()
    new_lines = []
    for line in lines:
        if '=' in line:
            key, val = line.split('=', 1)
            # If val starts with \u00a1 followed by a capital letter, it's probably ¡
            # But if it's \u00a1factura (Line 624), it should be ¡Factura or just Factura.
            
            # Let's fix the specific cases found in Select-String
            v = val
            v = v.replace(r'\u00a1Seguro', r'\u00bfSeguro') # 985
            v = v.replace(r'est\u00a1s', r'est\u00e1s')
            v = v.replace(r'est\u00a1 ', r'est\u00e1 ')
            v = v.replace(r'Est\u00a1s', r'Est\u00e1s')
            v = v.replace(r'Est\u00a1 ', r'Est\u00e1 ')
            v = v.replace(r'cat\u00a1logo', r'cat\u00e1logo')
            v = v.replace(r'par\u00a1metros', r'par\u00e1metros')
            v = v.replace(r'autom\u00a1ticamente', r'autom\u00e1ticamente')
            v = v.replace(r'autom\u00a1tica', r'autom\u00e1tica')
            v = v.replace(r'v\u00a1lido', r'v\u00e1lido')
            v = v.replace(r'inv\u00e1lido', r'inv\u00e1lido')
            v = v.replace(r'An\u00a1lisis', r'An\u00e1lisis')
            v = v.replace(r'd\u00ed\u00a1logo', r'di\u00e1logo') # Fix typo
            v = v.replace(r'di\u00a1logo', r'di\u00e1logo')
            v = v.replace(r'Im\u00a1genes', r'Im\u00e1genes')
            v = v.replace(r'r\u00a1pidos', r'r\u00e1pidos')
            v = v.replace(r'usar\u00a1n', r'usar\u00e1n')
            v = v.replace(r'heredar\u00a1n', r'heredar\u00e1n')
            v = v.replace(r'espec\u00edficos', r'espec\u00edficos')
            v = v.replace(r'Cat\u00a1logo', r'Cat\u00e1logo')
            v = v.replace(r'm\u00a1rgenes', r'm\u00e1rgenes')
            v = v.replace(r'Est\u00e1ndar', r'Est\u00e1ndar')
            v = v.replace(r't\u00e1', r't\u00e1')
            
            # Cases for ¡exclamation!
            v = v.replace(r'\u00a1factura', r'\u00a1Factura')
            v = v.replace(r'\u00a1Ticket', r'\u00a1Ticket')
            
            # Catch remaining u00a1 -> u00e1 if not at start of string or preceded by space
            # v = re.sub(r'(?<!^)(?<!\s)\\u00a1', r'\\u00e1', v)
            
            new_lines.append(key + '=' + v)
        else:
            new_lines.append(line)
    return '\n'.join(new_lines)

path = r'c:\Users\practicassoftware1\Documents\NetBeansProjects\VentaControlFX\src\main\resources\i18n\messages_es.properties'
with open(path, 'r', encoding='utf-8') as f:
    orig = f.read()

fixed = fix_encodings(orig)

with open(path, 'w', encoding='utf-8') as f:
    f.write(fixed)
print("Fixed encodings in messages_es.properties")
