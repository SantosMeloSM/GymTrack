# GymTrack v1.2 — Relatório de auditoria das imagens

Gerado por `tools/audit_images.py` a partir dos arquivos incluídos no APK (`app/src/main/assets/exercises`).

## Resumo

| Indicador | Valor |
|---|---|
| Exercícios na biblioteca padrão | **107** |
| Com pelo menos uma imagem correta | **107 (100%)** |
| Com duas ou mais posições | **107** |
| **Anatomia 3D (padrão desejado)** | **64** exercícios |
| Fotografias reais (ainda sem imagem no padrão) | 39 exercícios |
| Ilustrações originais | 4 exercícios |
| Com animação (botão play) | 107 — loop suave entre as posições reais, gerado no app; não há quadros intermediários inventados |
| Arquivos de imagem | 216 WebP (anatomia 640×640; fotos/ilustrações 720×480), total 4.1 MB |
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

- **Anatomia 3D:** imagens enviadas pelo proprietário do app (geradas por IA, segundo os nomes dos arquivos). Cada uma foi revisada; as rejeitadas estão listadas em `ENTREGA/REVISAO-IMAGENS-ENVIADAS.md`. As duas posições foram separadas, alinhadas e convertidas para WebP pelo script `tools/import_user_images.py`.
- **Fotos:** [Free Exercise DB](https://github.com/yuhonas/free-exercise-db), derivado de [wrkout/exercises.json](https://github.com/wrkout/exercises.json). Ambos são publicados sob **The Unlicense** (dedicação ao domínio público), que permite uso, modificação e distribuição, inclusive comercial, sem exigência de atribuição. O crédito é exibido mesmo assim no app (*Mais › Créditos das imagens* e no rodapé de cada exercício). Observação: a licença é declarada pelos mantenedores desses repositórios; não há como verificar de forma independente a autoria original de cada fotografia.
- **Ilustrações:** criadas para o GymTrack (vetores gerados por código e renderizados localmente) para os exercícios sem foto fiel disponível: *Elevação lateral máquina*, *Kettlebell swing*, *Thruster*, *Burpee*.
- As fotos foram redimensionadas e convertidas para WebP; imagens em retrato receberam fundo desfocado (sem cortar o executante).

## Exercício por exercício

| # | Exercício | Tipo | Posições | Origem | Licença | Status | Observações |
|---|---|---|---|---|---|---|---|
| 1 | Supino reto com barra | Anatomia 3D | 2 | Enviada pelo usuário: `demonstração_do_supino_com_barra.png` | Fornecida pelo proprietário do app | OK |  |
| 2 | Supino reto com halteres | Foto | 2 | free-exercise-db: `Dumbbell_Bench_Press` | Unlicense (domínio público) | OK |  |
| 3 | Supino inclinado com barra | Foto | 2 | free-exercise-db: `Barbell_Incline_Bench_Press_-_Medium_Grip` | Unlicense (domínio público) | OK |  |
| 4 | Supino inclinado com halteres | Anatomia 3D | 2 | Enviada pelo usuário: `supino_inclinado_com_halteres_movimento_completo.png` | Fornecida pelo proprietário do app | OK |  |
| 5 | Supino declinado | Foto | 2 | free-exercise-db: `Decline_Barbell_Bench_Press` | Unlicense (domínio público) | OK |  |
| 6 | Supino máquina | Foto | 2 | free-exercise-db: `Machine_Bench_Press` | Unlicense (domínio público) | OK |  |
| 7 | Supino inclinado no Smith | Foto | 2 | free-exercise-db: `Smith_Machine_Incline_Bench_Press` | Unlicense (domínio público) | OK |  |
| 8 | Crucifixo com halteres | Foto | 2 | free-exercise-db: `Dumbbell_Flyes` | Unlicense (domínio público) | OK |  |
| 9 | Crucifixo inclinado | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_medical_fitness_illustration_on_a_white_ba_4_batch_4.png` | Fornecida pelo proprietário do app | OK |  |
| 10 | Crucifixo máquina (peck deck) | Foto | 2 | free-exercise-db: `Butterfly` | Unlicense (domínio público) | OK |  |
| 11 | Crossover na polia | Anatomia 3D | 2 | Enviada pelo usuário: `crossover_na_polia_abertura_e_contração.png` | Fornecida pelo proprietário do app | OK |  |
| 12 | Flexão de braço | Foto | 2 | free-exercise-db: `Pushups` | Unlicense (domínio público) | OK |  |
| 13 | Paralelas (mergulho) | Anatomia 3D | 2 | Enviada pelo usuário: `wide_white_background_workout_illustration_a_clea_6_batch_6.png` | Fornecida pelo proprietário do app | OK |  |
| 14 | Puxada alta (pulldown) | Anatomia 3D | 2 | Enviada pelo usuário: `anatomia_do_pulldown_na_barra.png` | Fornecida pelo proprietário do app | OK |  |
| 15 | Puxada com triângulo | Foto | 2 | free-exercise-db: `V-Bar_Pulldown` | Unlicense (domínio público) | OK |  |
| 16 | Barra fixa | Anatomia 3D | 2 | Enviada pelo usuário: `ilustração_anatômica_do_pull_up_em_duas_fases.png` | Fornecida pelo proprietário do app | OK |  |
| 17 | Barra fixa supinada | Foto | 2 | free-exercise-db: `Chin-Up` | Unlicense (domínio público) | OK |  |
| 18 | Remada baixa | Anatomia 3D | 2 | Enviada pelo usuário: `remada_sentada_início_e_contração.png` | Fornecida pelo proprietário do app | OK |  |
| 19 | Remada curvada com barra | Anatomia 3D | 2 | Enviada pelo usuário: `remada_curvada_início_e_contração.png` | Fornecida pelo proprietário do app | OK |  |
| 20 | Remada unilateral com halter (serrote) | Anatomia 3D | 2 | Enviada pelo usuário: `remada_unilateral_com_halteres_em_destaque_muscula.png` | Fornecida pelo proprietário do app | OK |  |
| 21 | Remada cavalinho (T-bar) | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_white_background_high_detail_anatomical_f_10_batch_10.png` | Fornecida pelo proprietário do app | OK |  |
| 22 | Remada máquina | Foto | 2 | free-exercise-db: `Leverage_Iso_Row` | Unlicense (domínio público) | OK |  |
| 23 | Pulldown braços estendidos | Anatomia 3D | 2 | Enviada pelo usuário: `pulldown_com_braços_estendidos_início_e_contração.png` | Fornecida pelo proprietário do app | OK |  |
| 24 | Levantamento terra | Anatomia 3D | 2 | Enviada pelo usuário: `ilustração_anatômica_do_levantamento_terra.png` | Fornecida pelo proprietário do app | OK |  |
| 25 | Rack pull | Foto | 2 | free-exercise-db: `Rack_Pulls` | Unlicense (domínio público) | OK |  |
| 26 | Extensão lombar (banco romano) | Anatomia 3D | 2 | Enviada pelo usuário: `hiperextensão_postura_e_músculos_ativados.png` | Fornecida pelo proprietário do app | OK |  |
| 27 | Encolhimento com barra | Foto | 2 | free-exercise-db: `Barbell_Shrug` | Unlicense (domínio público) | OK |  |
| 28 | Encolhimento com halteres | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_white_background_anatomical_fitness_illust_5_batch_5.png` | Fornecida pelo proprietário do app | OK |  |
| 29 | Desenvolvimento militar com barra | Anatomia 3D | 2 | Enviada pelo usuário: `ilustração_anatômica_do_desenvolvimento_com_barra.png` | Fornecida pelo proprietário do app | OK |  |
| 30 | Desenvolvimento com halteres | Anatomia 3D | 2 | Enviada pelo usuário: `comparação_do_desenvolvimento_com_halteres.png` | Fornecida pelo proprietário do app | OK |  |
| 31 | Desenvolvimento máquina | Foto | 2 | free-exercise-db: `Machine_Shoulder_Military_Press` | Unlicense (domínio público) | OK |  |
| 32 | Desenvolvimento Arnold | Anatomia 3D | 2 | Enviada pelo usuário: `press_arnold_com_anatomia_muscular.png` | Fornecida pelo proprietário do app | OK |  |
| 33 | Elevação lateral | Anatomia 3D | 2 | Enviada pelo usuário: `elevação_lateral_com_halteres_anatomia_muscular.png` | Fornecida pelo proprietário do app | OK |  |
| 34 | Elevação lateral na polia | Anatomia 3D | 2 | Enviada pelo usuário: `elevação_lateral_no_cabo_posição_inicial_e_final.png` | Fornecida pelo proprietário do app | OK | Foto da variação sentada bilateral: descrição ajustada. |
| 35 | Elevação lateral máquina | Ilustração | 2 | Original GymTrack | Própria (GymTrack) | OK |  |
| 36 | Elevação frontal | Anatomia 3D | 2 | Enviada pelo usuário: `elevação_frontal_com_halteres_início_e_execução.png` | Fornecida pelo proprietário do app | OK |  |
| 37 | Crucifixo invertido | Foto | 2 | free-exercise-db: `Seated_Bent-Over_Rear_Delt_Raise` | Unlicense (domínio público) | OK |  |
| 38 | Crucifixo invertido máquina | Anatomia 3D | 2 | Enviada pelo usuário: `wide_clean_instructional_fitness_illustration_on_2_batch_2.png` | Fornecida pelo proprietário do app | OK |  |
| 39 | Face pull | Anatomia 3D | 2 | Enviada pelo usuário: `execução_do_face_pull_com_anatomia_muscular.png` | Fornecida pelo proprietário do app | OK |  |
| 40 | Remada alta | Foto | 2 | free-exercise-db: `Upright_Barbell_Row` | Unlicense (domínio público) | OK | Ordem das fotos invertida para início → fim. |
| 41 | Rosca direta com barra | Anatomia 3D | 2 | Enviada pelo usuário: `rosca_direta_posição_inicial_e_contraída.png` | Fornecida pelo proprietário do app | OK |  |
| 42 | Rosca direta barra W | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_white_background_anatomical_fitness_illust_7_batch_7.png` | Fornecida pelo proprietário do app | OK |  |
| 43 | Rosca alternada com halteres | Foto | 2 | free-exercise-db: `Dumbbell_Alternate_Bicep_Curl` | Unlicense (domínio público) | OK |  |
| 44 | Rosca martelo | Anatomia 3D | 2 | Enviada pelo usuário: `ilustração_anatômica_da_rosca_martelo.png` | Fornecida pelo proprietário do app | OK |  |
| 45 | Rosca inclinada | Anatomia 3D | 2 | Enviada pelo usuário: `wide_clean_white_background_anatomical_fitness_ill_7_batch_7.png` | Fornecida pelo proprietário do app | OK |  |
| 46 | Rosca Scott | Anatomia 3D | 2 | Enviada pelo usuário: `rosca_scott_posições_inicial_e_contraída.png` | Fornecida pelo proprietário do app | OK |  |
| 47 | Rosca concentrada | Anatomia 3D | 2 | Enviada pelo usuário: `rosca_concentrada_início_e_contração.png` | Fornecida pelo proprietário do app | OK |  |
| 48 | Rosca na polia | Foto | 2 | free-exercise-db: `Standing_Biceps_Cable_Curl` | Unlicense (domínio público) | OK |  |
| 49 | Tríceps pulley | Anatomia 3D | 2 | Enviada pelo usuário: `pushdown_de_tríceps_posições_inicial_e_final.png` | Fornecida pelo proprietário do app | OK |  |
| 50 | Tríceps corda | Anatomia 3D | 2 | Enviada pelo usuário: `ilustração_anatômica_do_tríceps_na_polia.png` | Fornecida pelo proprietário do app | OK |  |
| 51 | Tríceps francês na polia | Foto | 2 | free-exercise-db: `Cable_Rope_Overhead_Triceps_Extension` | Unlicense (domínio público) | OK |  |
| 52 | Tríceps francês com halter | Anatomia 3D | 2 | Enviada pelo usuário: `extensão_de_tríceps_sentado_com_halter.png` | Fornecida pelo proprietário do app | OK | Foto em pé: descrição ajustada para 'em pé ou sentado'. |
| 53 | Tríceps testa | Anatomia 3D | 2 | Enviada pelo usuário: `wide_clean_gym_illustration_anatomy_exercise_di_6_batch_6.png` | Fornecida pelo proprietário do app | OK |  |
| 54 | Supino fechado | Foto | 2 | free-exercise-db: `Close-Grip_Barbell_Bench_Press` | Unlicense (domínio público) | OK |  |
| 55 | Mergulho no banco | Foto | 2 | free-exercise-db: `Bench_Dips` | Unlicense (domínio público) | OK |  |
| 56 | Tríceps coice | Anatomia 3D | 2 | Enviada pelo usuário: `anatomia_do_tríceps_coice_com_halter.png` | Fornecida pelo proprietário do app | OK |  |
| 57 | Rosca de punho | Foto | 2 | free-exercise-db: `Seated_Palm-Up_Barbell_Wrist_Curl` | Unlicense (domínio público) | OK |  |
| 58 | Rosca inversa | Foto | 2 | free-exercise-db: `Reverse_Barbell_Curl` | Unlicense (domínio público) | OK | Foto com barra reta: equipamento ajustado para Barra. |
| 59 | Caminhada do fazendeiro | Foto | 2 | free-exercise-db: `Farmers_Walk` | Unlicense (domínio público) | OK |  |
| 60 | Agachamento livre | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_high_detail_anatomical_fitness_illustrati_1_batch_1.png` | Fornecida pelo proprietário do app | OK |  |
| 61 | Agachamento frontal | Anatomia 3D | 2 | Enviada pelo usuário: `anatomia_do_agachamento_frontal.png` | Fornecida pelo proprietário do app | OK |  |
| 62 | Agachamento no Smith | Anatomia 3D | 2 | Enviada pelo usuário: `wide_clean_white_background_fitness_illustration_9_batch_9.png` | Fornecida pelo proprietário do app | OK | Ordem das fotos invertida para início → fim. |
| 63 | Agachamento hack | Anatomia 3D | 2 | Enviada pelo usuário: `comparação_do_hack_squat_em_duas_posições.png` | Fornecida pelo proprietário do app | OK |  |
| 64 | Leg press 45° | Anatomia 3D | 2 | Enviada pelo usuário: `ilustração_anatômica_do_leg_press_45.png` | Fornecida pelo proprietário do app | OK |  |
| 65 | Agachamento goblet | Foto | 2 | free-exercise-db: `Goblet_Squat` | Unlicense (domínio público) | OK | Foto com kettlebell: equipamento ajustado para Kettlebell (descrição cita halter como alternativa). |
| 66 | Agachamento búlgaro | Anatomia 3D | 2 | Enviada pelo usuário: `agachamento_búlgaro_início_e_execução.png` | Fornecida pelo proprietário do app | OK | Imagem trocada para 'One_Leg_Barbell_Squat' (pé de trás no banco); equipamento ajustado para Barra. |
| 67 | Afundo / passada | Anatomia 3D | 2 | Enviada pelo usuário: `avanço_com_halteres_músculos_ativados.png` | Fornecida pelo proprietário do app | OK |  |
| 68 | Cadeira extensora | Anatomia 3D | 2 | Enviada pelo usuário: `extensão_de_pernas_início_e_contração.png` | Fornecida pelo proprietário do app | OK |  |
| 69 | Sissy squat | Foto | 2 | free-exercise-db: `Weighted_Sissy_Squat` | Unlicense (domínio público) | OK |  |
| 70 | Stiff / levantamento terra romeno | Anatomia 3D | 2 | Enviada pelo usuário: `wide_clean_instructional_fitness_illustration_on_w_2_batch_2.png` | Fornecida pelo proprietário do app | OK | Ordem das fotos invertida para início → fim. |
| 71 | Stiff com halteres | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_white_background_anatomical_fitness_illust_3_batch_3.png` | Fornecida pelo proprietário do app | OK |  |
| 72 | Mesa flexora | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_high_detail_fitness_anatomy_illustration_1_batch_1.png` | Fornecida pelo proprietário do app | OK |  |
| 73 | Cadeira flexora | Anatomia 3D | 2 | Enviada pelo usuário: `rosca_femoral_sentada_início_e_contração.png` | Fornecida pelo proprietário do app | OK |  |
| 74 | Good morning | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_white_background_fitness_anatomy_illustrat_8_batch_8.png` | Fornecida pelo proprietário do app | OK |  |
| 75 | Nordic curl | Anatomia 3D | 2 | Enviada pelo usuário: `curl_nórdico_foco_em_posteriores_da_coxa.png` | Fornecida pelo proprietário do app | OK |  |
| 76 | Elevação pélvica (hip thrust) | Anatomia 3D | 2 | Enviada pelo usuário: `ilustração_anatômica_do_hip_thrust_em_duas_posiçõe.png` | Fornecida pelo proprietário do app | OK |  |
| 77 | Ponte de glúteo | Foto | 2 | free-exercise-db: `Butt_Lift_Bridge` | Unlicense (domínio público) | OK |  |
| 78 | Glúteo na polia (coice) | Anatomia 3D | 2 | Enviada pelo usuário: `wide_fitness_illustration_diagram_scene_on_a_white_7_batch_7.png` | Fornecida pelo proprietário do app | OK |  |
| 79 | Cadeira abdutora | Anatomia 3D | 2 | Enviada pelo usuário: `abdução_de_quadril_na_máquina_início_e_final.png` | Fornecida pelo proprietário do app | OK |  |
| 80 | Cadeira adutora | Anatomia 3D | 2 | Enviada pelo usuário: `addução_de_quadril_início_e_contração.png` | Fornecida pelo proprietário do app | OK |  |
| 81 | Subida no banco (step-up) | Anatomia 3D | 2 | Enviada pelo usuário: `step_up_com_halteres_e_anatomia_muscular.png` | Fornecida pelo proprietário do app | OK |  |
| 82 | Panturrilha em pé | Anatomia 3D | 2 | Enviada pelo usuário: `elevação_de_panturrilha_na_máquina.png` | Fornecida pelo proprietário do app | OK |  |
| 83 | Panturrilha sentado | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_high_detail_instructional_fitness_illustr_8_batch_8.png` | Fornecida pelo proprietário do app | OK |  |
| 84 | Panturrilha no leg press | Foto | 2 | free-exercise-db: `Calf_Press_On_The_Leg_Press_Machine` | Unlicense (domínio público) | OK |  |
| 85 | Abdominal supra (crunch) | Anatomia 3D | 2 | Enviada pelo usuário: `ilustração_anatômica_do_abdominal_crunch.png` | Fornecida pelo proprietário do app | OK |  |
| 86 | Abdominal na polia | Anatomia 3D | 2 | Enviada pelo usuário: `demonstração_do_crunch_abdominal_na_polia.png` | Fornecida pelo proprietário do app | OK |  |
| 87 | Elevação de pernas na barra | Foto | 2 | free-exercise-db: `Hanging_Leg_Raise` | Unlicense (domínio público) | OK |  |
| 88 | Elevação de pernas deitado | Foto | 2 | free-exercise-db: `Flat_Bench_Lying_Leg_Raise` | Unlicense (domínio público) | OK | Foto em banco: descrição ajustada para 'no chão ou em um banco'. |
| 89 | Prancha | Foto | 2 | free-exercise-db: `Plank` | Unlicense (domínio público) | OK |  |
| 90 | Prancha lateral | Foto | 2 | free-exercise-db: `Side_Bridge` | Unlicense (domínio público) | OK |  |
| 91 | Roda abdominal | Foto | 2 | free-exercise-db: `Ab_Roller` | Unlicense (domínio público) | OK |  |
| 92 | Giro russo | Foto | 2 | free-exercise-db: `Russian_Twist` | Unlicense (domínio público) | OK |  |
| 93 | Abdominal máquina | Anatomia 3D | 2 | Enviada pelo usuário: `demonstração_do_abdominal_na_máquina.png` | Fornecida pelo proprietário do app | OK |  |
| 94 | Pallof press | Foto | 2 | free-exercise-db: `Pallof_Press` | Unlicense (domínio público) | OK |  |
| 95 | Kettlebell swing | Ilustração | 2 | Original GymTrack | Própria (GymTrack) | OK |  |
| 96 | Power clean | Foto | 2 | free-exercise-db: `Power_Clean` | Unlicense (domínio público) | OK |  |
| 97 | Thruster | Ilustração | 2 | Original GymTrack | Própria (GymTrack) | OK |  |
| 98 | Burpee | Ilustração | 4 | Original GymTrack | Própria (GymTrack) | OK |  |
| 99 | Agachamento sumô com halter | Anatomia 3D | 2 | Enviada pelo usuário: `agachamento_sumô_com_halter_destacando_músculos.png` | Fornecida pelo proprietário do app | OK |  |
| 100 | Panturrilha em pé com barra | Anatomia 3D | 2 | Enviada pelo usuário: `elevação_de_panturrilha_com_barra.png` | Fornecida pelo proprietário do app | OK |  |
| 101 | Rosca spider (barra W) | Anatomia 3D | 2 | Enviada pelo usuário: `rosca_spider_com_barra_ez_em_duas_fases.png` | Fornecida pelo proprietário do app | OK |  |
| 102 | Pullover com halter | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_white_background_instructional_fitness_ill_4_batch_4.png` | Fornecida pelo proprietário do app | OK |  |
| 103 | Puxada unilateral na polia | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_white_background_instructional_fitness_med_8_batch_8.png` | Fornecida pelo proprietário do app | OK |  |
| 104 | Tríceps francês unilateral com halter | Anatomia 3D | 2 | Enviada pelo usuário: `a_clean_white_background_high_contrast_gym_anatom_6_batch_6.png` | Fornecida pelo proprietário do app | OK |  |
| 105 | Esteira | Foto | 2 | free-exercise-db: `Running_Treadmill` | Unlicense (domínio público) | OK |  |
| 106 | Bicicleta ergométrica | Foto | 2 | free-exercise-db: `Bicycling_Stationary` | Unlicense (domínio público) | OK |  |
| 107 | Remo ergômetro | Foto | 2 | free-exercise-db: `Rowing_Stationary` | Unlicense (domínio público) | OK |  |
