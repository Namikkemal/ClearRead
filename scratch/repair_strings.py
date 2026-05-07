import os
import re

def fix_encoding(text):
    try:
        # The corruption looks like UTF-8 bytes were interpreted as Latin-1 and re-encoded.
        # We can try to reverse this by encoding to latin-1 and decoding as utf-8.
        # Sometimes it's double-corrupted.
        
        # Level 1 fix
        fixed = text.encode('latin-1').decode('utf-8')
        # If we see more Ã symbols, we might need another pass, but usually once is enough for this pattern.
        if 'Ã' in fixed:
            try:
                fixed = fixed.encode('latin-1').decode('utf-8')
            except:
                pass
        return fixed
    except:
        return text

def repair_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # We target specific common corrupted sequences
    # á -> ÃƒÂ¡ (sometimes Ã¡)
    # ó -> ÃƒÂ³
    # ñ -> ÃƒÂ±
    # · -> Ã‚Â· or similar
    
    # Instead of complex logic, let's just use a simple mapping for the most common ones found in the project
    replacements = {
        'ÃƒÂ¡': 'á',
        'ÃƒÂ©': 'é',
        'ÃƒÂ­': 'í',
        'ÃƒÂ³': 'ó',
        'ÃƒÂº': 'ú',
        'ÃƒÂ±': 'ñ',
        'ÃƒÂ': 'í', # Catch-all for some variations
        'Ã‚Â¿': '¿',
        'Ã‚Â¡': '¡',
        'ÃƒÂ¼': 'ü',
        'ÃƒÂ±': 'ñ',
        'ÃƒÂ§': 'ç',
        'ÃƒÂª': 'ê',
        'ÃƒÂ³': 'ó',
        'Ã‚Â·': '•', # Replace corrupted middle dot with bullet
        'ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â·': '•',
        'ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡': 'á',
        'ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³': 'ó',
        'ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â±': 'ñ'
    }
    
    new_content = content
    for target, replacement in replacements.items():
        new_content = new_content.replace(target, replacement)
    
    if new_content != content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Repaired: {path}")

def main():
    base_dir = r"c:\Users\talha\Desktop\ClearRead\app\src\main\res"
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file == "strings.xml":
                repair_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
