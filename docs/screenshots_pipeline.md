# 📸 Automated Screenshot Pipeline

To automate the tedious task of creating Play Store marketing materials, this repository includes a complete Python and ADB-based screenshot automation and post-processing pipeline. 

Located in the [screenshots_automation](../screenshots_automation/) directory, this pipeline automatically runs the app, takes screenshots across multiple languages, emulates tablet displays, crops out system UI elements, and generates professional promotional cards.

### 🌟 Pipeline Features
* **Multi-Language Automation:** Cycles through English (`en-US`), Spanish (`es-ES`), and French (`fr-FR`) by swapping the app locale on the fly via ADB.
* **Virtual Tablet Emulation:** Overrides the device display density (`wm density 280`) via ADB to force the app into tablet layouts without needing standard physical tablets or heavy emulators.
* **Aspect Ratio & Device Bar Cropping:** Automatically strips notifications, status bars, and navigation gesture bars, resizing the screen area to fit strict Play Store rules (9:16 ratio, `1080×1920` size).
* **Premium Showcase Generator:** Uses the `Pillow` image library to embed the screen captures inside styled terracotta dark-themed template frames with localized marketing headings.

### 🏃 How to Run the Pipeline
1. Connect your Android device via USB and make sure ADB is enabled and authorized:
   ```bash
   adb devices
   ```
2. Navigate to the automation directory and install dependencies:
   ```bash
   cd screenshots_automation
   pip install Pillow
   ```
3. Run the automation scripts:
   * **For Phones:**
     ```bash
     python3 screenshot_maker.py
     ```
   * **For Tablets (7-inch & 10-inch formats):**
     ```bash
     python3 screenshot_maker_tablet.py
     ```
4. Find the finalized, store-ready PNG images under:
   * `screenshots_automation/google_play/` (for phone screenshots)
   * `screenshots_automation/google_play_tablet/7_inch/` and `google_play_tablet/10_inch/` (for tablet screenshots)
