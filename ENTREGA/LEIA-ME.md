# GymTrack — Entrega

## Versão atual: v1.3 (retorno ao visual da v1.1)

| Item | Valor |
|---|---|
| Arquivo | `GymTrack-v1.3.apk` |
| Tipo | Release assinado (v2). otimizado com R8 |
| Tamanho | 7.076.292 bytes |
| SHA-256 do APK | `dbc62ebfe718e6ad251b024a912144d3a1544ed0229cdaabaf151f7b470811fd` |
| SHA-256 do certificado | `008e77bbc0e9af9fff2326dff50d434dc70b03dc984ffb71878a04214ac9a31c` (o mesmo de todas as versões) |
| Pacote | `com.gymtrack.app` · versão 1.3 (versionCode 4) · Android 8.0+ |

A v1.3 desfaz completamente a v1.2 (ilustrações anatômicas): o app volta a ser exatamente como na v1.1.
com as fotos de demonstração (97 exercícios) e as 4 ilustrações. total de 101 exercícios. O número de versão é
maior apenas para que possa ser instalada por cima da v1.2. sem desinstalar e sem perder dados.

- Os 6 exercícios que só existiam na v1.2 são removidos ao abrir o app. Se algum já tiver sido usado em treino.
  ficha ou meta. ele é mantido como exercício personalizado para não perder o histórico.
- Testes: 29 automatizados passando; no emulador. v1.3 instalada por cima da v1.2 com os dados preservados
  e a biblioteca de volta às fotos.

---

## Versão v1.1 (biblioteca visual)

| Item | Valor |
|---|---|
| Arquivo | `GymTrack-v1.1.apk` |
| Tipo | **Release assinado** (APK Signature Scheme v2), otimizado com R8 |
| Tamanho | 7.076.292 bytes (≈ 6,7 MB) |
| SHA-256 do APK | `150fffc50a3c35eb3fd396e397a9b6f5b6d03fb825cdf890d6efc46ecdd4d303` |
| SHA-256 do certificado | `008e77bbc0e9af9fff2326dff50d434dc70b03dc984ffb71878a04214ac9a31c` (o mesmo da v1.0) |
| Pacote | `com.gymtrack.app` · versão 1.1 (versionCode 2) |
| Android mínimo | **Android 8.0 (API 26)** |

**Atualização:** instale a v1.1 por cima da v1.0, sem desinstalar. O banco de dados é migrado
automaticamente (versão 1 → 2, apenas adiciona colunas) e todos os treinos, fichas, medidas e fotos são mantidos.

### Novidades da v1.1
- **101 de 101 exercícios com imagem** (relatório completo em `RELATORIO-IMAGENS.md`):
  - 97 com **fotos reais** da posição inicial e da final;
  - 4 com **ilustrações originais** (elevação lateral máquina, burpee com 4 posições, thruster, kettlebell swing),
    pois não havia foto livre fiel a esses exercícios.
- Biblioteca em cards com miniatura, nome, grupo muscular e equipamento.
- Tela de detalhes com:
  - imagem grande, deslizar entre as posições e **reprodução sob demanda**;
  - **tela cheia com zoom** (pinça);
  - execução, músculos trabalhados, orientações técnicas, **erros comuns** e histórico.
- Durante o treino: miniatura ao lado de cada exercício. O toque abre a demonstração por cima da sessão,
  sem perder os valores digitados nem parar os cronômetros; "Voltar às séries" fecha.
- Exercícios personalizados: imagem inicial e final pela **galeria ou câmera**, com opção de trocar ou remover.
  As imagens ficam salvas no aparelho e entram no backup `.zip`.
- Tudo funciona **offline**: as imagens estão dentro do APK, em WebP 720×480.
- Créditos das imagens em *Mais › Créditos das imagens*.

### Testes da v1.1
- **28 testes automatizados, todos passando**. Entre eles:
  - auditoria de mídia (cada arquivo existe, é WebP, decodifica, tem 720×480 e não se repete);
  - **migração real do banco v1 → v2** com dados;
  - atualização da biblioteca preservando edições do usuário;
  - backup com imagens personalizadas;
  - teste de interface da biblioteca, dos detalhes, da tela cheia e da folha de demonstração no treino.
- **Emulador Android 10, APK release, instalado por cima da v1.0 com dados reais:**
  - o treino antigo foi preservado;
  - as miniaturas aparecem na biblioteca;
  - os detalhes mostram fotos, controles e erros comuns;
  - a ilustração de 4 posições do burpee aparece;
  - no treino, a demonstração abre e o valor digitado é mantido.
- Não testado em aparelho físico. A troca de imagem pela **câmera** depende do app de câmera do celular
  e não pôde ser verificada no emulador sem câmera; a escolha pela galeria usa o seletor de fotos do sistema.

---

## Versão anterior: v1.0

| Item | Valor |
|---|---|
| Arquivo | `GymTrack-v1.0.apk` |
| Tipo | **Release assinado** (APK Signature Scheme v2), otimizado com R8 |
| Tamanho | 2.160.578 bytes (≈ 2,1 MB) |
| SHA-256 do APK | `523fb7695ecd7274e75832a58c4d42b5a2edd5e1c27e8035678133e4a95769f5` |
| SHA-256 do certificado | `008e77bbc0e9af9fff2326dff50d434dc70b03dc984ffb71878a04214ac9a31c` |
| Pacote | `com.gymtrack.app` · versão 1.0 (versionCode 1) |
| Android mínimo | **Android 8.0 (API 26)** |
| Android alvo | Android 15 (API 35) |

Conferir o hash: `sha256sum GymTrack-v1.0.apk` (Linux/macOS) ou
`Get-FileHash GymTrack-v1.0.apk -Algorithm SHA256` (Windows PowerShell).

## Como instalar

1. Copie `GymTrack-v1.0.apk` para o celular (cabo USB, Google Drive, e-mail, WhatsApp para você mesmo etc.).
2. No celular, abra o arquivo pelo app **Arquivos** / **Downloads**.
3. Se o Android pedir, permita **"Instalar apps desconhecidos"** para o app que você usou para abrir o arquivo
   (Configurações › Apps › [app] › Instalar apps desconhecidos) e volte.
4. Toque em **Instalar** e depois em **Abrir**.
5. O Play Protect pode avisar que o app não é conhecido (ele não está na Play Store). Toque em
   "Mais detalhes › Instalar mesmo assim".

Via computador com ADB: `adb install GymTrack-v1.0.apk`

## Primeiros passos

1. **Treinos › ✨ (Modelos)** para importar PPL/ABC/ABCD/Upper-Lower/Full Body, ou **Nova ficha** para criar do zero.
2. Na tela inicial, **Iniciar treino**. Digite carga e repetições e toque no **✓** para concluir a série: o descanso começa sozinho.
3. Na próxima vez, as séries já vêm preenchidas com o treino anterior: muitas vezes basta tocar no ✓.
4. **Mais › Backup e exportação** para guardar um `.zip` com todos os seus dados fora do aparelho.

## Testes realizados

- **23 testes automatizados, todos passando**:
  - 10 de domínio (JVM): volume, 1RM, recordes, consistência, formatação e PDF.
  - 10 de banco/repositório (Robolectric + Room): fluxo completo de treino, pré-preenchimento, descanso,
    pausa, finalização, versões de ficha, duplicação, backup zip ida e volta, CSV/PDF e lembretes.
  - 3 de interface (Compose): criar ficha → adicionar exercício → iniciar → registrar séries → descanso →
    pausar → concluir → resumo → progresso; importar modelo e iniciar pela tela inicial; abertura de todas as telas.
- **Emulador Android 10 (API 29)**, rodando este APK release: instalação, abertura, navegação por todas
  as abas, importação de modelo, registro de série com um toque e cronômetro de descanso, resumo e progresso.
  Também testado: **app encerrado à força no meio do treino** (sessão e série recuperadas ao reabrir) e
  **instalação de atualização** sobre a versão anterior sem perda de dados.
- Não testado em aparelho físico. O emulador rodou sem aceleração de hardware, então a velocidade
  observada nele não representa a de um celular real.

## Observações

- Funciona 100% offline, sem login e sem assinatura. Os dados ficam apenas no aparelho.
- No Android 13+ o app pede permissão de notificação ao iniciar o primeiro treino, para avisar o fim do descanso.
- Atualizações futuras precisam ser assinadas com a **mesma chave**. A chave não está no repositório (ele é
  público); ela foi entregue separadamente. Guarde-a em local seguro.
