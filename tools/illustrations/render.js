const { chromium } = require('playwright'); const fs = require('fs'); const path = require('path');
(async () => {
  const dir = process.argv[2]; const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 900, height: 600 } });
  for (const f of fs.readdirSync(dir).filter(f => f.endsWith('.svg'))) {
    await p.setContent(`<html><body style="margin:0">${fs.readFileSync(path.join(dir, f), 'utf8')}</body></html>`);
    await p.screenshot({ path: path.join(dir, f.replace('.svg', '.png')), clip: { x: 0, y: 0, width: 900, height: 600 } });
  }
  await b.close(); console.log('rendered');
})();
