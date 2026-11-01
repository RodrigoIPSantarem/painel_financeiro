package pt.painelfinanceiro.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.Map;

import pt.painelfinanceiro.app.PainelApp;
import pt.painelfinanceiro.app.data.PainelRepository;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.Desempenho;
import pt.painelfinanceiro.app.domain.Operacoes;

/** Partilhado pela Activity: usado pelo ecrã de rentabilidade e pelo diálogo de registo. */
public class RentabilidadeViewModel extends AndroidViewModel {
    private final PainelRepository repo;
    private final LiveData<Desempenho.Resumo> resumo;

    public RentabilidadeViewModel(@NonNull Application app) {
        super(app);
        repo = ((PainelApp) app).repositorio();
        resumo = Transformations.map(repo.dados(), Desempenho::calcular);
    }

    public LiveData<Desempenho.Resumo> resumo() {
        return resumo;
    }

    /** Dados atuais (para preencher o diálogo). */
    public Dados atuais() {
        return repo.dados().getValue();
    }

    /** @param dataOriginal null para criar; a data do registo para o editar */
    public void guardar(String dataOriginal, String data, String tipo, Double investido, Map<String, Double> valores, Double aportes) {
        repo.executar(d -> dataOriginal == null
                ? Operacoes.gerarRegisto(d, data, tipo, investido, valores, aportes, null, PainelRepository.agora())
                : Operacoes.editarRegisto(d, dataOriginal, data, tipo, investido, valores, aportes, PainelRepository.agora()));
    }

    public void remover(String data) {
        repo.executar(d -> Operacoes.removerRegisto(d, data));
    }
}
