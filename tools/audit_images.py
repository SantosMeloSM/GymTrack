#!/usr/bin/env python3
"""Audits the bundled exercise images and writes ENTREGA/RELATORIO-IMAGENS.md.

Checks per exercise: manifest entry, every frame file exists, is WebP, decodes, has the
standard size, and is not reused by another exercise. Run from the repository root:
    python3 tools/audit_images.py
"""
import hashlib, json, os, re, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app/src/main/assets/exercises")
SEED = os.path.join(ROOT, "app/src/main/java/com/gymtrack/app/data/seed/ExerciseSeed.kt")
OUT = os.path.join(ROOT, "ENTREGA/RELATORIO-IMAGENS.md")

# Notes from the manual visual review (every pair was inspected individually).
REVIEW_NOTES = {
    "bulgarian_split_squat": "Imagem trocada para 'One_Leg_Barbell_Squat' (pé de trás no banco); equipamento ajustado para Barra.",
    "goblet_squat": "Foto com kettlebell: equipamento ajustado para Kettlebell (descrição cita halter como alternativa).",
    "reverse_curl": "Foto com barra reta: equipamento ajustado para Barra.",
    "cable_lateral_raise": "Foto da variação sentada bilateral: descrição ajustada.",
    "db_overhead_ext": "Foto em pé: descrição ajustada para 'em pé ou sentado'.",
    "lying_leg_raise": "Foto em banco: descrição ajustada para 'no chão ou em um banco'.",
    "upright_row": "Ordem das fotos invertida para início → fim.",
    "smith_squat": "Ordem das fotos invertida para início → fim.",
    "romanian_deadlift": "Ordem das fotos invertida para início → fim.",
}

seeds = re.findall(r'ex\("([a-z0-9_]+)", "([^"]+)", ([A-Z_]+),', open(SEED, encoding="utf-8").read())
manifest = json.load(open(os.path.join(ASSETS, "manifest.json"), encoding="utf-8"))
hashes, problems, rows = {}, [], []
total_bytes = 0
for key, name, muscle in seeds:
    m = manifest.get(key)
    if not m:
        problems.append(f"{key}: sem entrada no manifest"); rows.append((name, key, "—", 0, "—", "—", "SEM IMAGEM")); continue
    status = "OK"
    for i in range(m["frames"]):
        p = os.path.join(ASSETS, key, f"{i}.webp")
        if not os.path.isfile(p):
            problems.append(f"{key}: arquivo ausente {i}.webp"); status = "ERRO"; continue
        data = open(p, "rb").read(); total_bytes += len(data)
        if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
            problems.append(f"{key}/{i}.webp não é WebP"); status = "ERRO"
        try:
            with Image.open(p) as im:
                im.load()
                if im.size != (720, 480): problems.append(f"{key}/{i}.webp tamanho {im.size}"); status = "ERRO"
        except Exception as e:
            problems.append(f"{key}/{i}.webp não decodifica: {e}"); status = "ERRO"
        h = hashlib.sha256(data).hexdigest()
        if h in hashes: problems.append(f"{key}/{i}.webp idêntica a {hashes[h]}"); status = "ERRO"
        hashes[h] = f"{key}/{i}.webp"
    kind = "Foto" if m["kind"] == "photo" else "Ilustração"
    src = f"free-exercise-db: `{m['sourceId']}`" if m["kind"] == "photo" else "Original GymTrack"
    rows.append((name, key, kind, m["frames"], src, m["license"], status))

extra = sorted(set(manifest) - {k for k, _, _ in seeds})
for k in extra: problems.append(f"manifest tem chave sem exercício: {k}")
n = len(seeds)
with_img = sum(1 for r in rows if r[3] >= 1 and r[6] == "OK")
two_pos = sum(1 for r in rows if r[3] >= 2 and r[6] == "OK")
photos = sum(1 for r in rows if r[2] == "Foto")
illus = sum(1 for r in rows if r[2] == "Ilustração")
files = sum(r[3] for r in rows)
invalid = sum(1 for p in problems if "ausente" in p or "não" in p or "tamanho" in p or "idêntica" in p)

L = []
L.append("# GymTrack v1.1 — Relatório de auditoria das imagens\n")
L.append("Gerado por `tools/audit_images.py` a partir dos arquivos incluídos no APK (`app/src/main/assets/exercises`).\n")
L.append("## Resumo\n")
L.append("| Indicador | Valor |\n|---|---|")
L.append(f"| Exercícios na biblioteca padrão | **{n}** |")
L.append(f"| Com pelo menos uma imagem correta | **{with_img} ({with_img*100//n}%)** |")
L.append(f"| Com duas ou mais posições | **{two_pos}** |")
L.append(f"| Fotografias reais | {photos} exercícios ({photos*2} fotos) |")
L.append(f"| Ilustrações originais | {illus} exercícios |")
L.append(f"| Com animação | {two_pos} — reprodução sob demanda alternando as posições reais (não há vídeos/GIFs) |")
L.append(f"| Arquivos de imagem | {files} WebP, 720×480 (3:2), total {total_bytes/1024/1024:.1f} MB |")
L.append(f"| Arquivos inválidos | **{invalid}** |")
L.append(f"| Imagens repetidas entre exercícios | **{sum(1 for p in problems if 'idêntica' in p)}** |")
L.append(f"| Funciona offline | Sim — todas as imagens estão dentro do APK (assets) |\n")
L.append("## Problemas encontrados\n")
L.append("Nenhum." if not problems else "\n".join(f"- {p}" for p in problems))
L.append("\n## Verificações realizadas\n")
L.append("- **Correspondência com o exercício:** cada par de fotos foi inspecionado visualmente, um a um. Ajustes feitos estão na coluna *Observações*.")
L.append("- **Arquivo existe / é WebP / decodifica / 720×480 / não repetido:** este script e o teste automatizado `MediaAuditTest` (decodifica cada arquivo com o `BitmapFactory` do Android).")
L.append("- **Aparece na biblioteca, nos detalhes e durante o treino:** teste de interface `UiFlowTest.imagesInLibraryDetailAndWorkout` e verificação manual no emulador Android 10 com o APK release.")
L.append("- **Offline:** as imagens são carregadas de `file:///android_asset/` — nenhuma URL externa é usada pelo app.\n")
L.append("## Origem e licença\n")
L.append("- **Fotos:** [Free Exercise DB](https://github.com/yuhonas/free-exercise-db), derivado de [wrkout/exercises.json](https://github.com/wrkout/exercises.json). "
         "Ambos são publicados sob **The Unlicense** (dedicação ao domínio público), que permite uso, modificação e distribuição, inclusive comercial, sem exigência de atribuição. "
         "O crédito é exibido mesmo assim no app (*Mais › Créditos das imagens* e no rodapé de cada exercício). "
         "Observação: a licença é declarada pelos mantenedores desses repositórios; não há como verificar de forma independente a autoria original de cada fotografia.")
L.append("- **Ilustrações:** criadas para o GymTrack (vetores gerados por código e renderizados localmente) para os exercícios sem foto fiel disponível: "
         + ", ".join(f"*{r[0]}*" for r in rows if r[2] == "Ilustração") + ".")
L.append("- As fotos foram redimensionadas e convertidas para WebP; imagens em retrato receberam fundo desfocado (sem cortar o executante).\n")
L.append("## Exercício por exercício\n")
L.append("| # | Exercício | Tipo | Posições | Origem | Licença | Status | Observações |\n|---|---|---|---|---|---|---|---|")
for i, (name, key, kind, frames, src, lic, status) in enumerate(rows, 1):
    L.append(f"| {i} | {name} | {kind} | {frames} | {src} | {lic} | {status} | {REVIEW_NOTES.get(key, '')} |")
open(OUT, "w", encoding="utf-8").write("\n".join(L) + "\n")
print(f"{n} exercícios, {with_img} com imagem, {two_pos} com 2+ posições, {files} arquivos, {invalid} inválidos, {len(problems)} problemas")
sys.exit(1 if problems else 0)
