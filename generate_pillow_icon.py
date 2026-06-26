from PIL import Image, ImageDraw
import os
import math

def draw_icon(dim):
    # Base scale: 81 is the virtual size. We scale to `dim`.
    scale = dim / 81.0
    img = Image.new("RGBA", (dim, dim), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    
    sw = int(max(1, round(2.5 * scale)))
    
    # Outer rectangle
    r_outer = 19 * scale
    x0, y0 = 1.25 * scale, 1.25 * scale
    x1, y1 = 79.75 * scale, 79.75 * scale
    
    draw.rounded_rectangle([x0, y0, x1, y1], radius=r_outer, fill="white", outline="black", width=sw)
    
    # Inner soccer ball at 40.5 * scale
    cx, cy = 40.5 * scale, 40.5 * scale
    
    # Circle radius 18
    r_ball = 18 * scale
    draw.ellipse([cx - r_ball, cy - r_ball, cx + r_ball, cy + r_ball], fill=None, outline="black", width=sw)
    
    # Pentagon vertices
    pts = [
        (0, -7), (6.65, -2.16), (4.12, 5.66), (-4.12, 5.66), (-6.65, -2.16)
    ]
    scaled_pts = [(cx + px * scale, cy + py * scale) for px, py in pts]
    draw.polygon(scaled_pts, fill=None, outline="black", width=sw)
    
    # Lines
    lines = [
        ((0, -7), (0, -18)),
        ((6.65, -2.16), (17.11, -5.56)),
        ((4.12, 5.66), (10.58, 14.56)),
        ((-4.12, 5.66), (-10.58, 14.56)),
        ((-6.65, -2.16), (-17.11, -5.56))
    ]
    for p1, p2 in lines:
        draw.line([(cx + p1[0]*scale, cy + p1[1]*scale), (cx + p2[0]*scale, cy + p2[1]*scale)], fill="black", width=sw)
        
    return img

res_dir = "/Users/davidwilson/Documents/GitHub/PaperScores/app/src/main/res"
sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

for dpi, dim in sizes.items():
    img = draw_icon(dim)
    d = os.path.join(res_dir, f"mipmap-{dpi}")
    os.makedirs(d, exist_ok=True)
    img.save(os.path.join(d, "ic_launcher.png"))
    img.save(os.path.join(d, "ic_launcher_round.png"))
    print(f"Saved {dpi}")

# Also generate the Vector XML for modern Android
vector_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="81"
    android:viewportHeight="81">
    
    <!-- White fill for the rounded rect -->
    <path
        android:pathData="M1.25,19 A17.75,17.75 0 0 1 19,1.25 L62,1.25 A17.75,17.75 0 0 1 79.75,19 L79.75,62 A17.75,17.75 0 0 1 62,79.75 L19,79.75 A17.75,17.75 0 0 1 1.25,62 Z"
        android:fillColor="#FFFFFF"
        android:strokeColor="#000000"
        android:strokeWidth="2.5" />

    <!-- Group for soccer ball centered at 40.5, 40.5 -->
    <group
        android:translateX="40.5"
        android:translateY="40.5">
        
        <!-- Ball circle -->
        <path
            android:pathData="M 0,-18 A 18,18 0 1 1 0,18 A 18,18 0 1 1 0,-18"
            android:strokeColor="#000000"
            android:strokeWidth="2.5"
            android:strokeLineCap="round"
            android:strokeLineJoin="round"
            android:fillColor="#00000000" />
            
        <!-- Pentagon and lines -->
        <path
            android:pathData="M 0,-7 L 6.65,-2.16 L 4.12,5.66 L -4.12,5.66 L -6.65,-2.16 Z M 0,-7 L 0,-18 M 6.65,-2.16 L 17.11,-5.56 M 4.12,5.66 L 10.58,14.56 M -4.12,5.66 L -10.58,14.56 M -6.65,-2.16 L -17.11,-5.56"
            android:strokeColor="#000000"
            android:strokeWidth="2.5"
            android:strokeLineCap="round"
            android:strokeLineJoin="round"
            android:fillColor="#00000000" />
    </group>
</vector>
"""

os.makedirs(os.path.join(res_dir, "mipmap-anydpi-v26"), exist_ok=True)
with open(os.path.join(res_dir, "mipmap-anydpi-v26", "ic_launcher.xml"), "w") as f:
    f.write(vector_xml)
with open(os.path.join(res_dir, "mipmap-anydpi-v26", "ic_launcher_round.xml"), "w") as f:
    f.write(vector_xml)
print("Saved XMLs")
