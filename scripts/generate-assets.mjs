#!/usr/bin/env node
/**
 * Regenerates the app's bundled resources from the sibling iPad repo (the
 * canonical native asset set — itself rendered from the Fire TV app):
 *
 *   fonts        FifiRecipesPad/Fonts/*.ttf      → app/src/main/res/font/
 *   ui strings   Resources/ui-strings.json       → app/src/main/assets/
 *   kids art     Assets.xcassets/KidsArt/*.png   → assets/kids-art/*.webp (384px)
 *   emblem       Emblem.imageset/emblem.png      → res/drawable-nodpi/emblem.webp
 *   launcher     adaptive foreground + monochrome (themed icons) layers
 *   store icon   store/icon-512.png (Play listing, 512×512)
 *
 * Usage:  (cd scripts && npm install) && IPAD_REPO=../fifirecipes-ipadosapp node scripts/generate-assets.mjs
 */
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const ipad = path.resolve(root, process.env.IPAD_REPO || '../fifirecipes-ipadosapp', 'FifiRecipesPad');
const sharp = createRequire(path.join(root, 'scripts/package.json'))('sharp');

const res = path.join(root, 'app/src/main/res');
const assets = path.join(root, 'app/src/main/assets');
const mk = (d) => fs.mkdirSync(d, { recursive: true });

// Fonts: Android resource names must be [a-z0-9_].
mk(path.join(res, 'font'));
for (const f of fs.readdirSync(path.join(ipad, 'Fonts'))) {
  if (!f.endsWith('.ttf')) continue;
  const name = f.replace('.ttf', '').replace(/-/g, '_').toLowerCase() + '.ttf';
  fs.copyFileSync(path.join(ipad, 'Fonts', f), path.join(res, 'font', name));
}
mk(path.join(assets, 'licenses'));
fs.copyFileSync(path.join(ipad, 'Fonts/OFL.txt'), path.join(assets, 'licenses/OFL.txt'));

mk(assets);
fs.copyFileSync(path.join(ipad, 'Resources/ui-strings.json'), path.join(assets, 'ui-strings.json'));

// Kids art: 512px PNG → 384px WebP (largest on-screen use is ~120dp @ xxhdpi).
const artDir = path.join(ipad, 'Assets.xcassets/KidsArt');
const outArt = path.join(assets, 'kids-art');
fs.rmSync(outArt, { recursive: true, force: true });
mk(outArt);
let n = 0;
for (const d of fs.readdirSync(artDir)) {
  if (!d.endsWith('.imageset')) continue;
  const id = d.replace('.imageset', '');
  const png = fs.readdirSync(path.join(artDir, d)).find((f) => f.endsWith('.png'));
  if (!png) continue;
  await sharp(path.join(artDir, d, png)).resize(384, 384, { fit: 'inside' })
    .webp({ quality: 88, alphaQuality: 90 }).toFile(path.join(outArt, `${id}.webp`));
  n++;
}

// Emblem (splash, sidebar, error screen).
const emblem = path.join(ipad, 'Assets.xcassets/Emblem.imageset/emblem.png');
mk(path.join(res, 'drawable-nodpi'));
await sharp(emblem).resize(384).webp({ quality: 92 }).toFile(path.join(res, 'drawable-nodpi/emblem.webp'));

// Adaptive launcher: 108dp canvas @ xxxhdpi = 432px; the emblem sits inside
// the 66dp safe zone (~264px) so no launcher mask clips it.
const canvas = 432, inner = 270;
const emb = await sharp(emblem).resize(inner, inner, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } }).png().toBuffer();
const blank = { create: { width: canvas, height: canvas, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } };
mk(path.join(res, 'mipmap-xxxhdpi'));
await sharp(blank).composite([{ input: emb, gravity: 'center' }]).webp({ quality: 95 })
  .toFile(path.join(res, 'mipmap-xxxhdpi/ic_launcher_foreground.webp'));
// Monochrome (Android 13 themed icons): the emblem's alpha as a solid silhouette.
const alpha = await sharp(emb).extractChannel('alpha').toBuffer();
const mono = await sharp({ create: { width: inner, height: inner, channels: 3, background: '#000' } })
  .joinChannel(alpha).png().toBuffer();
await sharp(blank).composite([{ input: mono, gravity: 'center' }]).webp({ quality: 95 })
  .toFile(path.join(res, 'mipmap-xxxhdpi/ic_launcher_monochrome.webp'));

// Play Console hi-res icon: 512×512, 32-bit PNG.
mk(path.join(root, 'store'));
await sharp(path.join(ipad, 'Assets.xcassets/AppIcon.appiconset/icon-1024.png')).resize(512, 512)
  .png().toFile(path.join(root, 'store/icon-512.png'));

console.log(`fonts, strings, ${n} kids art, emblem, launcher + store icons written`);
