import cairosvg
from PIL import Image
import os

svg_code = """<svg width="81" height="81" viewBox="0 0 81 81" xmlns="http://www.w3.org/2000/svg">
  <!-- White fill for the rounded rect -->
  <rect x="1.25" y="1.25" width="78.5" height="78.5" rx="17.75" fill="#FFFFFF" stroke="#000000" stroke-width="2.5"/>
  
  <!-- Soccer ball / sports score within 40x40 center area -> center at (40.5, 40.5) -->
  <g transform="translate(40.5, 40.5)" stroke="#000000" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
    <!-- Ball circle -->
    <circle cx="0" cy="0" r="18"/>
    
    <!-- Pentagon vertices -->
    <polygon points="0,-7 6.65,-2.16 4.12,5.66 -4.12,5.66 -6.65,-2.16" />
    
    <!-- Lines to edge -->
    <line x1="0" y1="-7" x2="0" y2="-18" />
    <line x1="6.65" y1="-2.16" x2="17.11" y2="-5.56" />
    <line x1="4.12" y1="5.66" x2="10.58" y2="14.56" />
    <line x1="-4.12" y1="5.66" x2="-10.58" y2="14.56" />
    <line x1="-6.65" y1="-2.16" x2="-17.11" y2="-5.56" />
  </g>
</svg>"""

with open("icon.svg", "w") as f:
    f.write(svg_code)

try:
    cairosvg.svg2png(url="icon.svg", write_to="icon.png")
except Exception as e:
    print(f"cairosvg failed: {e}")
    exit(1)

# Now resize to mipmaps
res_dir = "/Users/davidwilson/Documents/GitHub/PaperScores/app/src/main/res"
sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

img = Image.open("icon.png")
for dpi, dim in sizes.items():
    resized = img.resize((dim, dim), Image.Resampling.LANCZOS)
    d = os.path.join(res_dir, f"mipmap-{dpi}")
    os.makedirs(d, exist_ok=True)
    resized.save(os.path.join(d, "ic_launcher.png"))
    resized.save(os.path.join(d, "ic_launcher_round.png"))

print("Icons generated successfully!")
