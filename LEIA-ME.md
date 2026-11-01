# Percentagens configuráveis por método (soma obrigatória de 100%)

## O que faz

No ecrã de definições, o utilizador escolhe a percentagem de cada método com um *slider* (0 a 100):

- o total aparece em direto: "Total: 95% (faltam 5%)" a vermelho, "Total: 100%" a verde;
- o botão **Guardar** só fica ativo quando a soma dá exatamente 100%;
- **Repor valores de origem** volta às percentagens iniciais;
- 0% é permitido (o utilizador não usa esse método).

As percentagens são números inteiros, para a soma ser exata. A repartição do montante mensal é feita ao cêntimo, e a
soma das partes dá sempre o montante (os cêntimos dos arredondamentos vão para quem tem maior resto).

## Ficheiros (a pasta `app` tem a mesma estrutura do teu projeto)

| Ficheiro | Para quê |
|---|---|
| `domain/Alocacao.java` | Regra: 0–100 por método, válida só com soma 100, repartição ao cêntimo (sem Android, testável) |
| `data/AlocacaoRepositorio.java` | Grava nas definições do telemóvel (SharedPreferences); não mexe na base de dados Room |
| `ui/DefinicoesAlocacaoFragment.java` | O ecrã das definições |
| `res/layout/*.xml` | Layout do ecrã e de cada linha (nome, percentagem, slider) |
| `res/values/strings_alocacao.xml` | Textos em português (ficheiro novo: não substitui o teu `strings.xml`) |
| `test/.../AlocacaoTest.java` | 9 testes unitários (JUnit 4), como o teu `DominioTest` |
| `opcional_ingles/values-en/` | Os mesmos textos em inglês (ver abaixo) |

Testado: os 9 testes passam e o código compila para Java 11.

## Como integrar

1. Copia a pasta `app` para a raiz do projeto (junta-se às pastas que já existem; nenhum ficheiro teu é substituído).
2. Em `DefinicoesAlocacaoFragment`, troca o **exemplo** de `porDefeito()` e `NOMES` pelos métodos e percentagens
   que a tua app usa hoje (têm de somar 100). Os identificadores (`"etfs"`, `"cripto"`…) não devem mudar depois de
   publicares: são as chaves gravadas no telemóvel.
3. Abre o ecrã a partir da app. Com Navigation, acrescenta ao `nav_graph.xml`:

   ```xml
   <fragment
       android:id="@+id/definicoesAlocacao"
       android:name="pt.painelfinanceiro.app.ui.DefinicoesAlocacaoFragment"
       android:label="@string/alocacao_titulo" />
   ```

   e navega para ele a partir de um botão ou do menu.
4. Onde a app calcula hoje quanto vai para cada método, usa as percentagens gravadas em vez das fixas:

   ```java
   Alocacao alocacao = new AlocacaoRepositorio(context, DefinicoesAlocacaoFragment.porDefeito()).ler();
   Map<String, Long> partes = alocacao.repartir(montanteEmCentimos);   // ex.: 250,00 € -> 25000
   ```

5. O slider é da biblioteca Material (`com.google.android.material`), que os projetos novos do Android Studio já
   incluem. Se não a tiveres, acrescenta-a no `build.gradle` da app.

**Importante:** mudar as percentagens só afeta os cálculos daí para a frente. Os registos já feitos com «Já investi»
ficam com os valores que tinham.

## Inglês

A app pode ficar só em português: o F-Droid não o exige. A página em inglês da loja já diz que a interface está em
português europeu.

Se quiseres a app em inglês, copia `opcional_ingles/values-en` para `app/src/main/res/` **só quando** traduzires a
app toda (todos os textos em `res/values/strings.xml`, nenhum escrito diretamente no código ou nos layouts). Com uma
tradução parcial, a app fica metade em cada língua e o Lint dá erros de tradução em falta.

## Para o F-Droid

Quando publicares esta funcionalidade: aumenta o `versionCode` e o `versionName`, cria o
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (e o `pt-PT`), acrescenta a funcionalidade à
descrição e cria a tag da versão. O F-Droid apanha a versão nova sozinho.
