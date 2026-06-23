import os
import glob

old_pkg = "com.example.soccerscores"
new_pkg = "com.paperapps.paperscores"

def replace_in_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        return
        
    if old_pkg in content:
        new_content = content.replace(old_pkg, new_pkg)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

for root, dirs, files in os.walk('.'):
    # skip .git, build, etc
    if '.git' in root or '/build/' in root or '.gradle' in root:
        continue
    for file in files:
        if file.endswith('.kt') or file.endswith('.xml') or file.endswith('.kts') or file.endswith('.pro'):
            replace_in_file(os.path.join(root, file))

print("Done replacing.")
