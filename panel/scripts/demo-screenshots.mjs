/**
 * Photographs the demo's overview in both languages and themes
 * (docs/screenshots.md). The demo answers after a short delay, so it waits
 * for the skeletons to come and go, then for the chart animations. Run
 * against a served demo build: `node scripts/demo-screenshots.mjs <base url> <output dir>`.
 */
import { chromium } from "playwright";

const [baseUrl, outputDir] = process.argv.slice(2);
const LANGUAGES = ["de", "en"];
const THEMES = ["dark", "light"];
const SKELETON = "[data-slot=skeleton]";

const browser = await chromium.launch();
for (const language of LANGUAGES) {
  for (const theme of THEMES) {
    const page = await browser.newPage({
      viewport: { width: 1600, height: 900 },
      deviceScaleFactor: 2,
    });
    const failures = [];
    page.on("pageerror", (error) => failures.push(error.message));
    await page.addInitScript(([storedLanguage, storedTheme]) => {
      localStorage.setItem("clarity-language", storedLanguage);
      localStorage.setItem("clarity-theme", storedTheme);
    }, [language, theme]);
    await page.goto(baseUrl);
    await page.locator(SKELETON).first().waitFor();
    await page.waitForFunction(
      (selector) => !document.querySelector(selector), SKELETON);
    await page.waitForTimeout(1500);
    if (failures.length > 0) {
      throw new Error(`${language}-${theme}: ${failures.join("; ")}`);
    }
    await page.screenshot({
      path: `${outputDir}/${language}-${theme}/overview.png`,
    });
    await page.close();
  }
}
await browser.close();