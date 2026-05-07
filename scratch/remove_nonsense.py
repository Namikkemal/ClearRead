import os
import re

def remove_no_nonsense(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # We need to find the settings_about_desc string and remove the "No nonsense" part.
    # It usually ends with "No nonsense." or equivalent in other languages.
    # Pattern: No trackers. No ads. No nonsense. -> No trackers. No ads.
    
    # Since each language is different, we can look for the dot/sentence boundary.
    # In many languages it's at the end.
    
    # Let's see some examples from our files:
    # EN: No trackers. No ads. No nonsense.
    # ES: Sin rastreadores. Sin anuncios. Sin tonterías.
    
    # I'll use a regex to find the string and strip the last sentence if it contains "nonsense" or "tonterías" etc.
    # Actually, simpler: I'll just replace the common translated versions or look for the last part.
    
    # If I look at the files, the last part is usually preceded by a dot and space.
    
    match = re.search(r'<string name="settings_about_desc">(.*?)</string>', content)
    if match:
        full_text = match.group(1)
        # Split by dots or newlines to find sentences
        parts = full_text.split('.')
        # Filter out empty or whitespace-only parts
        filtered_parts = [p.strip() for p in parts if p.strip()]
        
        # If there are 3 parts, the last one is likely the "no nonsense" part
        if len(filtered_parts) >= 3:
            # We take the first two parts and join them back
            new_text = ". ".join(filtered_parts[:2]) + "."
            new_content = content.replace(full_text, new_text)
            
            with open(path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Updated: {path}")

def main():
    base_dir = r"c:\Users\talha\Desktop\ClearRead\app\src\main\res"
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file == "strings.xml":
                remove_no_nonsense(os.path.join(root, file))

if __name__ == "__main__":
    main()
