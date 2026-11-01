package pt.painelfinanceiro.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.Map;

import pt.painelfinanceiro.app.PainelApp;
import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.data.PainelRepository;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.Operacoes;

public class ValoresViewModel extends AndroidViewModel {
    private final PainelRepository repo;

    public ValoresViewModel(@NonNull Application app) {
        super(app);
        repo = ((PainelApp) app).repositorio();
    }

    public LiveData<Dados> dados() {
        return repo.dados();
    }

    public void guardar(Map<String, Double> valores, String fase, String zona, Double despesas, Double fundo) {
        repo.executar(d -> {
            String agora = PainelRepository.agora();
            Dados comValores = Operacoes.definirValores(d, valores, agora);
            return Operacoes.definirDefinicoes(comValores, fase, zona, despesas, fundo, agora);
        }, getApplication().getString(R.string.guardado));
    }
}
