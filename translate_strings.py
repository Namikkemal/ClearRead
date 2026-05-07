import os
import xml.etree.ElementTree as ET
from xml.dom import minidom
from googletrans import Translator
import time

# List of target languages (as requested: es, fr, de, pt, ru, zh, ja, ko, ar, hi, it, in, pl, nl, tr)
languages = {
    "es": "es", "fr": "fr", "de": "de", "pt": "pt", "ru": "ru",
    "zh": "zh-cn", "ja": "ja", "ko": "ko", "ar": "ar", "hi": "hi",
    "it": "it", "in": "id", "pl": "pl", "nl": "nl", "tr": "tr"
}

# The hardcoded strings to translate
english_strings = {
    "settings_title": "Settings",
    "settings_appearance": "Appearance",
    "settings_pure_black": "Pure black (AMOLED)",
    "settings_pure_black_desc": "Saves battery on AMOLED screens",
    "settings_reading": "Reading",
    "settings_scroll_direction": "Default scroll direction",
    "settings_scroll_vertical": "Vertical (continuous)",
    "settings_scroll_horizontal": "Horizontal (page-flip)",
    "settings_keep_screen_on": "Keep screen on while reading",
    "settings_enabled": "Enabled",
    "settings_disabled": "Disabled",
    "settings_data": "Data",
    "settings_clear_recent": "Clear recent files",
    "settings_clear_recent_desc": "Remove all recent file history",
    "settings_cleared": "Cleared!",
    "settings_about": "About",
    "settings_about_desc": "A clean, ad-free, lightweight PDF reader.\\nNo trackers. No ads.",
    "settings_about_badges": "Zero ads · Zero trackers · Fully offline",
    "settings_dialog_clear_title": "Clear recent files?",
    "settings_dialog_clear_desc": "This will remove all recent file history. Your bookmarks will not be affected.",
    "settings_dialog_clear": "Clear",
    "settings_dialog_cancel": "Cancel",
    "home_search": "Search in library...",
    "home_bookmarks": "Bookmarks",
    "home_view_all": "View All",
    "home_recent_files": "Recent Files",
    "home_no_results": "No results for \\\"%s\\\"",
    "home_check_spelling": "Check the spelling or try a different name",
    "home_no_files": "No files yet",
    "home_no_files_desc": "Open a PDF to start reading.\\nYour recent files will appear here.",
    "home_open_pdf": "Open a PDF",
    "home_just_now": "Just now",
    "home_min_ago": "%d min ago",
    "home_h_ago": "%d h ago",
    "home_yesterday": "Yesterday",
    "home_days_ago": "%d days ago",
    "explorer_title": "File Explorer",
    "explorer_choose_folder": "Choose Folder",
    "explorer_browse": "Browse your files",
    "explorer_browse_desc": "Choose a folder to find PDF files.",
    "explorer_continue_in": "Continue in",
    "explorer_loading": "Loading files...",
    "bookmarks_empty": "No bookmarks yet",
    "bookmarks_empty_desc": "Bookmark pages while reading to find them here.",
    "bookmarks_page": "Page %d",
    "import_large_title": "Large File Detected",
    "import_large_desc": "Android limits how we access files from other apps. To keep this large file in your library, we need to create a local copy (%s).",
    "import_large_tip": "Tip: You can bypass this fuss by selecting files from ClearRead\\'s built-in file picker!",
    "import_large_btn_copy": "Copy & Proceed",
    "import_large_btn_temp": "Open Temporarily",
    "reader_error_unknown": "Unknown error",
    "reader_error_back": "Tap back to return",
    "reader_mode_normal": "Normal",
    "reader_mode_dark": "Dark",
    "reader_mode_sepia": "Sepia",
    "acc_back": "Back",
    "acc_pdf_page": "PDF page",
    "acc_explorer": "File Explorer",
    "acc_settings": "Settings",
    "acc_clear_search": "Clear search",
    "acc_remove_recent": "Remove from recent",
    "acc_delete_bookmark": "Delete bookmark",
}

def prettify(elem):
    rough_string = ET.tostring(elem, 'utf-8')
    reparsed = minidom.parseString(rough_string)
    # The prettify function adds extra newlines, so we replace them
    xml_str = reparsed.toprettyxml(indent="    ")
    # remove empty lines
    return "\n".join([line for line in xml_str.split("\n") if line.strip()])

def update_xml(file_path, translations):
    if os.path.exists(file_path):
        tree = ET.parse(file_path)
        root = tree.getroot()
    else:
        root = ET.Element("resources")
        tree = ET.ElementTree(root)
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
    
    # Check existing keys
    existing_keys = [child.attrib.get("name") for child in root if "name" in child.attrib]
    
    for key, value in translations.items():
        if key not in existing_keys:
            string_elem = ET.SubElement(root, "string", name=key)
            string_elem.text = value
        else:
            # Update existing key if it's different (optional, but good for base values)
            for child in root:
                if child.attrib.get("name") == key:
                    child.text = value
                    break
    
    # Write to file
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(prettify(root))

def main():
    translator = Translator()
    base_res_dir = r"c:\Users\talha\Desktop\ClearRead\app\src\main\res"
    
    print("Updating English values...")
    en_file = os.path.join(base_res_dir, "values", "strings.xml")
    update_xml(en_file, english_strings)
    
    for lang_code, trans_code in languages.items():
        print(f"Processing language: {lang_code}...")
        
        file_path = os.path.join(base_res_dir, f"values-{lang_code}", "strings.xml")
        
        # Check what we already have in the target file
        existing_keys = []
        if os.path.exists(file_path):
            try:
                tree = ET.parse(file_path)
                existing_keys = [child.attrib.get("name") for child in tree.getroot() if "name" in child.attrib]
            except:
                pass
                
        translations_for_lang = {}
        for key, text in english_strings.items():
            if key != "settings_about_desc":
                continue
                
            try:
                # To maintain formatting placeholders like %s or %d we should be careful, but googletrans usually keeps them intact.
                # However, for \\n, it might strip it.
                to_translate = text.replace("\\n", "\n").replace("\\\"", "\"").replace("\\'", "'")
                translated = translator.translate(to_translate, src='en', dest=trans_code).text
                
                # Restore escape characters
                translated = translated.replace("\n", "\\n").replace("\"", "\\\"").replace("'", "\\'")
                translations_for_lang[key] = translated
                
                print(f"  Translated {key}")
                # Respect rate limits
                time.sleep(0.5)
            except Exception as e:
                print(f"Failed to translate {key} for {lang_code}: {e}")
                translations_for_lang[key] = text
        
        if translations_for_lang:
            update_xml(file_path, translations_for_lang)
            print(f"Updated {len(translations_for_lang)} strings for {lang_code}.")
        else:
            print(f"All strings up to date for {lang_code}.")

if __name__ == '__main__':
    main()
