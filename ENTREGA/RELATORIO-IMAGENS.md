# GymTrack v1.4 — Relatório de auditoria das imagens

Gerado por `tools/audit_images.py` a partir dos arquivos incluídos no APK (`app/src/main/assets/exercises`).

## Resumo

| Indicador | Valor |
|---|---|
| Exercícios na biblioteca padrão | **101** |
| Com pelo menos uma imagem correta | **100 (99%)** |
| Sem imagem (intencional) | 1 — machine_lateral_raise: Sem foto licenciada de pessoa usando a máquina (Wikimedia Commons e Openverse pesquisados). O usuário pode adicionar a própria foto no app. |
| Com duas ou mais posições | **99** |
| Fotografias reais | 100 exercícios (200 fotos) |
| Desenhos/ilustrações | 0 exercícios |
| Com animação | 99 — reprodução sob demanda alternando as posições reais (não há vídeos/GIFs) |
| Arquivos de imagem | 201 WebP, 720×480 (3:2), total 4.7 MB |
| Arquivos inválidos | **0** |
| Imagens repetidas entre exercícios | **0** |
| Funciona offline | Sim — todas as imagens estão dentro do APK (assets) |

## Problemas encontrados

Nenhum.

## Verificações realizadas

- **Correspondência com o exercício:** cada par de fotos foi inspecionado visualmente, um a um. Ajustes feitos estão na coluna *Observações*.
- **Arquivo existe / é WebP / decodifica / 720×480 / não repetido:** este script e o teste automatizado `MediaAuditTest` (decodifica cada arquivo com o `BitmapFactory` do Android).
- **Aparece na biblioteca, nos detalhes e durante o treino:** teste de interface `UiFlowTest.imagesInLibraryDetailAndWorkout` e verificação manual no emulador Android 10 com o APK release.
- **Offline:** as imagens são carregadas de `file:///android_asset/` — nenhuma URL externa é usada pelo app.

## Origem e licença

- **Fotos:** [Free Exercise DB](https://github.com/yuhonas/free-exercise-db), derivado de [wrkout/exercises.json](https://github.com/wrkout/exercises.json). Ambos são publicados sob **The Unlicense** (dedicação ao domínio público), que permite uso, modificação e distribuição, inclusive comercial, sem exigência de atribuição. O crédito é exibido mesmo assim no app (*Mais › Créditos das imagens* e no rodapé de cada exercício). Observação: a licença é declarada pelos mantenedores desses repositórios; não há como verificar de forma independente a autoria original de cada fotografia.
- **Wikimedia Commons:** burpee e kettlebell swing — fotos de Taco fleur (CC BY-SA 4.0); thruster — foto de Brad Greenlee (CC BY 2.0). Redimensionadas/recortadas para o app (as versões adaptadas seguem a mesma licença). Créditos com autor e link no app, em *Mais › Créditos das imagens*.
- As fotos foram redimensionadas e convertidas para WebP; imagens em retrato receberam fundo desfocado (sem cortar o executante).

## Exercício por exercício

| # | Exercício | Tipo | Posições | Origem | Licença | Status | Observações |
|---|---|---|---|---|---|---|---|
| 1 | Supino reto com barra | Foto | 2 | free-exercise-db: `Barbell_Bench_Press_-_Medium_Grip` | Unlicense (domínio público) | OK |  |
| 2 | Supino reto com halteres | Foto | 2 | free-exercise-db: `Dumbbell_Bench_Press` | Unlicense (domínio público) | OK |  |
| 3 | Supino inclinado com barra | Foto | 2 | free-exercise-db: `Barbell_Incline_Bench_Press_-_Medium_Grip` | Unlicense (domínio público) | OK |  |
| 4 | Supino inclinado com halteres | Foto | 2 | free-exercise-db: `Incline_Dumbbell_Press` | Unlicense (domínio público) | OK |  |
| 5 | Supino declinado | Foto | 2 | free-exercise-db: `Decline_Barbell_Bench_Press` | Unlicense (domínio público) | OK |  |
| 6 | Supino máquina | Foto | 2 | free-exercise-db: `Machine_Bench_Press` | Unlicense (domínio público) | OK |  |
| 7 | Supino inclinado no Smith | Foto | 2 | free-exercise-db: `Smith_Machine_Incline_Bench_Press` | Unlicense (domínio público) | OK |  |
| 8 | Crucifixo com halteres | Foto | 2 | free-exercise-db: `Dumbbell_Flyes` | Unlicense (domínio público) | OK |  |
| 9 | Crucifixo inclinado | Foto | 2 | free-exercise-db: `Incline_Dumbbell_Flyes` | Unlicense (domínio público) | OK |  |
| 10 | Crucifixo máquina (peck deck) | Foto | 2 | free-exercise-db: `Butterfly` | Unlicense (domínio público) | OK |  |
| 11 | Crossover na polia | Foto | 2 | free-exercise-db: `Cable_Crossover` | Unlicense (domínio público) | OK |  |
| 12 | Flexão de braço | Foto | 2 | free-exercise-db: `Pushups` | Unlicense (domínio público) | OK |  |
| 13 | Paralelas (mergulho) | Foto | 2 | free-exercise-db: `Dips_-_Chest_Version` | Unlicense (domínio público) | OK |  |
| 14 | Puxada alta (pulldown) | Foto | 2 | free-exercise-db: `Wide-Grip_Lat_Pulldown` | Unlicense (domínio público) | OK |  |
| 15 | Puxada com triângulo | Foto | 2 | free-exercise-db: `V-Bar_Pulldown` | Unlicense (domínio público) | OK |  |
| 16 | Barra fixa | Foto | 2 | free-exercise-db: `Pullups` | Unlicense (domínio público) | OK |  |
| 17 | Barra fixa supinada | Foto | 2 | free-exercise-db: `Chin-Up` | Unlicense (domínio público) | OK |  |
| 18 | Remada baixa | Foto | 2 | free-exercise-db: `Seated_Cable_Rows` | Unlicense (domínio público) | OK |  |
| 19 | Remada curvada com barra | Foto | 2 | free-exercise-db: `Bent_Over_Barbell_Row` | Unlicense (domínio público) | OK |  |
| 20 | Remada unilateral com halter (serrote) | Foto | 2 | free-exercise-db: `One-Arm_Dumbbell_Row` | Unlicense (domínio público) | OK |  |
| 21 | Remada cavalinho (T-bar) | Foto | 2 | free-exercise-db: `Lying_T-Bar_Row` | Unlicense (domínio público) | OK |  |
| 22 | Remada máquina | Foto | 2 | free-exercise-db: `Leverage_Iso_Row` | Unlicense (domínio público) | OK |  |
| 23 | Pulldown braços estendidos | Foto | 2 | free-exercise-db: `Straight-Arm_Pulldown` | Unlicense (domínio público) | OK |  |
| 24 | Levantamento terra | Foto | 2 | free-exercise-db: `Barbell_Deadlift` | Unlicense (domínio público) | OK |  |
| 25 | Rack pull | Foto | 2 | free-exercise-db: `Rack_Pulls` | Unlicense (domínio público) | OK |  |
| 26 | Extensão lombar (banco romano) | Foto | 2 | free-exercise-db: `Hyperextensions_Back_Extensions` | Unlicense (domínio público) | OK |  |
| 27 | Encolhimento com barra | Foto | 2 | free-exercise-db: `Barbell_Shrug` | Unlicense (domínio público) | OK |  |
| 28 | Encolhimento com halteres | Foto | 2 | free-exercise-db: `Dumbbell_Shrug` | Unlicense (domínio público) | OK |  |
| 29 | Desenvolvimento militar com barra | Foto | 2 | free-exercise-db: `Standing_Military_Press` | Unlicense (domínio público) | OK |  |
| 30 | Desenvolvimento com halteres | Foto | 2 | free-exercise-db: `Dumbbell_Shoulder_Press` | Unlicense (domínio público) | OK |  |
| 31 | Desenvolvimento máquina | Foto | 2 | free-exercise-db: `Machine_Shoulder_Military_Press` | Unlicense (domínio público) | OK |  |
| 32 | Desenvolvimento Arnold | Foto | 2 | free-exercise-db: `Arnold_Dumbbell_Press` | Unlicense (domínio público) | OK |  |
| 33 | Elevação lateral | Foto | 2 | free-exercise-db: `Side_Lateral_Raise` | Unlicense (domínio público) | OK |  |
| 34 | Elevação lateral na polia | Foto | 2 | free-exercise-db: `Cable_Seated_Lateral_Raise` | Unlicense (domínio público) | OK | Foto da variação sentada bilateral: descrição ajustada. |
| 35 | Elevação lateral máquina | — | 0 | — | — | SEM IMAGEM (intencional) |  |
| 36 | Elevação frontal | Foto | 2 | free-exercise-db: `Front_Dumbbell_Raise` | Unlicense (domínio público) | OK |  |
| 37 | Crucifixo invertido | Foto | 2 | free-exercise-db: `Seated_Bent-Over_Rear_Delt_Raise` | Unlicense (domínio público) | OK |  |
| 38 | Crucifixo invertido máquina | Foto | 2 | free-exercise-db: `Reverse_Machine_Flyes` | Unlicense (domínio público) | OK |  |
| 39 | Face pull | Foto | 2 | free-exercise-db: `Face_Pull` | Unlicense (domínio público) | OK |  |
| 40 | Remada alta | Foto | 2 | free-exercise-db: `Upright_Barbell_Row` | Unlicense (domínio público) | OK | Ordem das fotos invertida para início → fim. |
| 41 | Rosca direta com barra | Foto | 2 | free-exercise-db: `Barbell_Curl` | Unlicense (domínio público) | OK |  |
| 42 | Rosca direta barra W | Foto | 2 | free-exercise-db: `EZ-Bar_Curl` | Unlicense (domínio público) | OK |  |
| 43 | Rosca alternada com halteres | Foto | 2 | free-exercise-db: `Dumbbell_Alternate_Bicep_Curl` | Unlicense (domínio público) | OK |  |
| 44 | Rosca martelo | Foto | 2 | free-exercise-db: `Hammer_Curls` | Unlicense (domínio público) | OK |  |
| 45 | Rosca inclinada | Foto | 2 | free-exercise-db: `Incline_Dumbbell_Curl` | Unlicense (domínio público) | OK |  |
| 46 | Rosca Scott | Foto | 2 | free-exercise-db: `Preacher_Curl` | Unlicense (domínio público) | OK |  |
| 47 | Rosca concentrada | Foto | 2 | free-exercise-db: `Concentration_Curls` | Unlicense (domínio público) | OK |  |
| 48 | Rosca na polia | Foto | 2 | free-exercise-db: `Standing_Biceps_Cable_Curl` | Unlicense (domínio público) | OK |  |
| 49 | Tríceps pulley | Foto | 2 | free-exercise-db: `Triceps_Pushdown` | Unlicense (domínio público) | OK |  |
| 50 | Tríceps corda | Foto | 2 | free-exercise-db: `Triceps_Pushdown_-_Rope_Attachment` | Unlicense (domínio público) | OK |  |
| 51 | Tríceps francês na polia | Foto | 2 | free-exercise-db: `Cable_Rope_Overhead_Triceps_Extension` | Unlicense (domínio público) | OK |  |
| 52 | Tríceps francês com halter | Foto | 2 | free-exercise-db: `Standing_Dumbbell_Triceps_Extension` | Unlicense (domínio público) | OK | Foto em pé: descrição ajustada para 'em pé ou sentado'. |
| 53 | Tríceps testa | Foto | 2 | free-exercise-db: `EZ-Bar_Skullcrusher` | Unlicense (domínio público) | OK |  |
| 54 | Supino fechado | Foto | 2 | free-exercise-db: `Close-Grip_Barbell_Bench_Press` | Unlicense (domínio público) | OK |  |
| 55 | Mergulho no banco | Foto | 2 | free-exercise-db: `Bench_Dips` | Unlicense (domínio público) | OK |  |
| 56 | Tríceps coice | Foto | 2 | free-exercise-db: `Tricep_Dumbbell_Kickback` | Unlicense (domínio público) | OK |  |
| 57 | Rosca de punho | Foto | 2 | free-exercise-db: `Seated_Palm-Up_Barbell_Wrist_Curl` | Unlicense (domínio público) | OK |  |
| 58 | Rosca inversa | Foto | 2 | free-exercise-db: `Reverse_Barbell_Curl` | Unlicense (domínio público) | OK | Foto com barra reta: equipamento ajustado para Barra. |
| 59 | Caminhada do fazendeiro | Foto | 2 | free-exercise-db: `Farmers_Walk` | Unlicense (domínio público) | OK |  |
| 60 | Agachamento livre | Foto | 2 | free-exercise-db: `Barbell_Squat` | Unlicense (domínio público) | OK |  |
| 61 | Agachamento frontal | Foto | 2 | free-exercise-db: `Front_Barbell_Squat` | Unlicense (domínio público) | OK |  |
| 62 | Agachamento no Smith | Foto | 2 | free-exercise-db: `Smith_Machine_Squat` | Unlicense (domínio público) | OK | Ordem das fotos invertida para início → fim. |
| 63 | Agachamento hack | Foto | 2 | free-exercise-db: `Hack_Squat` | Unlicense (domínio público) | OK |  |
| 64 | Leg press 45° | Foto | 2 | free-exercise-db: `Leg_Press` | Unlicense (domínio público) | OK |  |
| 65 | Agachamento goblet | Foto | 2 | free-exercise-db: `Goblet_Squat` | Unlicense (domínio público) | OK | Foto com kettlebell: equipamento ajustado para Kettlebell (descrição cita halter como alternativa). |
| 66 | Agachamento búlgaro | Foto | 2 | free-exercise-db: `One_Leg_Barbell_Squat` | Unlicense (domínio público) | OK | Imagem trocada para 'One_Leg_Barbell_Squat' (pé de trás no banco); equipamento ajustado para Barra. |
| 67 | Afundo / passada | Foto | 2 | free-exercise-db: `Dumbbell_Lunges` | Unlicense (domínio público) | OK |  |
| 68 | Cadeira extensora | Foto | 2 | free-exercise-db: `Leg_Extensions` | Unlicense (domínio público) | OK |  |
| 69 | Sissy squat | Foto | 2 | free-exercise-db: `Weighted_Sissy_Squat` | Unlicense (domínio público) | OK |  |
| 70 | Stiff / levantamento terra romeno | Foto | 2 | free-exercise-db: `Romanian_Deadlift` | Unlicense (domínio público) | OK | Ordem das fotos invertida para início → fim. |
| 71 | Stiff com halteres | Foto | 2 | free-exercise-db: `Stiff-Legged_Dumbbell_Deadlift` | Unlicense (domínio público) | OK |  |
| 72 | Mesa flexora | Foto | 2 | free-exercise-db: `Lying_Leg_Curls` | Unlicense (domínio público) | OK |  |
| 73 | Cadeira flexora | Foto | 2 | free-exercise-db: `Seated_Leg_Curl` | Unlicense (domínio público) | OK |  |
| 74 | Good morning | Foto | 2 | free-exercise-db: `Good_Morning` | Unlicense (domínio público) | OK |  |
| 75 | Nordic curl | Foto | 2 | free-exercise-db: `Natural_Glute_Ham_Raise` | Unlicense (domínio público) | OK |  |
| 76 | Elevação pélvica (hip thrust) | Foto | 2 | free-exercise-db: `Barbell_Hip_Thrust` | Unlicense (domínio público) | OK |  |
| 77 | Ponte de glúteo | Foto | 2 | free-exercise-db: `Butt_Lift_Bridge` | Unlicense (domínio público) | OK |  |
| 78 | Glúteo na polia (coice) | Foto | 2 | free-exercise-db: `One-Legged_Cable_Kickback` | Unlicense (domínio público) | OK |  |
| 79 | Cadeira abdutora | Foto | 2 | free-exercise-db: `Thigh_Abductor` | Unlicense (domínio público) | OK |  |
| 80 | Cadeira adutora | Foto | 2 | free-exercise-db: `Thigh_Adductor` | Unlicense (domínio público) | OK |  |
| 81 | Subida no banco (step-up) | Foto | 2 | free-exercise-db: `Dumbbell_Step_Ups` | Unlicense (domínio público) | OK |  |
| 82 | Panturrilha em pé | Foto | 2 | free-exercise-db: `Standing_Calf_Raises` | Unlicense (domínio público) | OK |  |
| 83 | Panturrilha sentado | Foto | 2 | free-exercise-db: `Seated_Calf_Raise` | Unlicense (domínio público) | OK |  |
| 84 | Panturrilha no leg press | Foto | 2 | free-exercise-db: `Calf_Press_On_The_Leg_Press_Machine` | Unlicense (domínio público) | OK |  |
| 85 | Abdominal supra (crunch) | Foto | 2 | free-exercise-db: `Crunches` | Unlicense (domínio público) | OK |  |
| 86 | Abdominal na polia | Foto | 2 | free-exercise-db: `Cable_Crunch` | Unlicense (domínio público) | OK |  |
| 87 | Elevação de pernas na barra | Foto | 2 | free-exercise-db: `Hanging_Leg_Raise` | Unlicense (domínio público) | OK |  |
| 88 | Elevação de pernas deitado | Foto | 2 | free-exercise-db: `Flat_Bench_Lying_Leg_Raise` | Unlicense (domínio público) | OK | Foto em banco: descrição ajustada para 'no chão ou em um banco'. |
| 89 | Prancha | Foto | 2 | free-exercise-db: `Plank` | Unlicense (domínio público) | OK |  |
| 90 | Prancha lateral | Foto | 2 | free-exercise-db: `Side_Bridge` | Unlicense (domínio público) | OK |  |
| 91 | Roda abdominal | Foto | 2 | free-exercise-db: `Ab_Roller` | Unlicense (domínio público) | OK |  |
| 92 | Giro russo | Foto | 2 | free-exercise-db: `Russian_Twist` | Unlicense (domínio público) | OK |  |
| 93 | Abdominal máquina | Foto | 2 | free-exercise-db: `Ab_Crunch_Machine` | Unlicense (domínio público) | OK |  |
| 94 | Pallof press | Foto | 2 | free-exercise-db: `Pallof_Press` | Unlicense (domínio público) | OK |  |
| 95 | Kettlebell swing | Foto | 2 | Wikimedia Commons — Taco fleur: Kettlebell swing with arms extended upon back swing.jpg, Kettlebell swing with arms fully extended.jpg | CC BY-SA 4.0 | OK |  |
| 96 | Power clean | Foto | 2 | free-exercise-db: `Power_Clean` | Unlicense (domínio público) | OK |  |
| 97 | Thruster | Foto | 1 | Wikimedia Commons — Brad Greenlee: 2008 CrossFit Games - thrusters 2.jpg | CC BY 2.0 | OK |  |
| 98 | Burpee | Foto | 4 | Wikimedia Commons — Taco fleur: Burpee 1 Neutral Position.jpg, Burpee 2 Squat.jpg, Burpee 5 Thrust.jpg, Burpee 8 Come Upright.jpg | CC BY-SA 4.0 | OK |  |
| 99 | Esteira | Foto | 2 | free-exercise-db: `Running_Treadmill` | Unlicense (domínio público) | OK |  |
| 100 | Bicicleta ergométrica | Foto | 2 | free-exercise-db: `Bicycling_Stationary` | Unlicense (domínio público) | OK |  |
| 101 | Remo ergômetro | Foto | 2 | free-exercise-db: `Rowing_Stationary` | Unlicense (domínio público) | OK |  |
