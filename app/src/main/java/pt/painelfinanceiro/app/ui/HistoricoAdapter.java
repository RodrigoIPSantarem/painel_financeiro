package pt.painelfinanceiro.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.Consumer;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.ItemHistoricoBinding;
import pt.painelfinanceiro.app.domain.Plano;

/** Meses já registados; "Abrir" muda o ecrã para esse mês. */
public class HistoricoAdapter extends ListAdapter<Plano.Historico, HistoricoAdapter.VH> {
    private final Consumer<String> aoAbrir;
    private String chaveAberta = "";

    public HistoricoAdapter(Consumer<String> aoAbrir) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Plano.Historico a, @NonNull Plano.Historico b) {
                return a.chave().equals(b.chave());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Plano.Historico a, @NonNull Plano.Historico b) {
                return a.equals(b);
            }
        });
        this.aoAbrir = aoAbrir;
    }

    public void definirChaveAberta(String chave) {
        if (!chave.equals(chaveAberta)) {
            chaveAberta = chave;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new VH(ItemHistoricoBinding.inflate(LayoutInflater.from(pai.getContext()), pai, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int posicao) {
        Plano.Historico m = getItem(posicao);
        var c = h.b.getRoot().getContext();
        h.b.tvRotulo.setText(m.rotulo());
        String investido = m.investido() != null ? Fmt.eur(m.investido()) : c.getString(R.string.investimento_por_registar);
        h.b.tvDetalhe.setText(c.getString(R.string.recebeu_investiu, Fmt.eur(m.receita()), investido));
        h.b.btnAbrir.setVisibility(m.chave().equals(chaveAberta) ? android.view.View.GONE : android.view.View.VISIBLE);
        h.b.btnAbrir.setContentDescription(c.getString(R.string.desc_abrir_mes, m.rotulo()));
        h.b.btnAbrir.setOnClickListener(v -> aoAbrir.accept(m.chave()));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemHistoricoBinding b;

        VH(ItemHistoricoBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
