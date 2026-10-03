# Memora 15-Second Viral Demo Recorder 🎬

This tool lets you automatically record a studio-grade, 60fps 15-second vertical demo video (1080x1920) of Memora with animated tap interactions, captions, and transitions using **Playwright**.

---

## Quick Start

### 1. Install Playwright Dependencies
In this directory:
```bash
npm install
npx playwright install chromium
```

### 2. Generate the Video
```bash
npm run record
# or: node record_demo.js
```

The 15-second viral demo will be automatically recorded and saved to:
`tools/playwright-demo/output/memora_viral_15s_demo.webm`

---

## What the 15-Second Demo Shows
1. **0s - 3s (The Pain Point)**: Reading an email in Gmail with a buried deadline (`OPPE Submission: 4th Oct, 11:59 PM`).
2. **3s - 7s (The Magic)**: Tap the floating Memora button without switching apps. AI extracts title, category (`Academics`), and deadline in 1 second.
3. **7s - 11s (The Save)**: One-tap `Save Memory` -> auto-synced to interactive timeline & calendar.
4. **11s - 15s (Call to Action)**: Neo-Brutalist brand screen inviting stars and clones on GitHub.

---

## Real Phone Cut
In the project root, a 15-second cut from your real OnePlus 8 phone screen recording has also been created:
`memora_15s_real_clip.mp4`
