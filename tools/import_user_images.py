#!/usr/bin/env python3
"""Converts two-position anatomy illustrations (start | end in one image) into aligned
animation frames for the app.

Usage: python3 tools/import_user_images.py <uniq.json> <mapping.txt> <assets/exercises dir>
  - Splits each image at the emptiest column (or row, for stacked layouts) near the middle.
  - Normalizes the background to white, crops each position to its content, scales both
    positions by the same factor and aligns them at the bottom (feet) so a loop between
    them does not jump.
  - Writes <key>/0.webp and <key>/1.webp (640x640) and updates manifest.json.
"""
import json, os, sys
import numpy as np
from PIL import Image

SIZE, PAD = 640, 0.06

def normalize_bg(im):
    a = np.asarray(im.convert("RGB")).astype(np.float32)
    corners = np.concatenate([a[:20, :20].reshape(-1, 3), a[:20, -20:].reshape(-1, 3), a[-20:, :20].reshape(-1, 3), a[-20:, -20:].reshape(-1, 3)])
    bg = np.median(corners, axis=0)
    a = np.clip(a * (255.0 / np.maximum(bg, 1)), 0, 255)
    return Image.fromarray(a.astype(np.uint8))

def content_mask(im, thr=232):
    return np.asarray(im.convert("L")) < thr

def cleanup(mask):
    """Drops small fragments that are far from the main object (leftovers of the other position)."""
    from scipy import ndimage
    labels, count = ndimage.label(ndimage.binary_dilation(mask, iterations=3))
    if count <= 1: return mask
    sizes = ndimage.sum(mask, labels, range(1, count + 1))
    main = int(np.argmax(sizes)) + 1
    ys, xs = np.where(labels == main)
    y0, y1, x0, x1 = ys.min() - 25, ys.max() + 25, xs.min() - 25, xs.max() + 25
    out = np.zeros_like(mask)
    for lab in range(1, count + 1):
        comp = (labels == lab) & mask
        cy, cx = np.where(comp)
        if len(cx) == 0: continue
        near = not (cx.max() < x0 or cx.min() > x1 or cy.max() < y0 or cy.min() > y1)
        if lab == main or sizes[lab - 1] >= 0.15 * sizes[main - 1] or (near and sizes[lab - 1] >= 0.01 * sizes[main - 1]):
            out |= comp
    return out

def split(im, layout, cut_frac=None):
    """Separates the two positions using connected components: every object (figure, bar,
    machine) goes whole to the side holding most of its pixels; specks are dropped. Only a
    blob that physically joins both figures is cut at the emptiest line."""
    from scipy import ndimage
    m = content_mask(im)
    axis = 1 if layout == "v" else 0
    prof = m.sum(axis=axis); n = len(prof)
    lo, hi = (int(n * 0.3), int(n * 0.7)) if layout == "v" else (int(n * 0.35), int(n * 0.65))
    cut = int(n * cut_frac) if cut_frac else lo + int(np.argmin(prof[lo:hi]))
    grown = ndimage.binary_dilation(m, iterations=3)
    labels, count = ndimage.label(grown)
    coord = np.arange(m.shape[0])[:, None] if layout == "v" else np.arange(m.shape[1])[None, :]
    side_a = np.zeros_like(m); side_b = np.zeros_like(m); merged = 0
    min_px = m.size * 0.0004
    for lab in range(1, count + 1):
        comp = (labels == lab) & m
        px = comp.sum()
        if px < min_px: continue  # noise
        before = (comp & (coord < cut)).sum()
        after = px - before
        if min(before, after) > 0.25 * px:  # truly joins both figures: cut it
            merged += 1
            side_a |= comp & (coord < cut); side_b |= comp & (coord >= cut)
        elif before >= after: side_a |= comp
        else: side_b |= comp
    side_a, side_b = cleanup(side_a), cleanup(side_b)
    arr = np.asarray(im.convert("RGB"))
    def keep(mask):
        out = np.full_like(arr, 255)
        grown_mask = ndimage.binary_dilation(mask, iterations=2)  # keep antialiased edges
        out[grown_mask] = arr[grown_mask]
        return Image.fromarray(out)
    return keep(side_a), keep(side_b), merged

def bbox(im):
    m = content_mask(im)
    ys, xs = np.where(m)
    if len(xs) == 0: return (0, 0, im.width, im.height)
    return (int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1)

def frames(im, layout, cut_frac=None):
    a, b, overlap = split(normalize_bg(im), layout, cut_frac)
    boxes = [bbox(a), bbox(b)]
    crops = [x.crop(bb) for x, bb in zip((a, b), boxes)]
    inner = SIZE * (1 - 2 * PAD)
    scale = min(inner / max(c.width for c in crops), inner / max(c.height for c in crops))
    out = []
    for c in crops:
        c = c.resize((max(1, round(c.width * scale)), max(1, round(c.height * scale))), Image.LANCZOS)
        canvas = Image.new("RGB", (SIZE, SIZE), (255, 255, 255))
        x = (SIZE - c.width) // 2
        y = SIZE - round(SIZE * PAD) - c.height  # feet aligned at the same baseline
        canvas.paste(c, (x, y)); out.append(canvas)
    return out, overlap

def main(uniq_path, mapping_path, assets):
    items = {it["id"]: it for it in json.load(open(uniq_path, encoding="utf-8"))}
    man_path = os.path.join(assets, "manifest.json")
    manifest = json.load(open(man_path, encoding="utf-8"))
    for line in open(mapping_path, encoding="utf-8"):
        line = line.strip()
        if not line or line.startswith("#"): continue
        parts = line.split()
        key, iid, layout = parts[:3]
        cut_frac = float(parts[3]) if len(parts) > 3 else None
        src = items[int(iid)]
        fr, overlap = frames(Image.open(src["path"]), layout, cut_frac)
        d = os.path.join(assets, key)
        if os.path.isdir(d):
            for f in os.listdir(d): os.remove(os.path.join(d, f))
        os.makedirs(d, exist_ok=True)
        for i, f in enumerate(fr): f.save(os.path.join(d, f"{i}.webp"), "WEBP", quality=80, method=6)
        manifest[key] = {"kind": "anatomy", "frames": 2, "captions": ["Posição inicial", "Posição final"],
                         "source": "Ilustração anatômica fornecida pelo proprietário do app",
                         "sourceId": src["label"], "license": "Fornecida pelo proprietário do app", "swapped": False}
        if overlap > 0: print(f"note: {key}: {overlap} object(s) joined both positions and were cut")
    json.dump(dict(sorted(manifest.items())), open(man_path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("ok")

if __name__ == "__main__":
    main(*sys.argv[1:4])
