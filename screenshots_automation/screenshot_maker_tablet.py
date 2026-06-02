"""
Ink & Echo — Tablet Screenshot automation.
=============================================================================
Runs the app UI automation flow on a connected device, simulating a tablet
by setting the display density to 280. Captures raw physical screenshots (1080x2412)
and saves them to screenshots_tablet/{lang}/.
At the end, calls google_play_prep_tablet.py to compile the final Play Store assets.

Run:  python3 screenshot_maker_tablet.py
"""

import xml.etree.ElementTree as ET
import subprocess, time, re, os, shutil, sys

# ─── ADB helpers ──────────────────────────────────────────────────────────────

def run_cmd(cmd):
    print(f"  → {cmd}")
    r = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE, text=True)
    if r.returncode != 0:
        print(f"    ⚠ stderr: {r.stderr.strip()}")
    return r.stdout.strip()

def get_screen_size():
    out = run_cmd("adb shell wm size")
    m = re.search(r'Override size: (\d+)x(\d+)', out)
    if not m:
        m = re.search(r'Physical size: (\d+)x(\d+)', out)
    if m:
        return int(m.group(1)), int(m.group(2))
    return 1080, 2412  # physical default fallback

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
    w, h = get_screen_size()
    cx = w // 2
    y_start = int(h * 0.75)
    y_end = int(h * 0.25)
    run_cmd(f"adb shell input swipe {cx} {y_start} {cx} {y_end} 400")
    time.sleep(1.2)

def scroll_up():
    w, h = get_screen_size()
    cx = w // 2
    y_start = int(h * 0.25)
    y_end = int(h * 0.75)
    run_cmd(f"adb shell input swipe {cx} {y_start} {cx} {y_end} 400")
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
    screencap(f"screenshots_tablet/{lang_tag}/guide_section_1.png")
    scroll_down()
    screencap(f"screenshots_tablet/{lang_tag}/guide_section_2.png")
    scroll_down()
    screencap(f"screenshots_tablet/{lang_tag}/guide_section_3.png")


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
    screencap(f"screenshots_tablet/{lang_tag}/phone_screenshot_guide.png")
    capture_guide_all_sections(lang_tag)

    # ── Navigate to Setup
    if not tap_text(cfg['btn_start_session']):
        w, h = get_screen_size()
        run_cmd(f"adb shell input tap {w // 2} {int(h * 0.85)}")
        time.sleep(2)
    time.sleep(2)

    scroll_to_top()

    # Configure game rules for tablet: decrease writers to 1 round/etc.
    tap_text("Decrease", allow_scroll=False)
    tap_text("Decrease", allow_scroll=False)

    if tap_text(cfg['suggest_prompt']):
        time.sleep(1)
        tap_text(cfg['surprise_me'], allow_scroll=False)
        time.sleep(1.5)

    # ── SETUP screenshot
    screencap(f"screenshots_tablet/{lang_tag}/phone_screenshot_setup.png")

    # ── Start writing (Turn 1)
    tap_text(cfg['btn_start_writing'])
    time.sleep(2.5)
    tap_text(cfg['thread_placeholder'])
    type_text(cfg['type_text_1'])

    # ── WRITING screenshot
    screencap(f"screenshots_tablet/{lang_tag}/phone_screenshot_writing.png")

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
    screencap(f"screenshots_tablet/{lang_tag}/phone_screenshot_archive.png")

    print(f"  ✓ Done with {lang_tag}")


# ─── Configuration per language ──────────────────────────────────────────────

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
        "surprise_me":        "SURPRÉNDEME",
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


# ─── Main ─────────────────────────────────────────────────────────────────────

def main():
    print("Setting screen density for tablet layout...")
    run_cmd("adb shell wm density 280")
    time.sleep(2.5)

    try:
        run_cmd("adb shell input keyevent KEYCODE_WAKEUP")
        run_cmd("adb shell input keyevent 82")

        for lang, cfg in CONFIGS.items():
            run_flow_for_lang(lang, cfg)

        print("\nRunning google_play_prep_tablet.py to crop and build Play Store assets...")
        subprocess.run([sys.executable, "google_play_prep_tablet.py"], check=True)
        print("\n✅ All done! Check the google_play_tablet/ folder.")
    finally:
        print("Restoring original screen density...")
        run_cmd("adb shell wm density reset")


if __name__ == "__main__":
    main()
