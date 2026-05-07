import os
import re

def fix_description(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Target the specific settings_about_desc string
    match = re.search(r'(<string name="settings_about_desc">)(.*?)(</string>)', content, re.DOTALL)
    if match:
        prefix, text, suffix = match.groups()
        # Find all sentences (parts ending in . or similar)
        # We want to remove the very last part if it looks like the nonsense part.
        
        # Split by dots
        parts = text.split('.')
        # Remove the last part if it's the "nonsense" part. 
        # Typically the string has 3 sentences after the first line.
        # Line 1: A clean... reader.
        # Line 2: No trackers.
        # Line 3: No ads.
        # Line 4: No nonsense.
        
        clean_parts = [p.strip() for p in parts if p.strip()]
        
        # If we have 4 parts, the 4th is nonsense.
        # If we have 3, and we already messed it up (only 2 left), we should restore them.
        
        # Let's just hardcode the correct English one and then try to infer for others
        if "values/strings.xml" in path.replace("\\", "/"):
            new_text = "A clean, ad-free, lightweight PDF reader.\\nNo trackers. No ads."
        else:
            # For translations, if we have at least 3 parts, keep the first 3.
            # (First is the desc, second is trackers, third is ads)
            if len(clean_parts) >= 3:
                new_text = f"{clean_parts[0]}.\\n{clean_parts[1]}. {clean_parts[2]}."
            else:
                # If already damaged, we might need to be careful.
                new_text = text # fallback
        
        if new_text != text:
            new_content = content.replace(f"{prefix}{text}{suffix}", f"{prefix}{new_text}{suffix}")
            with open(path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Fixed: {path}")

def main():
    base_dir = r"c:\Users\talha\Desktop\ClearRead\app\src\main\res"
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file == "strings.xml":
                fix_description(os.path.join(root, file))

if __name__ == "__main__":
    main()
