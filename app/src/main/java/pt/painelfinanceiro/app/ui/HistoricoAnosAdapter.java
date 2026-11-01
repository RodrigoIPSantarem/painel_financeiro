package pt.painelfinanceiro.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.ItemAnoBinding;
import pt.painelfinanceiro.app.databinding.ItemHistoricoBinding;

/** RecyclerView com dois tipos de linha: cabeçalho do ano e mês. */
public class HistoricoAnosAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TIPO_ANO = 0;
    private static final int TIPO_MES = 1;

    private final List<HistoricoViewModel.Item> itens = new ArrayList<>();
    private final Consumer<String> aoAbrir;

    public HistoricoAnosAdapter(Consumer<String> aoAbrir) {
        this.aoAbrir = aoAbrir;
    }

    public void definir(List<HistoricoViewModel.Item> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int posicao) {
        return itens.get(posicao).eAno() ? TIPO_ANO : TIPO_MES;
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        LayoutInflater inf = LayoutInflater.from(pai.getContext());
        if (tipo == TIPO_ANO) return new AnoVH(ItemAnoBinding.inflate(inf, pai, false));
        return new MesVH(ItemHistoricoBinding.inflate(inf, pai, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int posicao) {
        HistoricoViewModel.Item it = itens.get(posicao);
        Context c = h.itemView.getContext();
        if (h instanceof AnoVH a) {
            a.b.tvAno.setText(String.valueOf(it.ano.ano()));
            a.b.tvResumo.setText(c.getString(R.string.ano_resumo, Fmt.eur(it.ano.receita()), Fmt.eur(it.ano.investido()),
                    it.ano.mesesComReceita(), it.ano.mesesInvestidos()));
        } else if (h instanceof MesVH m) {
            m.b.tvRotulo.setText(it.rotulo);
            String inv = it.investido != null ? Fmt.eur(it.investido) : c.getString(R.string.investimento_por_registar);
            m.b.tvDetalhe.setText(c.getString(R.string.recebeu_investiu, Fmt.eur(it.receita), inv));
            m.b.btnAbrir.setContentDescription(c.getString(R.string.desc_abrir_mes, it.rotulo));
            m.b.btnAbrir.setOnClickListener(v -> aoAbrir.accept(it.chave));
        }
    }

    static class AnoVH extends RecyclerView.ViewHolder {
        final ItemAnoBinding b;

        AnoVH(ItemAnoBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    static class MesVH extends RecyclerView.ViewHolder {
        final ItemHistoricoBinding b;

        MesVH(ItemHistoricoBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
