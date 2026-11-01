package pt.painelfinanceiro.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.tabs.TabLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import pt.painelfinanceiro.app.data.PainelDatabase;
import pt.painelfinanceiro.app.data.PainelRepository;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.Operacoes;
import pt.painelfinanceiro.app.domain.Tempo;
import pt.painelfinanceiro.app.ui.MainActivity;

/**
 * Testes da interface na JVM (Robolectric): abrem a MainActivity a sério, com o Room real,
 * e verificam que os ecrãs mostram e gravam o que devem.
 */

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class InterfaceTest {
    private PainelRepository repo;
    private final Observer<Dados> observadorDoTeste = d -> { };
    private ActivityScenario<MainActivity> cenario;
    private MainActivity atividade;

    @Before
    public void preparar() {
        // O ArchTaskExecutor é um singleton que guarda o Handler do Looper principal do 1.º teste;
        // nos seguintes, os resultados do Room nunca chegariam à interface. Um por teste resolve.
        androidx.arch.core.executor.ArchTaskExecutor.getInstance().setDelegate(new androidx.arch.core.executor.TaskExecutor() {
            private final java.util.concurrent.ExecutorService io = java.util.concurrent.Executors.newFixedThreadPool(2);

            @Override
            public void executeOnDiskIO(Runnable r) {
                io.execute(r);
            }

            @Override
            public void postToMainThread(Runnable r) {
                new android.os.Handler(Looper.getMainLooper()).post(r);
            }

            @Override
            public boolean isMainThread() {
                return Looper.getMainLooper().getThread() == Thread.currentThread();
            }
        });
        Context ctx = ApplicationProvider.getApplicationContext();
        repo = ((PainelApp) ctx).repositorio();
        // O LiveData do repositório só emite com um observador ativo (na app é o ecrã).
        repo.dados().observeForever(observadorDoTeste);
    }

    @After
    public void limpar() {
        if (cenario != null) cenario.close();
        repo.dados().removeObserver(observadorDoTeste);
        PainelDatabase.reiniciar(); // cada teste começa com uma base de dados nova
        androidx.arch.core.executor.ArchTaskExecutor.getInstance().setDelegate(null);
    }

    /** Espera por uma condição, deixando o thread principal e o Room trabalharem. */
    private static void esperar(String descricao, BooleanSupplier condicao) throws Exception {
        long fim = System.currentTimeMillis() + 25000;
        while (System.currentTimeMillis() < fim) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            if (condicao.getAsBoolean()) return;
            Thread.sleep(25);
        }
        throw new AssertionError("Não se verificou a tempo: " + descricao);
    }

    private void carregarExemplo() throws Exception {
        repo.executar(d -> Operacoes.exemploFicticio(LocalDate.now(), Tempo.agora()));
        esperar("dados de exemplo gravados", () -> repo.dados().getValue() != null && !repo.dados().getValue().meses.isEmpty());
    }

    private void abrirApp() {
        cenario = ActivityScenario.launch(MainActivity.class);
        AtomicReference<MainActivity> ref = new AtomicReference<>();
        cenario.onActivity(ref::set);
        atividade = ref.get();
    }

    /** O ecrã (Fragment) de um separador; o ViewPager2 dá-lhes a etiqueta "f0", "f1"... */
    private View ecra(int posicao) {
        Fragment f = atividade.getSupportFragmentManager().findFragmentByTag("f" + posicao);
        return f == null ? null : f.getView();
    }

    private <T extends View> T vista(int posicao, int id) {
        View raiz = ecra(posicao);
        return raiz == null ? null : raiz.findViewById(id);
    }

    private String texto(int posicao, int id) {
        TextView t = vista(posicao, id);
        return t == null ? "" : t.getText().toString();
    }

    private void esperarPlanoDeHoje() throws Exception {
        esperar("plano de hoje visível", () -> texto(0, R.id.tvOndeInvestir).contains("375,00"));
    }

    @Test
    public void mostraOsCincoSeparadores() {
        abrirApp();
        TabLayout tabs = atividade.findViewById(R.id.tabs);
        assertEquals(5, tabs.getTabCount());
        assertEquals("Este mês", tabs.getTabAt(0).getText().toString());
        assertEquals("Histórico", tabs.getTabAt(1).getText().toString());
        assertEquals("Rentabilidade", tabs.getTabAt(2).getText().toString());
        assertEquals("Os meus valores", tabs.getTabAt(3).getText().toString());
        assertEquals("Dados", tabs.getTabAt(4).getText().toString());
    }

    @Test
    public void esteMesMostraOPlanoComOsDadosDeExemplo() throws Exception {
        carregarExemplo();
        abrirApp();
        esperarPlanoDeHoje();

        assertEquals("Este mês", texto(0, R.id.tvTitulo));
        assertEquals(View.VISIBLE, vista(0, R.id.cardInvestir).getVisibility());
        assertEquals(View.GONE, vista(0, R.id.cardInvestido).getVisibility());
        assertEquals(5, ((RecyclerView) vista(0, R.id.rvDestinos)).getAdapter().getItemCount());
        assertEquals("1500", texto(0, R.id.etReceita));
    }

    @Test
    public void jaInvestiGravaORegistoESomaAosTotais() throws Exception {
        carregarExemplo();
        double etfsAntes = repo.dados().getValue().valores.get("etfs");
        abrirApp();
        esperarPlanoDeHoje();

        vista(0, R.id.btnInvestir).performClick();

        esperar("diálogo de confirmação", () -> ShadowDialog.getLatestDialog() != null);
        AlertDialog dialogo = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals("Confirmar", dialogo.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        String chave = Tempo.chaveMes(LocalDate.now());
        esperar("mês gravado como investido", () -> repo.dados().getValue().meses.get(chave) != null
                && repo.dados().getValue().meses.get(chave).jaInvestido());
        Dados d = repo.dados().getValue();
        assertEquals(375.0, d.meses.get(chave).investido(), 0);
        assertEquals(etfsAntes + 307.1, d.valores.get("etfs"), 0.001);

        // O ecrã passa a mostrar o mês como registado
        esperar("cartão 'mês registado' visível", () -> vista(0, R.id.cardInvestido).getVisibility() == View.VISIBLE);
        assertEquals(View.GONE, vista(0, R.id.cardInvestir).getVisibility());
        assertTrue(texto(0, R.id.tvInvestidoTitulo).contains("já está registado"));
        assertFalse(vista(0, R.id.etReceita).isEnabled());
    }

    @Test
    public void mudarParaOMesAnteriorMostraOEcraDeCorrecao() throws Exception {
        carregarExemplo();
        abrirApp();
        esperarPlanoDeHoje();

        vista(0, R.id.btnAnterior).performClick();

        String anterior = Tempo.rotuloMes(Tempo.somarMeses(Tempo.chaveMes(LocalDate.now()), -1));
        esperar("mês anterior aberto", () -> texto(0, R.id.tvTitulo).equals(anterior));
        assertEquals(View.VISIBLE, vista(0, R.id.btnHoje).getVisibility());
        assertTrue(texto(0, R.id.tvSubtitulo).contains("pode registar ou corrigir"));
        assertEquals("", texto(0, R.id.etReceita));
    }

    @Test
    public void valoresMostraOsCamposEGuardaAlteracoes() throws Exception {
        carregarExemplo();
        abrirApp();
        atividade.irPara(MainActivity.SEPARADOR_VALORES);
        esperar("total dos valores", () -> texto(3, R.id.tvTotal).contains("750,00"));
        assertEquals("585", texto(3, R.id.etDespesas));

        ((TextView) vista(3, R.id.etFundo)).setText("4000");
        vista(3, R.id.btnGuardar).performClick();

        esperar("fundo de emergência gravado", () -> repo.dados().getValue().definicoes.fundoEmergencia() != null
                && repo.dados().getValue().definicoes.fundoEmergencia() == 4000.0);
        assertEquals(1440.0, repo.dados().getValue().valores.get("etfs"), 0);
    }

    @Test
    public void rentabilidadeMostraAAcumuladaDosRegistos() throws Exception {
        carregarExemplo();
        abrirApp();
        atividade.irPara(2);
        esperar("rentabilidade acumulada", () -> texto(2, R.id.tvAcumulada).contains("14,0"));

        assertEquals("+14,0%", texto(2, R.id.tvAcumulada));
        assertEquals("+7,3%", texto(2, R.id.tvUltimo));
        assertEquals(3, ((RecyclerView) vista(2, R.id.rvRegistos)).getAdapter().getItemCount());
        // o separador de milhares pode variar (espaço ou nenhum), por isso compara-se sem espaços
        assertTrue(texto(2, R.id.tvValor).replaceAll("\\s|\u00a0", "").contains("8350,00"));
        assertTrue(texto(2, R.id.tvGanho).contains("950,00"));
        assertTrue(texto(2, R.id.tvGanho).contains("+12,8%"));
        assertEquals(View.VISIBLE, vista(2, R.id.cardGrafico).getVisibility());
        assertEquals(3, ((pt.painelfinanceiro.app.ui.GraficoBarrasView) vista(2, R.id.grafico)).getBarras().size());
    }

    @Test
    public void dadosMostraOResumoEApagarPedeConfirmacao() throws Exception {
        carregarExemplo();
        abrirApp();
        atividade.irPara(MainActivity.SEPARADOR_DADOS);
        esperar("resumo dos dados", () -> texto(4, R.id.tvResumoDados).contains("3 registos"));

        vista(4, R.id.btnApagar).performClick();
        esperar("diálogo de confirmação", () -> ShadowDialog.getLatestDialog() != null);
        ((AlertDialog) ShadowDialog.getLatestDialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        esperar("dados apagados", () -> repo.dados().getValue().meses.isEmpty() && repo.dados().getValue().registos.isEmpty());
        assertNotNull(repo.dados().getValue());
    }

    @Test
    public void novoRegistoComCapitalInvestidoPeloDialogo() throws Exception {
        carregarExemplo();
        abrirApp();
        atividade.irPara(MainActivity.SEPARADOR_RENTABILIDADE);
        esperar("ecrã de rentabilidade", () -> vista(2, R.id.btnNovo) != null && texto(2, R.id.tvAcumulada).contains("14,0"));

        vista(2, R.id.btnNovo).performClick();
        esperar("diálogo do registo", () -> ShadowDialog.getLatestDialog() != null && ShadowDialog.getLatestDialog().isShowing());
        AlertDialog dialogo = (AlertDialog) ShadowDialog.getLatestDialog();
        ((TextView) dialogo.findViewById(R.id.etCapital)).setText("8000");
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        String dezembro = LocalDate.now().getYear() + "-12-31";
        esperar("registo de 31 de dezembro gravado", () -> repo.dados().getValue().registos.stream().anyMatch(r -> r.data().equals(dezembro)));
        Dados.Registo r = repo.dados().getValue().registos.stream().filter(x -> x.data().equals(dezembro)).findFirst().orElseThrow();
        assertEquals(8000.0, r.investido(), 0);
        assertEquals(1440.0, r.etfs(), 0); // preenchido com "Os meus valores"
        assertEquals(600.0, r.aportes(), 0); // 8000 − 7400 do registo anterior
    }

    @Test
    public void historicoAgrupaPorAnoEAbreUmMesPassado() throws Exception {
        carregarExemplo();
        repo.executar(d -> Operacoes.guardarReceita(d, "2025-12", 1000, Tempo.agora()));
        esperar("mês de 2025 gravado", () -> repo.dados().getValue().meses.containsKey("2025-12"));
        abrirApp();
        atividade.irPara(MainActivity.SEPARADOR_HISTORICO);
        esperar("total do histórico", () -> texto(1, R.id.tvTotal).contains("500,00"));

        pt.painelfinanceiro.app.ui.HistoricoFragment f =
                (pt.painelfinanceiro.app.ui.HistoricoFragment) atividade.getSupportFragmentManager().findFragmentByTag("f1");
        pt.painelfinanceiro.app.ui.HistoricoAnosAdapter adapter = f.adapter();
        // 2026 (cabeçalho), Outubro 2026, 2025 (cabeçalho), Dezembro 2025
        assertEquals(4, adapter.getItemCount());
        RecyclerView rv = vista(1, R.id.rvHistorico);
        RecyclerView.ViewHolder vh = adapter.onCreateViewHolder(rv, adapter.getItemViewType(3));
        adapter.onBindViewHolder(vh, 3);
        assertTrue(((TextView) vh.itemView.findViewById(R.id.tvRotulo)).getText().toString().equals("Dezembro 2025"));
        vh.itemView.findViewById(R.id.btnAbrir).performClick();

        esperar("Este mês mostra Dezembro 2025", () -> texto(0, R.id.tvTitulo).equals("Dezembro 2025"));
        assertEquals("1000", texto(0, R.id.etReceita));
    }

}
