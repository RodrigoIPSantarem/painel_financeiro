package pt.painelfinanceiro.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pt.painelfinanceiro.app.domain.Allocation;
import pt.painelfinanceiro.app.domain.Contribution;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.DadosJson;
import pt.painelfinanceiro.app.domain.Desempenho;
import pt.painelfinanceiro.app.domain.Money;
import pt.painelfinanceiro.app.domain.Operacoes;
import pt.painelfinanceiro.app.domain.Plano;
import pt.painelfinanceiro.app.domain.PlanoService;
import pt.painelfinanceiro.app.domain.Protocolo;
import pt.painelfinanceiro.app.domain.RegraException;
import pt.painelfinanceiro.app.domain.Returns;
import pt.painelfinanceiro.app.domain.Tempo;

/** Testes das regras (Java puro, sem Android). Os valores são os mesmos da versão web. */
public class DominioTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 25);
    private static final String CH = "2026-10";
    private static final String AGORA = "2026-10-25T12:00:00.000Z";
    private static final Protocolo P = Protocolo.padrao();

    private static Dados exemplo() {
        return Operacoes.exemploFicticio(HOJE, AGORA);
    }

    private static double aporte(Plano p, String id) {
        return p.destinos().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow().aporte();
    }

    private static Map<String, Double> aportes(List<Contribution.Destino> r) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (Contribution.Destino d : r) m.put(d.id(), d.aporte());
        return m;
    }

    private static Map<String, Double> mapa(Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        return m;
    }

    // ---------- alocação ----------

    @Test
    public void zonaVerdeDivideAReceita() {
        Allocation.Resultado r = Allocation.calcular(1500, "verde", 5.1, "23-30", P);
        assertEquals(750.0, r.quotas().get("despesas").valor(), 0);
        assertEquals(225.0, r.quotas().get("desejos").valor(), 0);
        assertEquals(150.0, r.quotas().get("fundo").valor(), 0);
        assertEquals(375.0, r.quotas().get("investimento").valor(), 0);
        assertTrue(r.avisos().isEmpty());
    }

    @Test
    public void arredondamentoMeioParaCima() {
        assertEquals(1.01, Money.arredondar(1.005), 0);
        assertEquals(2.68, Money.arredondar(2.675), 0);
        assertEquals(-2.68, Money.arredondar(-2.675), 0);
        assertEquals(23.6, Money.arredondar(157.3 * 0.15), 0); // 23,595
    }

    @Test
    public void cadaRegraSoma100PorCento() {
        for (String zona : new String[]{"nenhuma", "verde", "amarela", "laranja", "vermelha", "emergencia"}) {
            for (double meses : new double[]{1, 4, 8}) {
                Allocation.Quotas q = Allocation.regras(zona, meses);
                assertEquals(zona + "/" + meses, 1.0, q.despesas() + q.desejos() + q.fundo() + q.investimento() + q.amortizacao(), 1e-9);
            }
        }
    }

    @Test
    public void regrasEspecificas() {
        assertEquals(0.0, Allocation.regras("verde", 2).investimento(), 0);
        assertEquals(0.35, Allocation.regras("verde", 2).fundo(), 0);
        assertEquals(0.175, Allocation.regras("amarela", 5).investimento(), 0);
        assertEquals(0.075, Allocation.regras("amarela", 5).amortizacao(), 0);
        assertEquals(0.35, Allocation.regras("nenhuma", 6).investimento(), 0);
        assertEquals(0.25, Allocation.regras("nenhuma", 4).investimento(), 0);
        assertEquals(0.35, Allocation.regras("vermelha", 6).amortizacao(), 0);
        assertEquals(0.25, Allocation.regras("vermelha", 4).amortizacao(), 0);
        assertEquals(0.14, Allocation.regras("emergencia", 12).fundo(), 0);
        assertThrows(RegraException.class, () -> Allocation.regras("roxa", 5));
    }

    @Test
    public void zonasPorTaeg() {
        assertEquals("nenhuma", Allocation.zonaPorTaeg(null));
        assertEquals("verde", Allocation.zonaPorTaeg(3.4));
        assertEquals("amarela", Allocation.zonaPorTaeg(3.5));
        assertEquals("laranja", Allocation.zonaPorTaeg(6.0));
        assertEquals("vermelha", Allocation.zonaPorTaeg(9.0));
        assertEquals("emergencia", Allocation.zonaPorTaeg(15.0));
    }

    @Test
    public void avisaSeAMatrizNaoSoma100() {
        JsonObject local = JsonParser.parseString("{\"matriz_idade\":{\"50+\":{\"etfs\":0.25,\"acoes\":0.05,\"cripto\":0.05,\"terras\":0.3,\"reserva\":0.15,\"renda_fixa\":0.15}}}").getAsJsonObject();
        Allocation.Resultado r = Allocation.calcular(1000, "verde", 10, "50+", P.fundir(local));
        assertEquals(1, r.avisos().size());
        assertTrue(r.avisos().get(0).contains("95"));
    }

    @Test
    public void asFasesPorOmissaoSomam100() {
        for (Map<String, Double> fase : P.matrizIdade.values()) {
            assertEquals(1.0, fase.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9);
        }
    }

    @Test
    public void pieCorePessoalSubstituiOExemploEAsMatrizesFundemPorChave() {
        JsonObject local = JsonParser.parseString("{\"pie_core\":{\"X\":0.7,\"Y\":0.3},\"matriz_idade\":{\"23-30\":{\"etfs\":0.4}}}").getAsJsonObject();
        Protocolo p = P.fundir(local);
        assertEquals(2, p.pieCore.size());
        assertEquals(0.7, p.pieCore.get("X"), 0);
        assertEquals(0.4, p.matrizIdade.get("23-30").get("etfs"), 0);
        assertEquals(0.15, p.matrizIdade.get("23-30").get("acoes"), 0);
        assertEquals(0.45, p.matrizIdade.get("30-40").get("etfs"), 0);
        assertEquals(0.5, P.matrizIdade.get("23-30").get("etfs"), 0); // o original não muda
    }

    // ---------- repartição do aporte ----------

    @Test
    public void aporteVaiParaQuemEstaAbaixoDaMeta() {
        assertEquals(mapa("a", 0, "b", 100), aportes(Contribution.distribuir(mapa("a", 100, "b", 0), 100, mapa("a", 0.5, "b", 0.5))));
    }

    @Test
    public void quemEstaAcimaDaMetaNaoRecebeNada() {
        Map<String, Double> a = aportes(Contribution.distribuir(mapa("etfs", 300, "acoes", 100, "cripto", 400), 100, mapa("etfs", 0.6, "acoes", 0.3, "cripto", 0.1)));
        assertEquals(0.0, a.get("cripto"), 0);
        assertTrue(a.get("etfs") > a.get("acoes"));
        assertEquals(100.0, a.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9);
    }

    @Test
    public void aporteGrandeIgualaAsMetas() {
        assertEquals(mapa("a", 500, "b", 400), aportes(Contribution.distribuir(mapa("a", 100, "b", 0), 900, mapa("a", 0.6, "b", 0.4))));
    }

    @Test
    public void excedenteRepartePelasMetas() {
        assertEquals(mapa("a", 50, "b", 50), aportes(Contribution.distribuir(mapa("a", 500, "b", 500), 100, mapa("a", 0.5, "b", 0.5))));
    }

    @Test
    public void semNadaInvestidoSegueAsMetas() {
        assertEquals(mapa("a", 700, "b", 300), aportes(Contribution.distribuir(mapa(), 1000, mapa("a", 0.7, "b", 0.3))));
    }

    @Test
    public void aSomaEExatamenteOAporte() {
        List<Contribution.Destino> r = Contribution.distribuir(mapa("a", 123.45, "b", 67.89, "c", 1000.01), 333.33,
                mapa("a", 0.35, "b", 0.2, "c", 0.2, "d", 0.15, "e", 0.1));
        long cent = Math.round(aportes(r).values().stream().mapToDouble(Double::doubleValue).sum() * 100);
        assertEquals(33333, cent);
    }

    @Test
    public void metasNormalizadasEAporteZero() {
        List<Contribution.Destino> r = Contribution.distribuir(mapa(), 100, mapa("a", 0.475, "b", 0.475));
        assertEquals(mapa("a", 50, "b", 50), aportes(r));
        assertEquals(0.5, r.get(0).meta(), 0);
        assertEquals(mapa("a", 0, "b", 0), aportes(Contribution.distribuir(mapa("a", 10), 0, mapa("a", 0.5, "b", 0.5))));
    }

    @Test
    public void estadoAbaixoPertoAcima() {
        List<Contribution.Destino> r = Contribution.distribuir(mapa("a", 700, "b", 290, "c", 10), 0, mapa("a", 0.5, "b", 0.3, "c", 0.2));
        assertEquals("acima", r.get(0).estado());
        assertEquals("perto", r.get(1).estado());
        assertEquals("abaixo", r.get(2).estado());
    }

    // ---------- rentabilidade ----------

    @Test
    public void dietzModificadoEncadeado() {
        List<Returns.Retorno> r = Returns.encadear(List.of(
                new Returns.Ponto(6000, 0, 0), new Returns.Ponto(7200, 800, 0), new Returns.Ponto(8350, 600, 0)));
        assertEquals(0.0, r.get(0).periodo(), 0);
        assertEquals(0.0625, r.get(1).periodo(), 1e-9);
        assertEquals(0.073333, r.get(2).periodo(), 1e-6);
        assertEquals(0.140417, r.get(2).acumulada(), 1e-6);
        assertEquals(1.0 / 3, Returns.periodo(100, 110, 0, 20), 1e-12);
        assertEquals(0.0, Returns.periodo(0, 0, 0, 0), 0);
    }

    // ---------- plano do mês ----------

    @Test
    public void planoFavoreceOQueEstaAbaixoDaMeta() {
        Plano p = PlanoService.calcular(exemplo(), P, CH, 1500.0);
        Map<String, Double> div = new LinkedHashMap<>();
        p.divisao().forEach(l -> div.put(l.id(), l.valor()));
        assertEquals(mapa("despesas", 750, "desejos", 225, "fundo", 150, "investimento", 375), div);
        assertEquals(0.0, aporte(p, "cripto"), 0);
        assertEquals(0.0, aporte(p, "terras"), 0);
        assertEquals(0.0, aporte(p, "reserva"), 0);
        assertEquals(307.1, aporte(p, "etfs"), 0);
        assertEquals(67.9, aporte(p, "acoes"), 0);
        assertTrue(p.avisos().isEmpty());
        Plano.DestinoPlano etfs = p.destinos().stream().filter(x -> x.id().equals("etfs")).findFirst().orElseThrow();
        assertEquals("abaixo", etfs.estado());
        assertEquals(2, etfs.pieCore().size());
        assertEquals(307.1, etfs.pieCore().stream().mapToDouble(Plano.PieItem::valor).sum(), 0.011);
    }

    @Test
    public void fundoAbaixoDe3MesesNaoMandaInvestir() {
        Dados d = Operacoes.definirDefinicoes(exemplo(), "23-30", "verde", 600.0, 500.0, AGORA);
        Plano p = PlanoService.calcular(d, P, CH, 1500.0);
        assertEquals(0.0, p.investir(), 0);
        assertTrue(String.join(" ", p.avisos()).contains("não manda investir"));
    }

    @Test
    public void semDefinicoesAssumeFundoDe4MesesEAvisa() {
        Dados d = Dados.vazios(AGORA);
        Plano p = PlanoService.calcular(d, P, CH, 1000.0);
        assertEquals(250.0, p.investir(), 0);
        assertTrue(String.join(" ", p.avisos()).contains("despesas fixas"));
        assertTrue(String.join(" ", p.avisos()).contains("quanto tem em cada tipo"));
        assertEquals(125.0, aporte(p, "etfs"), 0); // sem valores: segue as metas (250 × 50%)
    }

    @Test
    public void historicoListaOsMesesComReceita() {
        Dados d = Dados.vazios(AGORA);
        d = Operacoes.guardarReceita(d, "2026-08", 1200, AGORA);
        d = Operacoes.guardarReceita(d, "2026-09", 1300, AGORA);
        Plano p = PlanoService.calcular(d, P, "2026-10", 0.0);
        assertEquals("2026-09", p.historico().get(0).chave());
        assertEquals("2026-08", p.historico().get(1).chave());
        assertEquals(2, p.historico().size());
    }

    @Test
    public void mesesChavesRotulosENavegacao() {
        assertEquals("2026-10", Tempo.chaveMes(HOJE));
        assertEquals("Outubro 2026", Tempo.rotuloMes("2026-10"));
        assertEquals("2027-01", Tempo.somarMeses("2026-12", 1));
        assertEquals("2025-12", Tempo.somarMeses("2026-01", -1));
        assertEquals("31 dez 2026", Tempo.dataCurta("2026-12-31"));
        assertEquals(24, Tempo.agora().length());
    }

    // ---------- registar investimento ----------

    @Test
    public void marcarInvestidoSomaAosValoresENaoDeixaRepetir() {
        Dados d0 = exemplo();
        Dados d1 = Operacoes.marcarInvestido(d0, CH, 1500, true, AGORA);
        assertEquals(375.0, d1.meses.get(CH).investido(), 0);
        assertTrue(d1.meses.get(CH).somado());
        assertEquals(d0.valores.get("etfs") + 307.1, d1.valores.get("etfs"), 0.001);
        assertEquals(d0.valores.get("acoes") + 67.9, d1.valores.get("acoes"), 0.001);
        assertEquals(d0.valores.get("cripto"), d1.valores.get("cripto"), 0);
        RegraException e = assertThrows(RegraException.class, () -> Operacoes.marcarInvestido(d1, CH, 1500, true, AGORA));
        assertTrue(e.getMessage().contains("já foi marcado"));
        assertFalse(d0.meses.get(CH).jaInvestido()); // o original não foi alterado
    }

    @Test
    public void anularDevolveOsValoresAoQueEram() {
        Dados d0 = exemplo();
        Dados d2 = Operacoes.desfazerInvestido(Operacoes.marcarInvestido(d0, CH, 1500, true, AGORA), CH, AGORA);
        assertEquals(d0.valores, d2.valores);
        assertFalse(d2.meses.get(CH).jaInvestido());
        assertEquals(1500.0, d2.meses.get(CH).receita(), 0);
        assertThrows(RegraException.class, () -> Operacoes.desfazerInvestido(d2, CH, AGORA));
    }

    @Test
    public void mesPassadoSemSomarNaoMexeNosValores() {
        Dados d0 = exemplo();
        Dados d1 = Operacoes.marcarInvestido(d0, "2026-09", 1400, false, AGORA);
        assertEquals(d0.valores, d1.valores);
        assertEquals(350.0, d1.meses.get("2026-09").investido(), 0);
        assertEquals(d0.valores, Operacoes.desfazerInvestido(d1, "2026-09", AGORA).valores);
    }

    @Test
    public void receitaSoSeEditaSeNaoEstiverInvestido() {
        Dados d1 = Operacoes.marcarInvestido(exemplo(), CH, 1500, true, AGORA);
        assertThrows(RegraException.class, () -> Operacoes.guardarReceita(d1, CH, 1600, AGORA));
        assertEquals(1111.0, Operacoes.guardarReceita(exemplo(), "2026-09", 1111, AGORA).meses.get("2026-09").receita(), 0);
        assertThrows(RegraException.class, () -> Operacoes.guardarReceita(Dados.vazios(AGORA), CH, -1, AGORA));
    }

    @Test
    public void recusaValoresInvalidos() {
        Dados v = Dados.vazios(AGORA);
        assertThrows(RegraException.class, () -> Operacoes.definirValores(v, mapa("ouro", 5), AGORA));
        assertThrows(RegraException.class, () -> Operacoes.definirValores(v, mapa("etfs", -5), AGORA));
        assertThrows(RegraException.class, () -> Operacoes.definirDefinicoes(v, "23-30", "roxa", null, null, AGORA));
        assertThrows(RegraException.class, () -> Operacoes.definirDefinicoes(v, "99", "verde", null, null, AGORA));
    }

    // ---------- rentabilidade: registos ----------

    @Test
    public void registoUsaValoresAtuaisESomaAportesInvestidos() {
        Dados d = Operacoes.marcarInvestido(exemplo(), CH, 1500, true, AGORA);
        d = Operacoes.gerarRegisto(d, "2026-12-31", "31dez", null, null, AGORA);
        Dados.Registo r = d.registos.get(d.registos.size() - 1);
        assertEquals(375.0, r.aportes(), 0); // out 2026 está entre 2025-12-31 e 2026-12-31
        assertEquals(d.valores.get("etfs"), r.etfs(), 0);
        assertEquals(d.valores.get("etfs") + d.valores.get("acoes") + d.valores.get("terras") + d.valores.get("cripto"), r.valorTotal(), 0.01);
    }

    @Test
    public void aportesManuaisTemPrioridadeEPrimeiroRegistoNaoTemAportes() {
        Dados d = Operacoes.gerarRegisto(exemplo(), "2026-12-31", "31dez", 123.0, null, AGORA);
        assertEquals(123.0, d.registos.get(d.registos.size() - 1).aportes(), 0);
        assertEquals(0.0, Operacoes.gerarRegisto(Dados.vazios(AGORA), "2026-01-01", "inicio", null, null, AGORA).registos.get(0).aportes(), 0);
    }

    @Test
    public void repetirADataSubstituiORegistoEValida() {
        Dados d = Operacoes.gerarRegisto(exemplo(), "2026-12-31", "31dez", null, null, AGORA);
        d = Operacoes.gerarRegisto(d, "2026-12-31", "31dez", 1.0, null, AGORA);
        assertEquals(1, d.registos.stream().filter(r -> r.data().equals("2026-12-31")).count());
        Dados v = Dados.vazios(AGORA);
        assertThrows(RegraException.class, () -> Operacoes.gerarRegisto(v, "ontem", "31dez", null, null, AGORA));
        assertThrows(RegraException.class, () -> Operacoes.gerarRegisto(v, "2026-12-31", "xx", null, null, AGORA));
        assertEquals(3, Operacoes.removerRegisto(Operacoes.gerarRegisto(exemplo(), "2026-12-31", "31dez", null, null, AGORA), "2026-12-31").registos.size());
    }

    // ---------- parâmetros pessoais ----------

    @Test
    public void parametrosPessoaisSaoFundidosEValidados() {
        JsonObject ok = JsonParser.parseString("{\"pie_core\":{\"SXR8\":0.4,\"SXRV\":0.3,\"IS3N\":0.2,\"IITU\":0.1}}").getAsJsonObject();
        Dados d = Operacoes.definirProtocolo(Dados.vazios(AGORA), ok, AGORA);
        assertEquals(4, d.protocolo().pieCore.size());
        assertEquals(0.5, d.protocolo().matrizIdade.get("23-30").get("etfs"), 0);

        JsonObject mau = JsonParser.parseString("{\"pie_core\":{\"A\":0.5,\"B\":0.2}}").getAsJsonObject();
        assertTrue(assertThrows(RegraException.class, () -> Operacoes.definirProtocolo(Dados.vazios(AGORA), mau, AGORA)).getMessage().contains("somar 100%"));
    }

    // ---------- cópia de segurança e sincronização ----------

    @Test
    public void exportarEImportarDevolveOMesmo() {
        Dados d = Operacoes.marcarInvestido(exemplo(), CH, 1500, true, AGORA);
        String json = DadosJson.paraJson(d);
        Dados volta = Operacoes.importar(Dados.vazios(AGORA), DadosJson.deJson(json), Operacoes.Modo.SUBSTITUIR);
        assertEquals(json, DadosJson.paraJson(volta));
        assertEquals(d.valores, volta.valores);
        assertEquals(d.meses.get(CH).aportes(), volta.meses.get(CH).aportes());
        assertNull(volta.meses.get("2026-09"));
    }

    @Test
    public void leFicheirosExportadosPelaVersaoWeb() {
        String web = "{\"versao\":1,\"definicoes\":{\"fase_idade\":\"30-40\",\"zona_divida\":\"amarela\",\"despesas_fixas_mensais\":null,\"fundo_emergencia_saldo\":2500,\"atualizadoEm\":\"2026-10-01T10:00:00.000Z\"},"
                + "\"valores\":{\"etfs\":100.5},\"valoresAtualizadoEm\":\"2026-10-01T10:00:00.000Z\",\"protocolo\":{\"valores\":{},\"atualizadoEm\":\"2026-10-01T10:00:00.000Z\"},"
                + "\"meses\":{\"2026-10\":{\"receita\":1500,\"investido\":375,\"aportes\":{\"etfs\":307.1,\"acoes\":67.9},\"somado\":true,\"atualizadoEm\":\"2026-10-25T10:00:00.000Z\"},\"2026-09\":{\"receita\":1400,\"atualizadoEm\":\"2026-09-30T10:00:00.000Z\"}},"
                + "\"registos\":[{\"data\":\"2025-12-31\",\"tipo\":\"31dez\",\"etfs\":4200,\"acoes\":1400,\"terras\":1250,\"cripto\":1500,\"aportes\":600,\"levantamentos\":0,\"atualizadoEm\":\"2026-01-01T00:00:00.000Z\"}]}";
        Dados d = DadosJson.deJson(web);
        assertEquals("30-40", d.definicoes.faseIdade());
        assertNull(d.definicoes.despesasFixas());
        assertEquals(2500.0, d.definicoes.fundoEmergencia(), 0);
        assertEquals(100.5, d.valores.get("etfs"), 0);
        assertTrue(d.meses.get("2026-10").jaInvestido());
        assertEquals(67.9, d.meses.get("2026-10").aportes().get("acoes"), 0);
        assertFalse(d.meses.get("2026-09").jaInvestido());
        assertEquals(1, d.registos.size());
        assertEquals(8350.0, d.registos.get(0).valorTotal(), 0);
    }

    @Test
    public void recusaFicheirosQueNaoSaoDaApp() {
        assertTrue(assertThrows(RegraException.class, () -> DadosJson.deJson("[]")).getMessage().contains("Ficheiro inválido"));
        assertTrue(assertThrows(RegraException.class, () -> DadosJson.deJson("isto não é json {")).getMessage().contains("legível"));
        assertTrue(assertThrows(RegraException.class, () -> DadosJson.deJson("{\"versao\":99,\"meses\":{},\"registos\":[]}")).getMessage().contains("versão mais recente"));
        assertTrue(assertThrows(RegraException.class, () -> DadosJson.deJson("{\"versao\":1,\"meses\":[],\"registos\":[]}")).getMessage().contains("faltam os meses"));
        assertTrue(assertThrows(RegraException.class, () -> DadosJson.deJson("{\"versao\":1,\"meses\":{\"x\":{}},\"registos\":[]}")).getMessage().contains("mês"));
        assertTrue(assertThrows(RegraException.class, () -> DadosJson.deJson("{\"ola\":1}")).getMessage().contains("Ficheiro inválido"));
    }

    @Test
    public void juntarMantemOMesRecenteESemPerderMeses() {
        Dados pc = Operacoes.guardarReceita(Operacoes.guardarReceita(Dados.vazios(t(1)), "2026-08", 1000, t(2)), "2026-09", 1100, t(3));
        Dados tel = Operacoes.guardarReceita(Operacoes.guardarReceita(Dados.vazios(t(1)), "2026-09", 1150, t(10)), "2026-10", 1200, t(11));

        Dados r = Operacoes.importar(pc, DadosJson.deJson(DadosJson.paraJson(tel)), Operacoes.Modo.JUNTAR);

        assertEquals(1000.0, r.meses.get("2026-08").receita(), 0); // só no PC
        assertEquals(1150.0, r.meses.get("2026-09").receita(), 0); // o telemóvel é mais recente
        assertEquals(1200.0, r.meses.get("2026-10").receita(), 0); // só no telemóvel
    }

    @Test
    public void juntarValoresEDefinicoesMaisRecentesGanhamEmQualquerOrdem() {
        Map<String, Double> v100 = mapa("etfs", 100);
        Map<String, Double> v999 = mapa("etfs", 999);
        Dados pc = Operacoes.definirValores(Dados.vazios(t(1)), v100, t(5));
        Dados tel = Operacoes.definirDefinicoes(Operacoes.definirValores(Dados.vazios(t(1)), v999, t(9)), "30-40", "nenhuma", null, null, t(8));
        Dados pcViaJson = DadosJson.deJson(DadosJson.paraJson(pc));
        Dados telViaJson = DadosJson.deJson(DadosJson.paraJson(tel));

        Dados r = Operacoes.importar(pc, telViaJson, Operacoes.Modo.JUNTAR);
        assertEquals(999.0, r.valores.get("etfs"), 0);
        assertEquals("30-40", r.definicoes.faseIdade());

        Dados r2 = Operacoes.importar(tel, pcViaJson, Operacoes.Modo.JUNTAR); // ordem inversa: mesmo resultado
        assertEquals(999.0, r2.valores.get("etfs"), 0);
    }

    @Test
    public void juntarRegistosPorData() {
        Dados a = Operacoes.gerarRegisto(Operacoes.definirValores(Dados.vazios(AGORA), mapa("etfs", 10), AGORA), "2025-12-31", "31dez", null, null, AGORA);
        Dados b = Operacoes.gerarRegisto(Operacoes.definirValores(Dados.vazios(AGORA), mapa("etfs", 20), AGORA), "2026-12-31", "31dez", null, null, AGORA);
        Dados r = Operacoes.importar(a, DadosJson.deJson(DadosJson.paraJson(b)), Operacoes.Modo.JUNTAR);
        assertEquals(List.of("2025-12-31", "2026-12-31"), r.registos.stream().map(Dados.Registo::data).toList());
    }

    private static String t(int n) {
        return String.format("2026-10-%02dT10:00:00.000Z", n);
    }

    // ---------- capital investido, ganho e desempenho ----------

    @Test
    public void registoComCapitalInvestidoCalculaGanhoEAportesPelaDiferenca() {
        // exemplo: 31 dez 2025 tinha 7400 € investidos; agora diz que investiu 8000 € no total
        Dados d = Operacoes.gerarRegisto(exemplo(), "2026-12-31", "31dez", 8000.0, null, null, null, AGORA);
        Dados.Registo r = d.registos.get(d.registos.size() - 1);
        assertEquals(600.0, r.aportes(), 0);
        assertEquals(8000.0, r.investido(), 0);
        assertEquals(5750.0 - 1500.0 - 8000.0, r.ganho(), 0.001); // valores atuais sem a Reserva, menos o investido
    }

    @Test
    public void capitalInvestidoQueDesceContaComoLevantamento() {
        Dados d = Operacoes.gerarRegisto(exemplo(), "2026-12-31", "31dez", 7000.0, null, null, null, AGORA);
        Dados.Registo r = d.registos.get(d.registos.size() - 1);
        assertEquals(0.0, r.aportes(), 0);
        assertEquals(400.0, r.levantamentos(), 0);
    }

    @Test
    public void valoresDoRegistoPodemSerIndicadosAMao() {
        Dados d = Operacoes.gerarRegisto(exemplo(), "2026-12-31", "31dez", 8000.0, mapa("etfs", 5000, "acoes", 1500, "terras", 1000, "cripto", 1500), null, null, AGORA);
        Dados.Registo r = d.registos.get(d.registos.size() - 1);
        assertEquals(9000.0, r.valorTotal(), 0);
        assertEquals(1000.0, r.ganho(), 0);
        assertThrows(RegraException.class, () -> Operacoes.gerarRegisto(exemplo(), "2026-12-31", "31dez", -1.0, null, null, null, AGORA));
    }

    @Test
    public void editarRegistoMudaADataEOsValores() {
        Dados d = Operacoes.editarRegisto(exemplo(), "2025-12-31", "2026-01-15", "aniversario", 7500.0, mapa("etfs", 4300, "acoes", 1400, "terras", 1250, "cripto", 1500), null, AGORA);
        assertFalse(d.registos.stream().anyMatch(r -> r.data().equals("2025-12-31")));
        Dados.Registo r = d.registos.get(d.registos.size() - 1);
        assertEquals("2026-01-15", r.data());
        assertEquals("aniversario", r.tipo());
        assertEquals(700.0, r.aportes(), 0); // 7500 - 6800 do registo anterior
        assertThrows(RegraException.class, () -> Operacoes.editarRegisto(exemplo(), "2025-12-31", "2024-12-31", "31dez", null, null, null, AGORA));
        assertThrows(RegraException.class, () -> Operacoes.editarRegisto(exemplo(), "1999-01-01", "2026-01-01", "31dez", null, null, null, AGORA));
    }

    @Test
    public void desempenhoMostraGanhoERentabilidadeDeCadaRegisto() {
        Desempenho.Resumo r = Desempenho.calcular(exemplo());
        assertEquals(3, r.linhas().size());
        Desempenho.Linha ultima = r.ultima();
        assertEquals(8350.0, ultima.valor(), 0);
        assertEquals(7400.0, ultima.investido(), 0);
        assertEquals(950.0, ultima.ganho(), 0);
        assertEquals(950.0 / 7400, ultima.ganhoPct(), 1e-9);
        assertEquals(600.0, ultima.aportes(), 0);
        assertEquals(0.073333, ultima.periodo(), 1e-6);
        assertEquals(0.140417, ultima.acumulada(), 1e-6); // igual ao cálculo pelos aportes guardados
    }

    @Test
    public void desempenhoUsaOsAportesGuardadosQuandoNaoHaCapitalInvestido() {
        Dados d = Dados.vazios(AGORA);
        d.registos.add(new Dados.Registo("2024-01-01", "inicio", 6000, 0, 0, 0, 0, 0, AGORA, null));
        d.registos.add(new Dados.Registo("2024-12-31", "31dez", 7200, 0, 0, 0, 800, 0, AGORA, null));
        Desempenho.Linha l = Desempenho.calcular(d).ultima();
        assertNull(l.ganho());
        assertEquals(800.0, l.aportes(), 0);
        assertEquals(0.0625, l.periodo(), 1e-9);
    }

    @Test
    public void resumoPorAno() {
        Dados d = Dados.vazios(AGORA);
        d = Operacoes.guardarReceita(d, "2025-11", 1000, AGORA);
        d = Operacoes.marcarInvestido(d, "2025-12", 1200, false, AGORA);
        d = Operacoes.guardarReceita(d, "2026-01", 1100, AGORA);
        Desempenho.Resumo r = Desempenho.calcular(d);
        assertEquals(2, r.anos().size());
        assertEquals(2026, r.anos().get(0).ano());
        Desempenho.Ano a2025 = r.anos().get(1);
        assertEquals(2200.0, a2025.receita(), 0);
        assertEquals(2, a2025.mesesComReceita());
        assertEquals(1, a2025.mesesInvestidos());
        assertEquals(a2025.investido(), d.meses.get("2025-12").investido(), 0);
        assertEquals(3300.0, r.receitaTotal(), 0);
    }

    @Test
    public void capitalInvestidoSobreviveAoFicheiroEFicheirosAntigosContinuamALer() {
        Dados d = Operacoes.gerarRegisto(exemplo(), "2026-12-31", "31dez", 8000.0, null, null, null, AGORA);
        Dados volta = DadosJson.deJson(DadosJson.paraJson(d));
        assertEquals(8000.0, volta.registos.get(volta.registos.size() - 1).investido(), 0);
        String antigo = "{\"versao\":1,\"meses\":{},\"registos\":[{\"data\":\"2025-12-31\",\"tipo\":\"31dez\",\"etfs\":100,\"acoes\":0,\"terras\":0,\"cripto\":0,\"aportes\":0,\"levantamentos\":0}]}";
        assertNull(DadosJson.deJson(antigo).registos.get(0).investido());
    }
}
