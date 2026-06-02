"""
Ink & Echo — Screenshot automation + Google Play feature showcase generator.

Captures:
  1. Guide screen — all three scrolled sections (top / mid / bottom)
  2. Setup, Writing, Archive phone screenshots
  3. Beautiful "feature showcase" composite images for Google Play Store

Run:  python3 screenshot_maker.py
"""

import xml.etree.ElementTree as ET
import subprocess, time, re, os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ─── ADB helpers ──────────────────────────────────────────────────────────────

def run_cmd(cmd):
    print(f"  → {cmd}")
    r = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE, text=True)
    if r.returncode != 0:
        print(f"    ⚠ stderr: {r.stderr.strip()}")
    return r.stdout.strip()

def dump_ui(filename="temp_dump.xml"):
    if os.path.exists(filename):
        os.remove(filename)
    run_cmd("adb shell uiautomator dump /data/local/tmp/uidump.xml")
    run_cmd(f"adb pull /data/local/tmp/uidump.xml {filename}")
    return filename

def find_node_by_text(root, query):
    q = query.lower()
    for node in root.iter('node'):
        if q in node.get('text', '').lower() or q in node.get('content-desc', '').lower():
            return node
    return None

def node_center(node):
    m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', node.get('bounds', ''))
    if m:
        l, t, r, b = map(int, m.groups())
        return (l + r) // 2, (t + b) // 2
    return None

def scroll_down():
    run_cmd("adb shell input swipe 540 1800 540 600 400")
    time.sleep(1.2)

def scroll_up():
    run_cmd("adb shell input swipe 540 600 540 1800 400")
    time.sleep(1.2)

def scroll_to_top():
    for _ in range(5):
        scroll_up()

def dismiss_keyboard():
    if "mInputShown=true" in run_cmd("adb shell dumpsys input_method"):
        run_cmd("adb shell input keyevent 4")
        time.sleep(1.2)

def tap_text(query, allow_scroll=True):
    for attempt in range(4):
        tree = ET.parse(dump_ui())
        node = find_node_by_text(tree.getroot(), query)
        if node is not None:
            c = node_center(node)
            if c:
                run_cmd(f"adb shell input tap {c[0]} {c[1]}")
                time.sleep(1.5)
                return True
        if allow_scroll and attempt < 3:
            print(f"    '{query}' not visible — scrolling…")
            scroll_down()
        else:
            break
    print(f"    ⚠ could not find '{query}'")
    return False

def type_text(text):
    escaped = text.replace(" ", "%s").replace("'", "\\'").replace('"', '\\"')
    run_cmd(f"adb shell input text {escaped}")
    time.sleep(1.5)
    dismiss_keyboard()

def screencap(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    run_cmd(f"adb shell screencap -p > {path}")
    print(f"    📸 {path}")
    time.sleep(0.8)


# ─── Guide — full scroll capture ──────────────────────────────────────────────

def capture_guide_all_sections(lang_tag):
    """Capture guide screen at top / mid / bottom scroll positions."""
    print("  Capturing guide sections…")
    scroll_to_top()
    time.sleep(0.5)
    screencap(f"screenshots/{lang_tag}/guide_section_1.png")
    scroll_down()
    screencap(f"screenshots/{lang_tag}/guide_section_2.png")
    scroll_down()
    screencap(f"screenshots/{lang_tag}/guide_section_3.png")


# ─── Main game flow ───────────────────────────────────────────────────────────

def run_flow_for_lang(lang_tag, cfg):
    print(f"\n{'='*54}")
    print(f"  LANGUAGE: {lang_tag}")
    print(f"{'='*54}")

    run_cmd(f"adb shell cmd locale set-app-locales com.LakesCorp.FunCoStory --locales {lang_tag}")
    time.sleep(1)
    run_cmd("adb shell am force-stop com.LakesCorp.FunCoStory")
    time.sleep(1)
    run_cmd("adb shell am start -n com.LakesCorp.FunCoStory/.MainActivity")
    print("  Waiting for app launch…")
    time.sleep(4)

    # ── GUIDE — capture all scroll positions
    screencap(f"screenshots/{lang_tag}/phone_screenshot_guide.png")
    capture_guide_all_sections(lang_tag)

    # ── Navigate to Setup
    if not tap_text(cfg['btn_start_session']):
        run_cmd("adb shell input tap 540 2000")
        time.sleep(2)
    time.sleep(2)

    scroll_to_top()

    tap_text("Decrease", allow_scroll=False)
    tap_text("Decrease", allow_scroll=False)

    if tap_text(cfg['suggest_prompt']):
        time.sleep(1)
        tap_text(cfg['surprise_me'], allow_scroll=False)
        time.sleep(1.5)

    # ── SETUP screenshot
    screencap(f"screenshots/{lang_tag}/phone_screenshot_setup.png")

    # ── Start writing (Turn 1)
    tap_text(cfg['btn_start_writing'])
    time.sleep(2.5)
    tap_text(cfg['thread_placeholder'])
    type_text(cfg['type_text_1'])

    # ── WRITING screenshot
    screencap(f"screenshots/{lang_tag}/phone_screenshot_writing.png")

    tap_text(cfg['btn_seal_scroll'])
    time.sleep(2)
    tap_text(cfg['btn_ready'], allow_scroll=False)
    time.sleep(2)

    # Turn 2
    tap_text(cfg['thread_placeholder'])
    type_text(cfg['type_text_2'])
    tap_text(cfg['btn_seal_end'])
    time.sleep(3)

    # ── ARCHIVE screenshot
    screencap(f"screenshots/{lang_tag}/phone_screenshot_archive.png")

    print(f"  ✓ Done with {lang_tag}")


# ─── Feature showcase compositor ─────────────────────────────────────────────

ACCENT   = (205, 120,  75)   # warm terracotta (matches app primary)
BG_DARK  = ( 18,  18,  18)
WHITE    = (255, 255, 255)
MUTED    = (180, 180, 180)

CANVAS_W = 1080
CANVAS_H = 2412

def load_font(size, bold=False):
    """Try system mono/serif fonts that match the app's typographic feel."""
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf" if bold else
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationMono-Bold.ttf" if bold else
        "/usr/share/fonts/truetype/liberation/LiberationMono-Regular.ttf",
        "/usr/share/fonts/truetype/freefont/FreeMonoBold.ttf" if bold else
        "/usr/share/fonts/truetype/freefont/FreeMono.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()

def load_sans(size, bold=False):
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf" if bold else
        "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()

def draw_text_centered(draw, text, y, font, color, width=CANVAS_W):
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    draw.text(((width - tw) // 2, y), text, font=font, fill=color)
    return bbox[3] - bbox[1]   # height of text

def draw_wrapped_text_centered(draw, text, y, font, color, max_width, width=CANVAS_W):
    """Word-wrap text and draw each line centered. Returns total height."""
    words = text.split()
    lines, line = [], ""
    for w in words:
        test = f"{line} {w}".strip()
        bbox = draw.textbbox((0, 0), test, font=font)
        if bbox[2] - bbox[0] <= max_width:
            line = test
        else:
            if line:
                lines.append(line)
            line = w
    if line:
        lines.append(line)

    line_h = draw.textbbox((0, 0), "Ag", font=font)[3] + 8
    total_h = len(lines) * line_h
    for i, ln in enumerate(lines):
        bbox = draw.textbbox((0, 0), ln, font=font)
        tw = bbox[2] - bbox[0]
        draw.text(((width - tw) // 2, y + i * line_h), ln, font=font, fill=color)
    return total_h

def add_decorative_rule(draw, y, color=ACCENT, width=CANVAS_W, length=160):
    cx = width // 2
    draw.line([(cx - length//2, y), (cx + length//2, y)], fill=color, width=2)

STATUS_BAR_PX = 110   # pixels to crop from phone top (status bar)
CORNER_RADIUS = 48   # rounded corner radius for phone embed

def round_corners(img, radius):
    """Apply rounded corners mask to an RGBA image."""
    mask = Image.new("L", img.size, 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([(0, 0), (img.width - 1, img.height - 1)],
                         radius=radius, fill=255)
    out = img.copy()
    out.putalpha(mask)
    return out

def drop_shadow(img, offset=(12, 16), blur=22, shadow_color=(0, 0, 0, 180)):
    """Render a blurred drop shadow beneath an RGBA image."""
    sw, sh = img.width + abs(offset[0]) + blur*2, img.height + abs(offset[1]) + blur*2
    shadow = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    # Stamp shadow shape
    shadow_stamp = Image.new("RGBA", img.size, shadow_color)
    mask = img.split()[3]   # alpha channel as mask
    sx = blur + max(0,  offset[0])
    sy = blur + max(0,  offset[1])
    shadow.paste(shadow_stamp, (sx, sy), mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    # Composite image over shadow
    ix = blur + max(0, -offset[0])
    iy = blur + max(0, -offset[1])
    shadow.paste(img, (ix, iy), img)
    return shadow, (sx - ix, sy - iy)  # also return image offset within result

def build_feature_card(phone_img_path, headline, subtext, output_path, label_top=None):
    """
    Compose a beautiful Google Play feature card:
      - Gradient dark background
      - Phone screenshot (status bar cropped, rounded corners, drop shadow)
      - Headline + subtext on top
      - Decorative accent rule
    """
    W, H = CANVAS_W, CANVAS_H

    # ── Background: deep dark gradient
    grad = Image.new("RGB", (W, H), BG_DARK)
    draw_g = ImageDraw.Draw(grad)
    for i in range(H):
        ratio = i / H
        r = int(BG_DARK[0] + (ACCENT[0] - BG_DARK[0]) * ratio * 0.20)
        g = int(BG_DARK[1] + (ACCENT[1] - BG_DARK[1]) * ratio * 0.12)
        b = int(BG_DARK[2] + (ACCENT[2] - BG_DARK[2]) * ratio * 0.06)
        draw_g.line([(0, i), (W, i)], fill=(r, g, b))
    canvas = grad.convert("RGBA")
    draw = ImageDraw.Draw(canvas)

    # Fonts
    f_label = load_font(28, bold=False)
    f_head  = load_font(72, bold=True)
    f_sub   = load_sans(38, bold=False)

    TEXT_AREA_H = 480
    PHONE_Y     = TEXT_AREA_H
    PHONE_H     = H - PHONE_Y

    # ── Top label
    y_cursor = 88
    if label_top:
        draw_text_centered(draw, label_top, y_cursor, f_label, (*ACCENT, 220))
        y_cursor += 50
        add_decorative_rule(draw, y_cursor, color=(*ACCENT, 160))
        y_cursor += 28

    # ── Headline
    draw_text_centered(draw, headline, y_cursor, f_head, (*WHITE, 255))
    y_cursor += 88

    # ── Rule
    add_decorative_rule(draw, y_cursor, color=(*ACCENT, 200))
    y_cursor += 24

    # ── Subtext
    draw_wrapped_text_centered(draw, subtext, y_cursor, f_sub, (*MUTED, 220),
                               max_width=W - 100)

    # ── Phone screenshot
    phone_raw = Image.open(phone_img_path).convert("RGBA")
    # Crop status bar
    phone_cropped = phone_raw.crop((0, STATUS_BAR_PX, phone_raw.width, phone_raw.height))
    # Scale to fit PHONE_H
    ph, pw = phone_cropped.height, phone_cropped.width
    scale = PHONE_H / ph
    new_pw = int(pw * scale)
    new_ph = PHONE_H
    phone_scaled = phone_cropped.resize((new_pw, new_ph), Image.LANCZOS)
    # Rounded corners
    phone_rounded = round_corners(phone_scaled, CORNER_RADIUS)
    # Drop shadow
    phone_shadowed, shadow_offset = drop_shadow(phone_rounded, offset=(8, 14), blur=20)
    # Center-paste with shadow
    px = (W - phone_shadowed.width) // 2
    py = PHONE_Y - shadow_offset[1]   # align image top to PHONE_Y
    canvas.paste(phone_shadowed, (px, py), phone_shadowed)

    # Soft top-fade over phone area
    fade_h = 160
    fade = Image.new("RGBA", (W, fade_h), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fade)
    for i in range(fade_h):
        a = int(255 * (1 - i / fade_h) ** 1.4)
        fd.line([(0, i), (W, i)], fill=(*BG_DARK, a))
    canvas.alpha_composite(fade, (0, PHONE_Y))

    canvas.convert("RGB").save(output_path, "PNG", optimize=True)
    print(f"  🖼  Feature card → {output_path}")


# ─── Showcase definitions ─────────────────────────────────────────────────────

SHOWCASES = {
    "en-US": [
        {
            "screen":   "phone_screenshot_setup.png",
            "headline": "SET THE STAGE",
            "subtext":  "Choose your co-authors, set the echo length, and pick a story prompt — then let the ink flow.",
            "output":   "showcase_setup.png",
        },
        {
            "screen":   "phone_screenshot_writing.png",
            "headline": "WRITE YOUR PART",
            "subtext":  "Each author sees only the last few words — the echo — and continues the story from there.",
            "output":   "showcase_writing.png",
        },
        {
            "screen":   "phone_screenshot_archive.png",
            "headline": "PAST ECHOES",
            "subtext":  "A library of every tale your group has woven together. Read, revisit, marvel.",
            "output":   "showcase_archive.png",
        },
    ],
    "es-ES": [
        {
            "screen":   "phone_screenshot_setup.png",
            "headline": "PREPARA EL JUEGO",
            "subtext":  "Elige los coautores, ajusta la longitud del eco y selecciona un tema antes de escribir.",
            "output":   "showcase_setup.png",
        },
        {
            "screen":   "phone_screenshot_writing.png",
            "headline": "ESCRIBE TU PARTE",
            "subtext":  "Cada autor solo ve las últimas palabras — el eco — y continúa la historia desde ahí.",
            "output":   "showcase_writing.png",
        },
        {
            "screen":   "phone_screenshot_archive.png",
            "headline": "ECOS PASADOS",
            "subtext":  "Una biblioteca de todos los relatos que tu grupo ha tejido juntos. Lee, revive, disfruta.",
            "output":   "showcase_archive.png",
        },
    ],
    "fr-FR": [
        {
            "screen":   "phone_screenshot_setup.png",
            "headline": "PRÉPAREZ LE JEU",
            "subtext":  "Choisissez vos coauteurs, réglez la longueur de l'écho et choisissez une accroche.",
            "output":   "showcase_setup.png",
        },
        {
            "screen":   "phone_screenshot_writing.png",
            "headline": "ÉCRIVEZ VOTRE PART",
            "subtext":  "Chaque auteur ne voit que les derniers mots — l'écho — et poursuit l'histoire à partir de là.",
            "output":   "showcase_writing.png",
        },
        {
            "screen":   "phone_screenshot_archive.png",
            "headline": "ÉCHOS PASSÉS",
            "subtext":  "Une bibliothèque de tous les récits que votre groupe a tissés ensemble. Lisez et savourez.",
            "output":   "showcase_archive.png",
        },
    ],
}

LABEL = "INK & ECHO"


def generate_showcases():
    print("\n=== Generating Google Play feature showcases ===")
    for lang, items in SHOWCASES.items():
        for item in items:
            src = f"screenshots/{lang}/{item['screen']}"
            out = f"screenshots/{lang}/{item['output']}"
            if not os.path.exists(src):
                print(f"  ⚠ missing source: {src} — skipping")
                continue
            build_feature_card(
                phone_img_path=src,
                headline=item['headline'],
                subtext=item['subtext'],
                output_path=out,
                label_top=LABEL,
            )


# ─── Main ─────────────────────────────────────────────────────────────────────

CONFIGS = {
    "en-US": {
        "btn_start_session":  "START A SESSION",
        "btn_start_writing":  "START WRITING",
        "suggest_prompt":     "SUGGEST PROMPT",
        "surprise_me":        "SURPRISE ME",
        "thread_placeholder": "Continue the thread here",
        "type_text_1":        "The old lighthouse keeper found a bottle with a map inside.",
        "btn_seal_scroll":    "SEAL THE SCROLL",
        "btn_ready":          "I AM READY",
        "type_text_2":        "The map led to an island that appeared only at midnight.",
        "btn_seal_end":       "SEAL THE SCROLL & END",
    },
    "es-ES": {
        "btn_start_session":  "COMENZAR UNA SESIÓN",
        "btn_start_writing":  "COMENZAR A ESCRIBIR",
        "suggest_prompt":     "SUGERIR TEMA",
        "surprise_me":        "SORPRÉNDEME",
        "thread_placeholder": "tinta",
        "type_text_1":        "El viejo faro guardaba un secreto que nadie conocia.",
        "btn_seal_scroll":    "SELLAR EL PERGAMINO",
        "btn_ready":          "ESTOY LISTO",
        "type_text_2":        "El mapa llevaba a una isla que solo aparecia a medianoche.",
        "btn_seal_end":       "SELLAR Y TERMINAR",
    },
    "fr-FR": {
        "btn_start_session":  "COMMENCER UNE SESSION",
        "btn_start_writing":  "COMMENCER À ÉCRIRE",
        "suggest_prompt":     "PROPOSER ACCROCHE",
        "surprise_me":        "SURPRENDS-MOI",
        "thread_placeholder": "Laissez couler",
        "type_text_1":        "Le vieux gardien du phare trouva une bouteille avec une carte.",
        "btn_seal_scroll":    "SCELLER LE PARCHEMIN",
        "btn_ready":          "JE SUIS PRÊT",
        "type_text_2":        "La carte menait a une ile qui n apparaissait qu a minuit.",
        "btn_seal_end":       "SCELLER & TERMINER",
    },
}


def main():
    run_cmd("adb shell input keyevent KEYCODE_WAKEUP")
    run_cmd("adb shell input keyevent 82")

    for lang, cfg in CONFIGS.items():
        run_flow_for_lang(lang, cfg)

    generate_showcases()
    print("\n✅ All done! Check the screenshots/ folder.")


if __name__ == "__main__":
    main()
