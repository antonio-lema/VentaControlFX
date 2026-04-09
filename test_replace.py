import re

text = r"podr\u00C3\u00ADamos"
corrupted = r"\u00C3\u00AD"
fixed = "í"
result = text.replace(corrupted, fixed)
print(f"Result: {result}")
