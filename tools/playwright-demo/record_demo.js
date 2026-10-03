const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

async function recordViralDemo() {
  console.log('🎬 Launching Playwright to record 15-second viral demo...');
  
  const outputDir = path.resolve(__dirname, 'output');
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  const browser = await chromium.launch({
    headless: true
  });

  // 1080x1920 (9:16 vertical format, optimal for Reddit Mobile, Twitter, YouTube Shorts, Reels)
  const context = await browser.newContext({
    viewport: { width: 1080, height: 1920 },
    recordVideo: {
      dir: outputDir,
      size: { width: 1080, height: 1920 }
    }
  });

  const page = await context.newPage();
  const demoUrl = 'file://' + path.resolve(__dirname, 'demo.html').replace(/\\/g, '/');
  console.log('📱 Loading showcase:', demoUrl);

  await page.goto(demoUrl, { waitUntil: 'networkidle' });

  console.log('⏱️ Recording 15 seconds of interactive action...');
  // Let the entire 15-second scripted sequence play smoothly
  await page.waitForTimeout(15500);

  console.log('💾 Finalizing video recording...');
  await page.close();
  await context.close();
  await browser.close();

  // Locate the saved video file
  const videoFiles = fs.readdirSync(outputDir).filter(f => f.endsWith('.webm'));
  if (videoFiles.length > 0) {
    const latestFile = path.join(outputDir, videoFiles[videoFiles.length - 1]);
    const finalTarget = path.join(outputDir, 'memora_viral_15s_demo.webm');
    try {
      if (fs.existsSync(finalTarget)) fs.unlinkSync(finalTarget);
      fs.renameSync(latestFile, finalTarget);
      console.log(`\n🎉 SUCCESS! Your 15-second viral demo is ready:\n${finalTarget}`);
    } catch (e) {
      console.log(`\n🎉 SUCCESS! Video saved at: ${latestFile}`);
    }
  }
}

recordViralDemo().catch(err => {
  console.error('❌ Failed to record demo:', err);
  process.exit(1);
});
