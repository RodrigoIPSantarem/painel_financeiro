# Painel Financeiro no F-Droid — guia passo a passo

O F-Droid não aceita um APK: compila a app a partir do código-fonte público e assina-a com a chave dele.
Por isso o trabalho tem duas partes: preparar o **repositório da app** (GitHub) e enviar um **ficheiro de
metadados** para o repositório do F-Droid (`fdroiddata`, no GitLab).

## O que está neste pacote

| Pasta / ficheiro | Para quê |
|---|---|
| `preparar_fdroid.ps1` | Lê o teu projeto, verifica os requisitos, copia os ficheiros e gera os metadados |
| `1_projeto/LICENSE` | Licença GPL-3.0 (texto oficial da FSF), para a raiz do projeto |
| `1_projeto/fastlane/` | Textos da loja em `en-US` (obrigatório) e `pt-PT` |
| `2_fdroiddata/modelo.yml` | Modelo dos metadados (o script preenche-o) |
| `2_fdroiddata/metadata/` | Onde o script grava `pt.painelfinanceiro.app.yml` |
| `PEDIDO_MERGE_REQUEST.md` | Texto para colar no pedido de integração |

O ficheiro de metadados foi validado com as ferramentas oficiais do F-Droid (`fdroid lint` e `fdroid rewritemeta`).

## 0. Antes de começar

- **Licença:** o pacote usa GPL-3.0 (`GPL-3.0-or-later`). Se o projeto é de grupo, todos os autores têm de concordar.
  Para usar MIT, substitui o `LICENSE` pelo texto MIT e muda `License:` para `MIT` no `modelo.yml`.
- **Unidade curricular:** confirma que podes publicar o trabalho.
- **Textos da loja:** escrevi-os a partir do que se vê no projeto (botão «Já investi», totais, metas, base de dados
  local). Revê `fastlane/metadata/android/*/full_description.txt` e acrescenta o que a app faz.
- **GitHub:** o projeto tem de estar num repositório **público**.

## 1. Correr o script

Na pasta deste pacote, abre o PowerShell e corre:

```powershell
Unblock-File .\preparar_fdroid.ps1
powershell -ExecutionPolicy Bypass -File .\preparar_fdroid.ps1 -Projeto "C:\Users\rodri\Desktop\PDM\painel-financeiro-android" -Autor "O teu nome ou alcunha"
```

O script mostra `[OK]`, `[AVISO]` e `[PROBLEMA]`:

- **PROBLEMA** impede a entrada no F-Droid (Firebase, Google Play Services, anúncios, ficheiros `.jar`/`.aar`
  pré-compilados, falta do `gradlew`). Corrige e volta a correr.
- **AVISO** deve ser revisto:
  - assinatura com `storeFile`: o F-Droid compila sem a tua chave; usa-a só se o ficheiro existir:
    `if (file("chave.jks").exists()) { ... }`
  - acesso à Internet: se a app usa um serviço externo não livre, fica marcada com «NonFreeNet»

O utilizador e o repositório do GitHub são lidos do `git remote` do projeto. Se não for possível, indica-os:
`-Utilizador o_teu_utilizador -Repositorio painel-financeiro-android`.

## 2. Ícone e capturas de ecrã

- **Ícone 512x512:** em Android Studio, `File > New > Image Asset` gera `app/src/main/ic_launcher-playstore.png`;
  o script copia-o. Sem ele, o F-Droid usa o ícone do APK.
- **Capturas de ecrã:** com a app aberta no emulador ou no telemóvel, usa o botão da câmara no painel
  *Running Devices*. Grava-as como `1.png`, `2.png`, `3.png` em
  `fastlane/metadata/android/en-US/images/phoneScreenshots/` (e, se quiseres, também em `pt-PT`).

## 3. Publicar a versão (commit, tag e hash)

O F-Droid exige que a versão tenha uma tag e que o campo `commit` dos metadados tenha o **hash completo** desse
commit. O script trata de tudo:

```powershell
powershell -ExecutionPolicy Bypass -File .\preparar_fdroid.ps1 -Projeto "C:\...\painel-financeiro-android" -Autor "O teu nome" -Publicar
```

Isto faz o commit do `LICENSE` e da pasta `fastlane`, cria a tag `v<versionName>`, envia tudo para o GitHub e grava
o hash em `2_fdroiddata/metadata/pt.painelfinanceiro.app.yml`. Corre-o **depois** de pôr as capturas de ecrã.

## 4. Submeter no GitLab

1. Cria uma conta em <https://gitlab.com>.
2. Abre <https://gitlab.com/fdroid/fdroiddata> e carrega em **Fork**. O fork tem de ser **público**.
3. No teu fork, cria um ramo chamado `pt.painelfinanceiro.app` (Code > Branches > New branch).
   O ramo **não pode ser protegido**.
4. Nesse ramo, entra na pasta `metadata` e carrega o ficheiro `pt.painelfinanceiro.app.yml` gerado pelo script
   (botão **+ > Upload file**).
5. O GitLab corre sozinho a validação e a compilação (Build > Pipelines). Espera que todos os passos fiquem verdes.
   Se o GitLab pedir número de telefone ou cartão para correr a validação, **não dês**: abre na mesma o pedido e
   escreve isso numa nota, que a equipa do F-Droid corre a validação.
6. Abre o pedido de integração (**Merge request**) para `fdroid/fdroiddata`, ramo `master`:
   - título: `New app: Painel Financeiro`
   - descrição: o texto do `PEDIDO_MERGE_REQUEST.md`, com 2 ou 3 capturas de ecrã anexadas
7. Responde depressa às perguntas dos revisores. Se te pedirem alterações, faz commit no mesmo ramo (não abras outro
   pedido). A revisão pode demorar semanas.

## 5. Se a compilação falhar no GitLab

| Erro | Correção no `pt.painelfinanceiro.app.yml` |
|---|---|
| A versão do Android Gradle Plugin exige Java 21 | dentro do bloco do build, antes de `gradle:`, acrescenta as linhas abaixo |
| Versão do Gradle desconhecida (`gradlew-fdroid`) | o teu Gradle é muito recente; explica-o no pedido, a equipa atualiza a ferramenta, ou usa uma versão do Gradle um pouco mais antiga |
| Falta o ficheiro da chave de assinatura | ver o aviso da secção 1 |
| A app tem *product flavors* | troca `- yes` pelo nome do sabor (ex.: `- foss`) |

Linhas para usar o Java 21:

```yaml
    sudo:
      - apt-get update
      - apt-get install -y openjdk-21-jdk-headless
      - update-java-alternatives -a
```

## 6. Versões seguintes

Com `AutoUpdateMode: Version` e `UpdateCheckMode: Tags`, não precisas de voltar ao GitLab. Para cada versão nova:

1. Aumenta `versionCode` e `versionName` no `app/build.gradle`.
2. Cria `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (máximo 500 caracteres).
3. Faz commit, `git tag v<versionName>` e `git push --tags`. O F-Droid deteta a tag e publica a versão sozinho.

## Alternativas mais rápidas

- **IzzyOnDroid:** repositório alternativo com os mesmos princípios de código aberto. Distribui o APK das tuas
  *releases* do GitHub; a revisão é mais simples.
- **Repositório F-Droid próprio:** com o `fdroidserver` crias um repositório teu (por exemplo no GitHub Pages) e
  qualquer pessoa o adiciona na app F-Droid. Não depende de revisão.
