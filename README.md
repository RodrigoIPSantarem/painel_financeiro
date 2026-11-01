# Painel Financeiro (Android, Java)

Orçamento mensal e repartição dos aportes pelos tipos de ativo, seguindo um protocolo de investimento pessoal. **100% offline**: sem servidor, sem conta e **sem permissões**. Os dados ficam no telemóvel, numa base de dados SQLite (Room).

Licença: [MIT](LICENSE). A cópia de segurança em JSON tem o mesmo formato da versão web da app, por isso os ficheiros servem nas duas.

## Como se usa (uma vez por mês)

1. **Este mês:** escreva quanto recebeu. A app mostra quanto vai para necessidades, desejos, fundo de emergência e investimento, e em que tipos de ativo investir: quem está abaixo da meta recebe mais, quem já está acima não recebe nada (não se vende para rebalancear).
2. Invista na corretora e carregue em **Já investi** (pode anular).
3. Com **Anterior**, **Seguinte** ou **Escolher mês** abre qualquer outro mês para registar a receita ou corrigir erros.
4. **Histórico:** todos os meses guardados, agrupados por ano, com o total recebido e investido em cada ano. "Abrir" leva ao mês em "Este mês".
5. **Os meus valores:** corrija de vez em quando o total de cada tipo de ativo e as definições (despesas fixas, fundo de emergência, dívidas, fase de idade).
6. **Rentabilidade:** a 31 de dezembro e no aniversário do investimento, toque em **Novo registo** e indique o **capital realmente investido** até essa data; os valores de cada tipo vêm preenchidos com "Os meus valores" (pode corrigi-los). A app mostra o valor, o capital investido, o **ganho em euros e em percentagem**, a rentabilidade do período e a acumulada (Dietz Modificado), e um gráfico de barras investido/valor. Toque num registo para o editar.
7. **Dados:** exportar e importar a cópia de segurança (por exemplo para o Google Drive), juntar dados de dois aparelhos e importar os seus parâmetros do protocolo.

## Abrir e executar

Requisitos: Android Studio recente (traz o JDK 17 ou superior).

1. **File > Open** e escolha esta pasta. Espere pela sincronização do Gradle (o Android Studio cria o `local.properties` sozinho).
2. Ligue o telemóvel com a depuração USB ativa (ou crie um emulador) e carregue em **Run**.
3. APK para instalar no seu telemóvel: **Build > Build Bundle(s) / APK(s) > Build APK(s)**. Para publicar é preciso assinar: **Build > Generate Signed App Bundle / APK**.

Na linha de comandos: `./gradlew testDebugUnitTest assembleDebug` (precisa do Android SDK 35; o APK sai em `app/build/outputs/apk/debug/`).

## Arquitetura (princípios de PDM)

| Conceito | Onde |
| --- | --- |
| Activity única com TabLayout + ViewPager2 e um Fragment por ecrã | `ui/MainActivity`, `ui/PaginasAdapter`, `ui/*Fragment` |
| MVVM: ViewModel + LiveData, sem lógica nos Fragments | `ui/*ViewModel` |
| Persistência local com Room (entidades, DAO, transações) | `data/*Entity`, `data/PainelDao`, `data/PainelDatabase` |
| Repositório como única porta de acesso, operações fora do thread principal | `data/PainelRepository` |
| RecyclerView com ListAdapter e DiffUtil | `ui/DestinosAdapter`, `HistoricoAdapter`, `RegistosAdapter` |
| ViewBinding e recursos (strings, cores, temas, layouts) | `res/` |
| Seletor de ficheiros do sistema (Storage Access Framework), sem permissões | `ui/DadosFragment`, `ui/DadosViewModel` |
| Diálogos de confirmação, Snackbar, DatePicker | `ui/*Fragment`, `ui/MainActivity` |
| DialogFragment para criar e editar registos | `ui/RegistoDialogFragment` |
| View personalizada desenhada em Canvas (gráfico de barras) | `ui/GraficoBarrasView` |
| RecyclerView com vários tipos de linha (cabeçalho do ano e mês) | `ui/HistoricoAnosAdapter` |
| ViewModel partilhado pela Activity (Histórico abre um mês em Este mês) | `ui/EsteMesViewModel`, `ui/HistoricoFragment` |
| Migração da base de dados Room (versão 1 → 2) | `data/PainelDatabase` |
| Regras de negócio em Java puro, sem Android, testáveis na JVM | `domain/` |

Evento de uso único (`data/Evento`) para mensagens que não se repetem ao rodar o ecrã. As regras alteram sempre uma **cópia** dos dados (`domain/Operacoes`), por isso a interface nunca vê estados a meio.

## Testes

```bash
./gradlew testDebugUnitTest
```

- `DominioTest` (45): alocação, repartição do aporte, rentabilidade (Dietz Modificado), capital investido e ganho, resumo anual, registo de investimento, junção de dois aparelhos e leitura de ficheiros da versão web.
- `InterfaceTest` (9): abrem a MainActivity com o Room real (Robolectric) e verificam os ecrãs, o "Já investi", a mudança de mês, o histórico por ano, o novo registo com capital investido, os valores, a rentabilidade e o apagar com confirmação.

Não há testes instrumentados (em emulador ou telemóvel).

## Os seus parâmetros do protocolo

Os valores de fábrica (matriz por idade, divisão dos ETFs, zonas de dívida) são exemplos. Em **Dados > Parâmetros do protocolo** importe um `.json` só com o que quer alterar:

```json
{
  "pie_core": { "ETF1": 0.6, "ETF2": 0.4 },
  "matriz_idade": { "23-30": { "etfs": 0.4, "acoes": 0.2, "cripto": 0.1, "terras": 0.15, "reserva": 0.15, "renda_fixa": 0 } }
}
```

Ficam guardados só no telemóvel. Não os ponha no repositório.

## Decisões e limites

- **Repartição do aporte:** quanto cada tipo deveria valer depois do aporte (meta × novo total). Quem está abaixo recebe o que lhe falta; se o aporte chega para todos, o resto reparte-se pelas metas; se não chega, reparte-se proporcionalmente. É uma regra desta app, não do protocolo.
- **Backup automático do Android:** `allowBackup` está ligado, por isso o Android pode copiar a base de dados para a conta Google do telemóvel. Para desligar, ponha `android:allowBackup="false"` no `AndroidManifest.xml`.
- **Versões:** `minSdk` 26 (Android 8), `compileSdk` e `targetSdk` 35.
- **Aportes de cada período:** vêm da diferença do capital investido entre registos (se descer, conta como levantamento). Em registos sem capital investido usam-se os aportes guardados.
- Se apagar um registo de rentabilidade num aparelho, o "Juntar" pode trazê-lo de volta do outro; apague-o nos dois.
