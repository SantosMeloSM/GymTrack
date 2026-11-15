import json, math, sys
OUT = sys.argv[1]
W, H, FLOOR = 900, 600, 520
NEAR, FAR, SKIN_HEAD = "#E8EAEE", "#9AA0AA", "#E8EAEE"
STEEL, STEEL_D, LIME = "#5B626D", "#3A3F48", "#C8F135"

def bg(extra=""):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs><linearGradient id="g" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#20242B"/><stop offset="1" stop-color="#121418"/></linearGradient>
<radialGradient id="spot" cx="0.5" cy="0.45" r="0.6"><stop offset="0" stop-color="#2A3038" stop-opacity="0.9"/><stop offset="1" stop-color="#121418" stop-opacity="0"/></radialGradient></defs>
<rect width="{W}" height="{H}" fill="url(#g)"/><rect width="{W}" height="{H}" fill="url(#spot)"/>
<rect x="0" y="{FLOOR}" width="{W}" height="{H-FLOOR}" fill="#0E1013"/><line x1="0" y1="{FLOOR}" x2="{W}" y2="{FLOOR}" stroke="#2E333B" stroke-width="3"/>
<ellipse cx="450" cy="{FLOOR+8}" rx="230" ry="10" fill="#000" opacity="0.35"/>
{extra}'''

def seg(a, b, w, c):
    return f'<line x1="{a[0]}" y1="{a[1]}" x2="{b[0]}" y2="{b[1]}" stroke="{c}" stroke-width="{w}" stroke-linecap="round"/>'

def limb_chain(pts, ws, c):
    return "".join(seg(pts[i], pts[i+1], ws[i], c) for i in range(len(pts)-1))

def side_figure(p, far_offset=(-10, 0)):
    """Side-view figure. p: dict of joints. Far limbs drawn first, slightly offset & darker."""
    fo = lambda q: (q[0]+far_offset[0], q[1]+far_offset[1])
    s = ""
    # far leg & arm
    s += limb_chain([fo(p['hip']), fo(p['knee2']), fo(p['ankle2']), fo(p['toe2'])], [34, 28, 16], FAR)
    s += limb_chain([fo(p['shoulder']), fo(p['elbow2']), fo(p['wrist2'])], [24, 20], FAR)
    # torso (tapered: two strokes)
    s += seg(p['hip'], p['shoulder'], 50, NEAR)
    s += seg(p['shoulder'], p['neck'], 22, NEAR)
    s += f'<circle cx="{p["head"][0]}" cy="{p["head"][1]}" r="29" fill="{SKIN_HEAD}"/>'
    # near leg & arm
    s += limb_chain([p['hip'], p['knee'], p['ankle'], p['toe']], [36, 30, 17], NEAR)
    return s

def near_arm(p):
    return limb_chain([p['shoulder'], p['elbow'], p['wrist']], [26, 22], NEAR) + f'<circle cx="{p["wrist"][0]}" cy="{p["wrist"][1]}" r="12" fill="{NEAR}"/>'

def kettlebell(x, y, r=30):
    return (f'<path d="M{x-16},{y-r+4} Q{x-18},{y-r-30} {x},{y-r-30} Q{x+18},{y-r-30} {x+16},{y-r+4}" fill="none" stroke="{STEEL}" stroke-width="9" stroke-linecap="round"/>'
            f'<circle cx="{x}" cy="{y}" r="{r}" fill="{STEEL}" stroke="{LIME}" stroke-width="3"/>')

def plate(x, y, r=62):
    return (f'<circle cx="{x}" cy="{y}" r="{r}" fill="{STEEL_D}" stroke="{LIME}" stroke-width="4" opacity="0.95"/>'
            f'<circle cx="{x}" cy="{y}" r="{r*0.55}" fill="none" stroke="{STEEL}" stroke-width="4"/><circle cx="{x}" cy="{y}" r="9" fill="{STEEL}"/>')

def label(text):
    return f'<text x="{W-24}" y="{H-24}" font-family="sans-serif" font-size="18" fill="#6B7280" text-anchor="end">Ilustração GymTrack · {text}</text>'

def stand(dx=0, dy=0):
    j = dict(hip=(450,370), knee=(452,446), ankle=(450,515), toe=(488,517), knee2=(452,446), ankle2=(450,515), toe2=(488,517),
             shoulder=(452,248), neck=(456,212), head=(462,182), elbow=(456,318), wrist=(460,386), elbow2=(456,318), wrist2=(460,386))
    return {k: (v[0]+dx, v[1]+dy) for k, v in j.items()}

frames = {}

# --- Kettlebell swing (two hands) ---
a = dict(hip=(352,360), knee=(428,438), ankle=(432,515), toe=(470,517), knee2=(428,438), ankle2=(432,515), toe2=(470,517),
         shoulder=(500,300), neck=(534,286), head=(562,276), elbow=(470,368), wrist=(444,430), elbow2=(470,368), wrist2=(444,430))
b = stand(); b.update(elbow=(522,262), wrist=(592,272), elbow2=(522,262), wrist2=(592,272))
frames['kb_swing'] = [
    (bg() + side_figure(a) + kettlebell(440, 476) + near_arm(a) + label("Kettlebell swing")+"</svg>", "Início: quadril para trás, kettlebell entre as pernas"),
    (bg() + side_figure(b) + kettlebell(612, 318) + near_arm(b) + label("Kettlebell swing")+"</svg>", "Fim: quadril estendido, kettlebell na altura do peito"),
]

# --- Thruster (barbell) ---
a = dict(hip=(372,432), knee=(486,446), ankle=(450,515), toe=(490,517), knee2=(486,446), ankle2=(450,515), toe2=(490,517),
         shoulder=(426,292), neck=(436,258), head=(446,228), elbow=(500,312), wrist=(458,280), elbow2=(500,312), wrist2=(458,280))
b = stand(); b.update(elbow=(458,178), wrist=(462,108), elbow2=(458,178), wrist2=(462,108))
frames['thruster'] = [
    (bg() + plate(476, 288, 44) + side_figure(a) + near_arm(a) + label("Thruster")+"</svg>", "Início: agachamento frontal, barra apoiada nos ombros"),
    (bg() + plate(464, 104, 44) + side_figure(b) + near_arm(b) + label("Thruster")+"</svg>", "Fim: em pé, barra estendida acima da cabeça"),
]

# --- Burpee (4 positions) ---
f1 = stand()
f2 = dict(hip=(392,452), knee=(478,446), ankle=(440,515), toe=(478,517), knee2=(478,446), ankle2=(440,515), toe2=(478,517),
          shoulder=(500,352), neck=(524,328), head=(548,308), elbow=(526,430), wrist=(536,508), elbow2=(526,430), wrist2=(536,508))
f3 = dict(hip=(430,440), knee=(338,468), ankle=(256,496), toe=(236,517), knee2=(338,468), ankle2=(256,496), toe2=(236,517),
          shoulder=(590,402), neck=(626,392), head=(656,384), elbow=(596,458), wrist=(600,510), elbow2=(596,458), wrist2=(600,510))
f4 = stand(dy=-48); f4.update(elbow=(470,112), wrist=(484,48), elbow2=(440,112), wrist2=(430,48))
frames['burpee'] = [
    (bg() + side_figure(f1) + near_arm(f1) + label("Burpee")+"</svg>", "1. Em pé"),
    (bg() + side_figure(f2) + near_arm(f2) + label("Burpee")+"</svg>", "2. Agache e apoie as mãos no chão"),
    (bg() + side_figure(f3) + near_arm(f3) + label("Burpee")+"</svg>", "3. Pernas para trás: prancha / flexão"),
    (bg() + side_figure(f4) + near_arm(f4) + label("Burpee")+"</svg>", "4. Volte e salte com os braços acima da cabeça"),
]

# --- Machine lateral raise (front view) ---
def machine_front(arm_up):
    s = bg()
    # machine frame behind
    s += f'<rect x="300" y="120" width="300" height="20" rx="8" fill="{STEEL_D}"/><rect x="430" y="120" width="40" height="300" rx="8" fill="{STEEL_D}"/>'
    s += f'<rect x="330" y="400" width="240" height="26" rx="10" fill="{STEEL}"/><rect x="438" y="426" width="24" height="94" fill="{STEEL_D}"/><rect x="380" y="512" width="140" height="10" rx="4" fill="{STEEL_D}"/>'
    # body (front view)
    sh_l, sh_r = (392, 232), (508, 232)
    s += seg((420, 392), (450, 238), 0, NEAR)
    s += f'<path d="M386,226 L514,226 L494,392 L406,392 Z" fill="{NEAR}" stroke="{NEAR}" stroke-width="14" stroke-linejoin="round"/>'
    s += seg((450, 226), (450, 196), 26, NEAR) + f'<circle cx="450" cy="166" r="30" fill="{SKIN_HEAD}"/>'
    # legs: thighs toward viewer (short), shins vertical
    for x in (418, 482):
        s += seg((x, 392), (x + (-14 if x < 450 else 14), 432), 36, FAR) + seg((x + (-14 if x < 450 else 14), 432), (x + (-14 if x < 450 else 14), 512), 30, NEAR)
    # arms + pads + lever arms
    if arm_up:
        el_l, wr_l, el_r, wr_r = (300, 240), (232, 256), (600, 240), (668, 256)
    else:
        el_l, wr_l, el_r, wr_r = (372, 318), (378, 390), (528, 318), (522, 390)
    for sh, el, wr, side in ((sh_l, el_l, wr_l, -1), (sh_r, el_r, wr_r, 1)):
        pivot = (450 + side * 40, 210)
        s += seg(pivot, el, 10, STEEL)
        s += limb_chain([sh, el, wr], [26, 22], NEAR) + f'<circle cx="{wr[0]}" cy="{wr[1]}" r="12" fill="{NEAR}"/>'
        mx, my = (sh[0] + el[0]) / 2, (sh[1] + el[1]) / 2
        ang = math.degrees(math.atan2(el[1] - sh[1], el[0] - sh[0]))
        s += f'<rect x="{mx-34}" y="{my-15}" width="68" height="30" rx="12" fill="{STEEL}" stroke="{LIME}" stroke-width="3" transform="rotate({ang} {mx} {my})"/>'
    s += label("Elevação lateral máquina") + "</svg>"
    return s

frames['machine_lateral_raise'] = [
    (machine_front(False), "Início: braços ao lado do corpo, apoios nos braços"),
    (machine_front(True), "Fim: braços elevados até a altura dos ombros"),
]

meta = {}
for key, fr in frames.items():
    meta[key] = []
    for i, (svg, cap) in enumerate(fr):
        open(f"{OUT}/{key}_{i}.svg", "w").write(svg)
        meta[key].append(cap)
json.dump(meta, open(f"{OUT}/captions.json", "w"), ensure_ascii=False, indent=1)
print("ok", {k: len(v) for k, v in frames.items()})
