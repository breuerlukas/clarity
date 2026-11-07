# Screenshots

Every deployment of the demo to GitHub Pages photographs its overview and
publishes the pictures next to it, so the README always shows the current
panel with the demo's sample data. Nothing is committed.

`https://breuerlukas.github.io/clarity/screenshots/<language>-<theme>/overview.png`

`<language>-<theme>` is `de-dark`, `de-light`, `en-dark` or `en-light`; each
picture is 3200 x 1800 (1600 x 900 at 2x). The `Demo on GitHub Pages`
workflow serves the demo build with `vite preview` and runs
`panel/scripts/demo-screenshots.mjs`, which presets `clarity-language` and
`clarity-theme` in `localStorage`, waits until no skeleton is left plus 1.5 s
for the chart animations, and fails the job on any page error.

Locally, in `panel/`:

```bash
VITE_DEMO=true npx vite build --base=/clarity/
npx playwright install chromium
npx vite preview --base=/clarity/ --port 4173 &
node scripts/demo-screenshots.mjs http://localhost:4173/clarity/ dist/screenshots
```