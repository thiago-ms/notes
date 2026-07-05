# Notas (Android)

App Android **multi-módulo** de anotações rápidas com **auto captura do clipboard** e
**backup no Google Drive** (via SAF, idêntico ao WatchUp).

- **Linguagem/UI:** Kotlin + Jetpack Compose (Material 3)
- **Navegação:** Navigation Compose (2 abas + FAB + engrenagem)
- **Persistência:** Room (local) · **Backup:** Storage Access Framework + WorkManager
- **Build:** 100% via Docker (não precisa instalar JDK/Gradle/Android SDK no host)
- **Package base:** `br.com.notes` · minSdk 26 · targetSdk 35 · JDK 17

## Telas

- **Nota** — o bloco atual, com nome e conteúdo editáveis (autosave) e datas de
  criação/última atualização. Todo bloco nasce salvo.
- **Blocos** — lista de todos os blocos (inclui o atual), ordenada por atualização;
  abrir, renomear e apagar. **FAB +** cria uma nota nova.
- **Configurações** (engrenagem, em ambas as abas):
  - **Auto captura** — toggle (só em memória: desliga ao fechar o app), com dois modos:
    - **Segundo plano (acessibilidade)** — captura tudo que você copia automaticamente,
      mesmo em outros apps. Requer habilitar o serviço de acessibilidade do Notas.
    - **Botão flutuante** — um botão fica sobre a tela; ao copiar algo ele acende e você
      toca para colar (leitura no toque, via Activity transparente instantânea). Sem
      acessibilidade; requer permissão de sobreposição + uma notificação fixa. Arrastável.
  - **Regras de destino** — se o texto copiado contém um termo cadastrado (ex.:
    `instagram.com`), a captura vai para um bloco com esse nome (criado sob demanda).
  - **Backup no Drive** — escolhe uma pasta (pode ser do Drive) e grava/lê
    `backup.json`; backup manual + automático diário; restaurar/apagar.

Cada captura mostra um toast bem rápido (Toast curto cancelado em ~700ms).

> **Limitação do Android (esperada):** desde o Android 10 o clipboard só é legível
> com o app em primeiro plano. Por isso a auto captura funciona **só com o app
> aberto** — que é justamente o comportamento pedido.

## Arquitetura de módulos

```
:app              # MainActivity (capturador de clipboard), NavHost, bottom nav + FAB
:core:ui          # tema (NotesTheme) + componentes compartilhados
:core:data        # Room (Bloco, RegraCaptura), repositório, backup serializer/prefs, AutoCaptureState
:feature:notepad  # aba Nota (bloco atual)
:feature:blocos   # aba Blocos (lista)
:feature:settings # Configurações (auto captura + regras + backup SAF)
```

## Build

```bash
make image     # 1ª vez: imagem Docker de build
make wrapper   # 1ª vez: Gradle wrapper
make apk       # APK debug em dist/notes-<versão>-debug.apk
make dist-all  # debug + release em dist/
```

Instalar no aparelho: `./adb.sh build-install` (debug) ou `./adb.sh build-install-release`.
Distribuir por HTTP local: `./server.sh` (serve `dist/`).

## Conformidade

App pessoal, local-only. O backup usa SAF (a sincronização é do provider do Drive; o
app não faz rede direta nem usa OAuth/Drive API). Não trata dados de clientes.
