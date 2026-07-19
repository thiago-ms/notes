# Notes — Backlog de ajustes, melhorias e bugs

Lista numerada de itens para resolver **um a um**. A numeração é estável: escolha
um número e a gente conversa e executa. `Status` começa tudo em `pendente`.

Legenda de tipo: 🐛 bug · ✨ melhoria · 🔧 ajuste

| # | Tipo | Título | Status |
|---|------|--------|--------|
| 1 | 🔧 | Ícone do app com cara de caderno / bloco de notas | feito (v1.3, validar no device) |
| 2 | ✨ | Botão na lista para mesclar blocos de mesmo nome | feito (v1.6, validar no device) |
| 3 | ✨ | Ícone no bloco para remover linhas duplicadas (com confirmação) | feito (v1.5, validar no device) |
| 4 | ✨ | Ícone no bloco para apagar o bloco (com confirmação) | feito (v1.4, validar no device) |

## Histórico de entregas

Mapa item → versão em que foi entregue (APK debug+release em `dist/` e release no
hub `../.dist/`). **Manter atualizado a cada entrega:** ao concluir um item, subir a
linha correspondente com a versão (`versionName`) usada no build.

| Versão | Itens | Resumo |
|--------|-------|--------|
| 1.3 | 1 | Novo ícone: folha/bloco de notas (página branca com canto dobrado + linhas) no fundo roxo |
| 1.4 | 4 | Botão de apagar o bloco atual na aba Nota (com confirmação; recai no bloco mais recente ou cria um novo) |
| 1.5 | 3 | Botão de remover linhas duplicadas na aba Nota (com confirmação; mantém 1ª ocorrência, preserva ordem, ignora linhas em branco) |
| 1.6 | 2 | Mesclar blocos de mesmo nome na lista (ação por item só em nomes repetidos; novo bloco "(mesclado)", origem por nome+data de criação, ordem por criação, originais preservados) |

Versão atual em `app/build.gradle.kts`: **1.6** (`versionCode` 7).

---

## 1. 🔧 Ícone do app com cara de caderno / bloco de notas
Trocar o ícone genérico atual (losango branco sobre fundo roxo) por algo que
remeta a **caderno, bloco de notas ou folha**.

- Arquivos: `app/src/main/res/drawable/ic_launcher_foreground.xml` (o símbolo, hoje
  um losango em `M54,32 L64,50 L54,68 L44,50 Z`) e `ic_launcher_background.xml`
  (fundo chapado `#5B4BE8`). Ícone adaptativo já plugado nos
  `mipmap-anydpi-v26/ic_launcher*.xml`.
- Desenhar o novo símbolo em vetor (`VectorDrawable`), centralizado na safe zone do
  ícone adaptativo (~66dp úteis dentro de 108dp). Ideias: folha com linhas de
  caderno, espiral lateral, ou canto dobrado.
- Manter a paleta atual (fundo roxo `#5B4BE8`, símbolo branco) salvo pedido em
  contrário.

## 2. ✨ Botão na lista para mesclar blocos de mesmo nome
Na aba **Blocos** ([BlocosScreen.kt](../feature/blocos/src/main/kotlin/br/com/notes/feature/blocos/BlocosScreen.kt)),
oferecer uma ação de **mesclar (merge)** blocos que têm o **mesmo nome**.

- Resultado: cria um **bloco novo** com o conteúdo de todos os blocos daquele nome
  concatenado. **Não apaga os originais.**
- No bloco mesclado, cada trecho vindo de outro bloco deve ter, **uma linha acima**,
  um identificador de origem (ex.: `--- de: <nome do bloco> ---`). Como todos têm o
  mesmo nome, provavelmente vale identificar por origem + data/ordem para
  desambiguar (a definir na conversa do item).
- Decisões tomadas (entregue na v1.6):
  - **Gatilho**: ação por item — o ícone de mesclar só aparece em blocos cujo nome
    se repete e junta **todos** com aquele nome.
  - **Nome do bloco novo**: sufixo `<nome> (mesclado)`.
  - **Identificador de origem**: `--- de: <nome> (criado dd/MM/yyyy HH:mm) ---`
    (data desambigua os blocos de mesmo nome).
  - **Ordem de concatenação**: por data de criação, mais antigo primeiro.
  - **Confirmação**: adicionado um `AlertDialog` (mesmo sendo não destrutivo) que
    mostra quantos blocos serão juntados.
- Precisa de método novo no repositório/DAO
  ([NotesRepository.kt](../core/data/src/main/kotlin/br/com/notes/core/data/repo/NotesRepository.kt),
  [NotesDao.kt](../core/data/src/main/kotlin/br/com/notes/core/data/db/NotesDao.kt)) —
  hoje `buscarPorNome` retorna só o primeiro (`LIMIT 1`); vai precisar buscar
  **todos** de um nome.

## 3. ✨ Ícone no bloco para remover linhas duplicadas (com confirmação)
Na aba **Nota** ([NotepadScreen.kt](../feature/notepad/src/main/kotlin/br/com/notes/feature/notepad/NotepadScreen.kt)),
ao lado do botão **copiar** (canto inferior, `Icons.Filled.ContentCopy`), adicionar
um ícone que **remove linhas duplicadas** do conteúdo do bloco atual.

- **Exige confirmação do usuário** (`AlertDialog`) antes de aplicar — a ação altera
  o conteúdo salvo.
- Pontos a decidir na execução:
  - Manter a **primeira ocorrência** de cada linha e remover as repetições
    seguintes (preservando a ordem)?
  - Comparação **exata** ou ignorando espaços nas pontas / maiúsculas?
  - O que fazer com **linhas em branco** (as capturas separam blocos com linha
    vazia) — provavelmente **não** deduplicar linhas vazias.
- Grava via `repo.atualizarConteudo(id, ...)` (já existente).

## 4. ✨ Ícone no bloco para apagar o bloco (com confirmação)
Ainda na aba **Nota**, ao lado do ícone de **remover duplicadas** (item 3),
adicionar um ícone para **apagar o bloco atual** (`Icons.Filled.Delete`).

- **Exige confirmação do usuário** (`AlertDialog`), no mesmo espírito do apagar já
  existente na lista de blocos (`repo.remover(bloco)`).
- Após apagar, o app deve cair no **bloco mais recente** (o `remover` do repositório
  já reatribui `blocoAtualId` para o mais recente) — validar o comportamento da tela
  quando o bloco atual deixa de existir (recompor para o novo bloco atual).
