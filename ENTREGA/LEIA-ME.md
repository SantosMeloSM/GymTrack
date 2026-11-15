# GymTrack v1.0 — Entrega

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
