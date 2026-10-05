# 🔒 Java Image Steganography Tool

A lightweight, powerful Java-based Command-Line Utility that enables users to hide secret text messages inside PNG images using **Least Significant Bit (LSB) Bitwise Manipulation**. The modified image looks visually identical to the original image to the human eye, ensuring high security and privacy.

---

## 🌟 Key Features

- **Invisible Data Hiding:** Embeds secret text messages directly into the Red channel LSBs of image pixels.
- **Visual Losslessness:** Preserves full image quality without perceptible distortion.
- **Automatic Capacity Check:** Prevents buffer overflow by ensuring the text fits within the given image resolution.
- **Delimiter Detection:** Uses custom delimiters to accurately stop decoding at the end of the hidden payload.
- **Zero Heavy External Dependencies:** Built using standard Core Java libraries (`AWT`, `ImageIO`, `Scanner`).

---

## 🛠️ Tech Stack & Concepts Used

- **Language:** Java (JDK 8+)
- **Core Engineering Concepts:**
  - Bitwise Operations (`&`, `|`, `<<`, `>>`)
  - Image Buffer Handling (`BufferedImage`, ARGB Channel Separation)
  - File I/O Operations (`ImageIO`, Standard IO Streams)
  - Terminal/CLI Application Design

---

## 🚀 Getting Started

### Prerequisites
Make sure you have JDK installed on your system. You can verify by running:
```bash
java -version
