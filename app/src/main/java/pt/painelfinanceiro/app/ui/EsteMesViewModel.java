package pt.painelfinanceiro.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import pt.painelfinanceiro.app.PainelApp;
import pt.painelfinanceiro.app.data.PainelRepository;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.Operacoes;
import pt.painelfinanceiro.app.domain.Plano;
import pt.painelfinanceiro.app.domain.PlanoService;
import pt.painelfinanceiro.app.domain.Tempo;

/** Estado do ecrã "Este mês": o mês escolhido, a receita escrita e o plano daí resultante. */
public class EsteMesViewModel extends AndroidViewModel {

    /** Tudo o que o ecrã precisa de mostrar, já calculado. */
    public static class Estado {
        public final Plano plano;
        public final double receitaGuardada;
        public final boolean alterada;
        public final boolean eHoje;
        public final boolean futuro;
        public final List<String> mesesDisponiveis;

        Estado(Plano plano, double receitaGuardada, boolean alterada, boolean eHoje, boolean futuro, List<String> meses) {
            this.plano = plano;
            this.receitaGuardada = receitaGuardada;
            this.alterada = alterada;
            this.eHoje = eHoje;
            this.futuro = futuro;
            this.mesesDisponiveis = meses;
        }
    }

    private final PainelRepository repo;
    private final String hoje = Tempo.chaveMes(LocalDate.now());
    private final MutableLiveData<String> chave = new MutableLiveData<>(hoje);
    private final MutableLiveData<String> receitaTexto = new MutableLiveData<>();
    private final MutableLiveData<Boolean> somar = new MutableLiveData<>(true);
    private final MediatorLiveData<Estado> estado = new MediatorLiveData<>();

    public EsteMesViewModel(@NonNull Application app) {
        super(app);
        repo = ((PainelApp) app).repositorio();
        estado.addSource(repo.dados(), d -> {
            if (receitaTexto.getValue() == null) receitaTexto.setValue(textoGuardado(d, chave.getValue()));
            recalcular();
        });
        estado.addSource(chave, c -> recalcular());
        estado.addSource(receitaTexto, t -> recalcular());
    }

    public LiveData<Estado> estado() {
        return estado;
    }

    public LiveData<String> receitaTexto() {
        return receitaTexto;
    }

    public LiveData<Boolean> somar() {
        return somar;
    }

    public String hoje() {
        return hoje;
    }

    public void setReceitaTexto(String texto) {
        if (!texto.equals(receitaTexto.getValue())) receitaTexto.setValue(texto);
    }

    public void setSomar(boolean valor) {
        if (!Boolean.valueOf(valor).equals(somar.getValue())) somar.setValue(valor);
    }

    /** Muda de mês: carrega a receita guardada e escolhe se, por omissão, soma aos totais (só de hoje em diante). */
    public void selecionarMes(String nova) {
        Dados d = repo.dados().getValue();
        chave.setValue(nova);
        somar.setValue(nova.compareTo(hoje) >= 0);
        receitaTexto.setValue(d == null ? "" : textoGuardado(d, nova));
    }

    public void guardarReceita() {
        String c = chave.getValue();
        double r = receitaAtual();
        repo.executar(d -> Operacoes.guardarReceita(d, c, r, PainelRepository.agora()));
    }

    public void investir(boolean somarAosTotais) {
        String c = chave.getValue();
        double r = receitaAtual();
        repo.executar(d -> Operacoes.marcarInvestido(d, c, r, somarAosTotais, PainelRepository.agora()));
    }

    public void anular() {
        String c = chave.getValue();
        repo.executar(d -> Operacoes.desfazerInvestido(d, c, PainelRepository.agora()));
    }

    private double receitaAtual() {
        Double v = Fmt.lerNumero(receitaTexto.getValue());
        return v == null ? 0 : v;
    }

    private static String textoGuardado(Dados d, String chave) {
        Dados.Mes m = d.meses.get(chave);
        return m == null || m.receita() <= 0 ? "" : Fmt.paraCampo(m.receita());
    }

    private void recalcular() {
        Dados d = repo.dados().getValue();
        String c = chave.getValue();
        String texto = receitaTexto.getValue();
        if (d == null || c == null || texto == null) return;

        Double digitada = Fmt.lerNumero(texto);
        double rec = digitada == null ? 0 : digitada;
        Plano plano = PlanoService.calcular(d, d.protocolo(), c, rec);
        Dados.Mes guardado = d.meses.get(c);
        double receitaGuardada = guardado == null ? 0 : guardado.receita();
        boolean alterada = !plano.mes().jaInvestido() && Math.abs(rec - receitaGuardada) > 1e-9;

        TreeSet<String> meses = new TreeSet<>(d.meses.keySet());
        for (int i = -6; i <= 2; i++) meses.add(Tempo.somarMeses(hoje, i));
        meses.add(c);
        List<String> ordenados = new ArrayList<>(meses);
        Collections.reverse(ordenados);

        estado.setValue(new Estado(plano, receitaGuardada, alterada, c.equals(hoje), c.compareTo(hoje) > 0, ordenados));
    }
}
