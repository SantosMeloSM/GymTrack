# GymTrack

Aplicativo Android de musculação, 100% offline, sem login e sem assinatura.

**APK pronto para instalar:** [`ENTREGA/GymTrack-v1.2.apk`](ENTREGA/) — veja `ENTREGA/LEIA-ME.md`.

## Funcionalidades

- **Fichas de treino ilimitadas**: divisões A/B/C/D, Push/Pull/Legs, personalizadas; duplicar, reordenar,
  séries, faixa de repetições, carga planejada, aquecimentos, descanso por exercício, metas e observações;
  **versões** salvas e restauráveis; **modelos editáveis** (PPL, ABC, ABCD, Upper/Lower, Full Body).
- **Biblioteca com 107 exercícios, todos com imagem**: 64 com ilustrações anatômicas 3D (posições inicial e
  final, músculos em destaque), os demais com fotos em domínio público ou ilustrações originais. Grupo
  principal, secundários, equipamento, execução, orientações e erros comuns. Play = demonstração em loop;
  tela cheia com zoom; demonstração durante o treino. Novas ilustrações: `tools/import_user_images.py`.
  Busca sem acentos, filtros, favoritos e exercícios personalizados com fotos da galeria ou câmera.
  Auditoria: `python3 tools/audit_images.py` (gera `ENTREGA/RELATORIO-IMAGENS.md`).
- **Tela de treino rápida**: séries pré-preenchidas com o último treino (ou a carga planejada), coluna
  "Anterior" (toque para copiar), conclusão com **um toque**, aquecimentos, séries extras, trocar/mover/remover
  exercícios, RPE/RIR opcional, pausar/continuar, cronômetro da sessão. Cada alteração é gravada no banco
  imediatamente (WAL), então fechar o app não perde dados.
- **Descanso**: inicia automaticamente ao concluir a série, tempo por exercício, −15 s / +15 s / pular,
  vibração e notificação local (alarme exato), notificação contínua com cronômetro quando o app está em segundo plano.
- **Histórico e evolução**: sessões, calendário mensal, comparação entre treinos, recordes pessoais
  (carga, reps, volume, 1RM estimado), gráficos interativos (toque/arraste), volume semanal, séries por
  grupo muscular, frequência semanal com meta, sequência, aderência, dias ativos.
- **1RM estimado** (Epley ou Brzycki), somente para séries de até 12 repetições, sempre identificado como estimativa.
- **Evolução corporal**: peso, altura, cintura, peitoral, braços, coxas, quadril, panturrilha, ombros,
  pescoço, antebraço e medidas livres; comparação por período e gráficos; fotos de evolução (armazenadas no
  app) com comparação lado a lado. Nenhum percentual de gordura ou diagnóstico é inferido.
- **Backup**: exportação/importação completa em `.zip` (inclui fotos), backups locais automáticos após cada
  treino e cópia de segurança antes de cada importação/restauração. **Relatórios PDF e CSV.**
- Metas de desempenho, lembretes por dia da semana, tema escuro/claro/sistema.

## Arquitetura

| Módulo | Conteúdo |
|---|---|
| `:domain` | Kotlin puro: cálculos (volume, recordes, 1RM, consistência), formatação e gerador de PDF. Testado em JVM. |
| `:app` | Jetpack Compose + Material 3 personalizado, Room, DataStore, ViewModel, Coroutines/Flow, navegação. |

Pacotes do `:app`: `data/db` (entidades e DAOs Room), `data/repo` (repositórios, backup, relatórios, análises),
`data/seed` (biblioteca e modelos), `notify` (alarmes, notificações, lembretes), `ui` (tema, componentes,
gráficos em Canvas, telas).

Os gráficos são desenhados em Canvas do Compose (sem dependência externa), com interação por toque/arraste.

## Build

Requisitos: JDK 17+ e Android SDK (platform 35, build-tools 35).

```bash
./gradlew :domain:test :app:testDebugUnitTest   # testes
./gradlew :app:assembleDebug                    # APK de desenvolvimento
./gradlew :app:assembleRelease                  # APK release (requer keystore.properties)
```

Para assinar o release, crie `keystore.properties` na raiz (não versionado):

```
storeFile=signing/gymtrack-release.jks
storePassword=...
keyAlias=gymtrack
keyPassword=...
```

Os testes do `:app` usam Robolectric. Em ambientes sem acesso à rede durante os testes, coloque os jars
`android-all-instrumented` (API 34 e 35) em `.robolectric/` para rodar offline.
