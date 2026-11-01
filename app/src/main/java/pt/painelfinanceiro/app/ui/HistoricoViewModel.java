package pt.painelfinanceiro.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pt.painelfinanceiro.app.PainelApp;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.Desempenho;
import pt.painelfinanceiro.app.domain.Tempo;

/** Lista de todos os meses, agrupados por ano (cabeçalho do ano + meses, do mais recente para o mais antigo). */
public class HistoricoViewModel extends AndroidViewModel {

    /** Um elemento da lista: ou o cabeçalho de um ano, ou um mês. */
    public static class Item {
        public final Desempenho.Ano ano;   // preenchido nos cabeçalhos
        public final String chave;         // preenchido nos meses
        public final String rotulo;
        public final double receita;
        public final Double investido;

        private Item(Desempenho.Ano ano, String chave, String rotulo, double receita, Double investido) {
            this.ano = ano;
            this.chave = chave;
            this.rotulo = rotulo;
            this.receita = receita;
            this.investido = investido;
        }

        public boolean eAno() {
            return ano != null;
        }
    }

    public static class Lista {
        public final List<Item> itens;
        public final double receitaTotal;
        public final double investidoTotal;

        Lista(List<Item> itens, double receitaTotal, double investidoTotal) {
            this.itens = itens;
            this.receitaTotal = receitaTotal;
            this.investidoTotal = investidoTotal;
        }
    }

    private final LiveData<Lista> lista;

    public HistoricoViewModel(@NonNull Application app) {
        super(app);
        lista = Transformations.map(((PainelApp) app).repositorio().dados(), HistoricoViewModel::montar);
    }

    public LiveData<Lista> lista() {
        return lista;
    }

    static Lista montar(Dados d) {
        Desempenho.Resumo r = Desempenho.calcular(d);
        List<Item> itens = new ArrayList<>();
        for (Desempenho.Ano ano : r.anos()) {
            itens.add(new Item(ano, null, null, 0, null));
            for (Map.Entry<String, Dados.Mes> e : d.meses.descendingMap().entrySet()) {
                if (!e.getKey().startsWith(ano.ano() + "-")) continue;
                Dados.Mes m = e.getValue();
                if (m.receita() <= 0 && !m.jaInvestido()) continue;
                itens.add(new Item(null, e.getKey(), Tempo.rotuloMes(e.getKey()), m.receita(), m.investido()));
            }
        }
        return new Lista(itens, r.receitaTotal(), r.investidoNosMeses());
    }
}
