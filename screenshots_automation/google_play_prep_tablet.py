"""
google_play_prep_tablet.py — Google Play Store image compliance tool for Ink & Echo (Tablet)
=============================================================================
Converts raw tablet layout screenshots (1080×2412 captured with density 280)
to Google Play-valid 9:16 images (1080×1920), and generates beautiful
showcase cards at the same size. Copies output to both 7-inch and 10-inch categories.

Output: 
  google_play_tablet/7_inch/{lang}/
  google_play_tablet/10_inch/{lang}/
  
  Files:
    01_guide_part1.png         ← Guide screen, section 1
    02_guide_part2.png         ← Guide screen, section 2 (scrolled)
    03_guide_part3.png         ← Guide screen, section 3 (scrolled)
    04_setup_showcase.png      ← Feature card: Setup screen
    05_writing_showcase.png    ← Feature card: Writing screen
    06_archive_showcase.png    ← Feature card: Archive screen

Google Play requirements:
  ✓ 9:16 portrait  (1080×1920)
  ✓ PNG
  ✓ sides 320–3840 px (7-inch) / 1080–7680 px (10-inch)
  ✓ < 8 MB each

Run:  python3 google_play_prep_tablet.py
"""

import os, sys, shutil
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ─── Constants ─────────────────────────────────────────────────────────────────

# Target: valid 9:16 for Google Play (meets both 7-inch and 10-inch size bounds)
OUT_W = 1080
OUT_H = 1920

STATUS_BAR = 96    # pixels at top of physical screen to crop (status bar)
NAV_BAR    = 132   # pixels at bottom to crop (nav bar)

LANGS = ["en-US", "es-ES", "fr-FR"]
SRC_DIR = "screenshots_tablet"
OUT_DIR = "google_play_tablet"

# Brand colours (match app's terracotta / dark theme)
ACCENT  = (205, 120,  75)
BG_DARK = ( 18,  18,  18)
WHITE   = (255, 255, 255)
MUTED   = (180, 180, 180)


# ─── Font helpers ──────────────────────────────────────────────────────────────

def _try_fonts(paths, size):
    for p in paths:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()

def font_mono(size, bold=False):
    return _try_fonts([
        f"/usr/share/fonts/truetype/dejavu/DejaVuSansMono{'-Bold' if bold else ''}.ttf",
        f"/usr/share/fonts/truetype/liberation/LiberationMono-{'Bold' if bold else 'Regular'}.ttf",
    ], size)

def font_sans(size, bold=False):
    return _try_fonts([
        f"/usr/share/fonts/truetype/dejavu/DejaVuSans{'-Bold' if bold else ''}.ttf",
        f"/usr/share/fonts/truetype/liberation/LiberationSans-{'Bold' if bold else 'Regular'}.ttf",
    ], size)


# ─── Drawing helpers ───────────────────────────────────────────────────────────

def center_text(draw, text, y, font, fill, width=OUT_W):
    bbox = draw.textbbox((0, 0), text, font=font)
    x = (width - (bbox[2] - bbox[0])) // 2
    draw.text((x, y), text, font=font, fill=fill)

def wrap_text_centered(draw, text, y, font, fill, max_w, line_gap=10, width=OUT_W):
    """Word-wrap and center each line. Returns total height used."""
    words = text.split()
    lines, cur = [], ""
    for w in words:
        test = f"{cur} {w}".strip()
        if draw.textbbox((0,0), test, font=font)[2] <= max_w:
            cur = test
        else:
            if cur: lines.append(cur)
            cur = w
    if cur: lines.append(cur)

    lh = draw.textbbox((0,0), "Ag", font=font)[3] + line_gap
    for i, ln in enumerate(lines):
        center_text(draw, ln, y + i*lh, font, fill, width)
    return len(lines) * lh

def accent_rule(draw, y, length=140, color=None, width=OUT_W):
    color = color or (*ACCENT, 200)
    cx = width // 2
    draw.line([(cx - length//2, y), (cx + length//2, y)], fill=color, width=2)

def dark_gradient(w, h, tint_ratio=0.22):
    img = Image.new("RGB", (w, h), BG_DARK)
    d = ImageDraw.Draw(img)
    for i in range(h):
        t = i / h
        r = int(BG_DARK[0] + (ACCENT[0] - BG_DARK[0]) * t * tint_ratio)
        g = int(BG_DARK[1] + (ACCENT[1] - BG_DARK[1]) * t * tint_ratio * 0.6)
        b = int(BG_DARK[2] + (ACCENT[2] - BG_DARK[2]) * t * tint_ratio * 0.3)
        d.line([(0,i),(w,i)], fill=(r,g,b))
    return img

def rounded_rect_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([(0,0),(size[0]-1,size[1]-1)], radius=radius, fill=255)
    return mask

def apply_shadow(img, blur=18, offset=(6,10), color=(0,0,0,160)):
    pad = blur * 2
    sw, sh = img.width + pad + abs(offset[0]), img.height + pad + abs(offset[1])
    shadow = Image.new("RGBA", (sw, sh), (0,0,0,0))
    stamp  = Image.new("RGBA", img.size, color)
    mask   = img.split()[3]
    ox, oy = pad//2 + max(0, offset[0]), pad//2 + max(0, offset[1])
    shadow.paste(stamp, (ox, oy), mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    ix, iy = pad//2 + max(0, -offset[0]), pad//2 + max(0, -offset[1])
    shadow.paste(img, (ix, iy), img)
    return shadow, (ix - ox, iy - oy)   # img offset relative to shadow origin


# ─── Core converters ───────────────────────────────────────────────────────────

def process_raw_screenshot(src_path):
    """
    Load a raw 1080×2412 screenshot (captured under density 280),
    crop status bar and nav bar, then resize the remaining 1080×2184
    to exactly 1080×1920 to output a perfect 9:16 layout.
    """
    img = Image.open(src_path).convert("RGB")
    cropped = img.crop((0, STATUS_BAR, 1080, 2412 - NAV_BAR)) # leaves 1080x2184
    resized = cropped.resize((OUT_W, OUT_H), Image.LANCZOS)
    return resized


def make_showcase_card(phone_path, headline, subtext, out_path):
    """
    Build a 1080×1920 Google Play showcase card for tablets.
    """
    W, H = OUT_W, OUT_H
    TEXT_H = 460               # Start phone screenshot higher up (was 729)
    PHONE_H = H - TEXT_H       # 1460 px (was 1191)

    # Background
    canvas = dark_gradient(W, H).convert("RGBA")
    draw   = ImageDraw.Draw(canvas)

    # Text section
    f_label = font_mono(26, bold=False)
    f_head  = font_mono(62, bold=True)
    f_sub   = font_sans(33, bold=False)

    y = 76
    center_text(draw, "INK & ECHO", y, f_label, (*ACCENT, 210))
    y += 46
    accent_rule(draw, y, color=(*ACCENT, 150))
    y += 26

    wrap_text_centered(draw, headline, y, f_head, (*WHITE, 255), max_w=W - 80)
    y += 68
    accent_rule(draw, y, color=(*ACCENT, 180), length=120)
    y += 18
    wrap_text_centered(draw, subtext, y, f_sub, (*MUTED, 220), max_w=W - 100)

    # Phone screenshot embed (already cropped and resized to 1080x1920)
    screenshot_9_16 = process_raw_screenshot(phone_path).convert("RGBA")
    
    # Scale to fit PHONE_H
    scale   = PHONE_H / screenshot_9_16.height
    pw      = int(screenshot_9_16.width * scale)
    ph      = PHONE_H
    scaled  = screenshot_9_16.resize((pw, ph), Image.LANCZOS)
    # Rounded corners
    mask    = rounded_rect_mask((pw, ph), radius=42)
    scaled.putalpha(mask)
    # Drop shadow
    shadowed, (six, siy) = apply_shadow(scaled, blur=18, offset=(6, 12))
    # Center horizontally, top at TEXT_H
    sx = (W - shadowed.width) // 2
    sy = TEXT_H - siy
    canvas.paste(shadowed, (sx, sy), shadowed)

    # Soft vertical fade between text and phone areas
    fade_h = 120
    fade   = Image.new("RGBA", (W, fade_h))
    fd     = ImageDraw.Draw(fade)
    for i in range(fade_h):
        a = int(255 * ((1 - i/fade_h) ** 1.6))
        fd.line([(0,i),(W,i)], fill=(*BG_DARK, a))
    canvas.alpha_composite(fade, (0, TEXT_H))

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    canvas.convert("RGB").save(out_path, "PNG", optimize=True)


def make_guide_card(src_path, section_label, out_path):
    """
    Crop a guide section screenshot to 9:16 and embed it in a 1080×1920 tablet card.
    """
    W, H = OUT_W, OUT_H
    canvas = dark_gradient(W, H).convert("RGBA")
    draw   = ImageDraw.Draw(canvas)

    # Tiny top label
    f_label = font_mono(26)
    center_text(draw, f"INK & ECHO  ·  {section_label}", 52, f_label, (*ACCENT, 190))
    accent_rule(draw, 96, length=100, color=(*ACCENT, 120))

    # Embed tablet screenshot
    HEADER = 120
    AVAIL  = H - HEADER

    screenshot_9_16 = process_raw_screenshot(src_path).convert("RGBA")
    scale   = AVAIL / screenshot_9_16.height
    pw      = int(screenshot_9_16.width * scale)
    ph      = AVAIL
    scaled  = screenshot_9_16.resize((pw, ph), Image.LANCZOS)

    mask = rounded_rect_mask((pw, ph), radius=36)
    scaled.putalpha(mask)
    shadowed, (six, siy) = apply_shadow(scaled, blur=14, offset=(4, 8))

    sx = (W - shadowed.width) // 2
    sy = HEADER - siy
    canvas.paste(shadowed, (sx, sy), shadowed)

    # Fade at top of phone area
    fade_h = 80
    fade   = Image.new("RGBA", (W, fade_h))
    fd     = ImageDraw.Draw(fade)
    for i in range(fade_h):
        a = int(255 * ((1 - i/fade_h) ** 2))
        fd.line([(0,i),(W,i)], fill=(*BG_DARK, a))
    canvas.alpha_composite(fade, (0, HEADER))

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    canvas.convert("RGB").save(out_path, "PNG", optimize=True)


# ─── Showcase copy per language ────────────────────────────────────────────────

SHOWCASES = {
    "en-US": [
        ("phone_screenshot_setup.png",   "SET THE STAGE",    "Choose your co-authors, set the echo length, and pick a story prompt.",   "04_setup_showcase.png"),
        ("phone_screenshot_writing.png", "WRITE YOUR PART",  "Each author sees only the last words — the echo — then continues.",        "05_writing_showcase.png"),
        ("phone_screenshot_archive.png", "PAST ECHOES",      "A library of every tale your group has woven together.",                   "06_archive_showcase.png"),
    ],
    "es-ES": [
        ("phone_screenshot_setup.png",   "PREPARA EL JUEGO", "Elige coautores, ajusta la longitud del eco y escoge un tema.",           "04_setup_showcase.png"),
        ("phone_screenshot_writing.png", "ESCRIBE TU PARTE", "Cada autor solo ve las últimas palabras — el eco — y continúa la historia.", "05_writing_showcase.png"),
        ("phone_screenshot_archive.png", "ECOS PASADOS",     "Una biblioteca de todos los relatos que tu grupo ha tejido.",              "06_archive_showcase.png"),
    ],
    "fr-FR": [
        ("phone_screenshot_setup.png",   "PRÉPAREZ LE JEU",    "Choisissez vos coauteurs, réglez l'écho et choisissez une accroche.",  "04_setup_showcase.png"),
        ("phone_screenshot_writing.png", "ÉCRIVEZ VOTRE PART", "Chaque auteur ne voit que les derniers mots — l'écho — et continue.", "05_writing_showcase.png"),
        ("phone_screenshot_archive.png", "ÉCHOS PASSÉS",       "Une bibliothèque de tous les récits que votre groupe a tissés.",       "06_archive_showcase.png"),
    ],
}

GUIDE_LABELS = {
    "en-US": ["HOW TO PLAY · 1/3", "HOW TO PLAY · 2/3", "HOW TO PLAY · 3/3"],
    "es-ES": ["CÓMO JUGAR · 1/3",  "CÓMO JUGAR · 2/3",  "CÓMO JUGAR · 3/3"],
    "fr-FR": ["COMMENT JOUER · 1/3","COMMENT JOUER · 2/3","COMMENT JOUER · 3/3"],
}


# ─── Main ──────────────────────────────────────────────────────────────────────

def process_lang(lang):
    src = f"{SRC_DIR}/{lang}"
    out_base = f"{OUT_DIR}/base/{lang}"
    print(f"\n{'─'*54}")
    print(f"  Processing {lang} (Tablet)...")
    print(f"{'─'*54}")

    # Create directories
    os.makedirs(out_base, exist_ok=True)

    # 1. Guide sections (9:16 cards)
    for i, label in enumerate(GUIDE_LABELS[lang], 1):
        guide_src = f"{src}/guide_section_{i}.png"
        guide_out = f"{out_base}/0{i}_guide_part{i}.png"

        if not os.path.exists(guide_src):
            if i == 1 and os.path.exists(f"{src}/phone_screenshot_guide.png"):
                guide_src = f"{src}/phone_screenshot_guide.png"
            else:
                print(f"  ⚠  {guide_src} not found — skipping section {i}")
                continue

        make_guide_card(guide_src, label, guide_out)

    # 2. Showcase cards (9:16)
    for phone_file, headline, subtext, out_file in SHOWCASES[lang]:
        phone_path = f"{src}/{phone_file}"
        out_path   = f"{out_base}/{out_file}"
        if not os.path.exists(phone_path):
            print(f"  ⚠  {phone_path} not found — skipping")
            continue
        make_showcase_card(phone_path, headline, subtext, out_path)

    # Copy files to 7_inch and 10_inch folders
    out_7 = f"{OUT_DIR}/7_inch/{lang}"
    out_10 = f"{OUT_DIR}/10_inch/{lang}"
    os.makedirs(out_7, exist_ok=True)
    os.makedirs(out_10, exist_ok=True)

    for item in os.listdir(out_base):
        src_item = os.path.join(out_base, item)
        if os.path.isfile(src_item):
            shutil.copy(src_item, os.path.join(out_7, item))
            shutil.copy(src_item, os.path.join(out_10, item))
    print(f"  ✓ Copied processed {lang} assets to 7_inch and 10_inch folders.")


def main():
    print("=" * 54)
    print("  Ink & Echo — Google Play Tablet Image Prep")
    print("  Target: 1080×1920, 9:16, PNG, Duplicated to 7_inch & 10_inch")
    print("=" * 54)

    for lang in LANGS:
        process_lang(lang)

    print("\n" + "=" * 54)
    print("  SUMMARY")
    print("=" * 54)
    for size_cat in ["7_inch", "10_inch"]:
        print(f"\n  Category: {size_cat}")
        for lang in LANGS:
            out = f"{OUT_DIR}/{size_cat}/{lang}"
            if not os.path.exists(out):
                continue
            files = sorted(f for f in os.listdir(out) if f.endswith(".png"))
            print(f"    ✓ {lang}: {len(files)} images -> {out}/")
            for f in files:
                kb = os.path.getsize(f"{out}/{f}") // 1024
                print(f"         {f} ({kb} KB)")
    print("\n✅ All done! All tablet images are 1080×1920 (9:16) PNG.")


if __name__ == "__main__":
    main()
