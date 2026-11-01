package pt.painelfinanceiro.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.Consumer;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.ItemRegistoBinding;
import pt.painelfinanceiro.app.domain.Desempenho;
import pt.painelfinanceiro.app.domain.Tempo;

/** Registos de rentabilidade: valor, capital investido, ganho e rentabilidade. Tocar edita. */
public class RegistosAdapter extends ListAdapter<Desempenho.Linha, RegistosAdapter.VH> {
    private final Consumer<String> aoEditar;
    private final Consumer<String> aoRemover;

    public RegistosAdapter(Consumer<String> aoEditar, Consumer<String> aoRemover) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Desempenho.Linha a, @NonNull Desempenho.Linha b) {
                return a.registo().data().equals(b.registo().data());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Desempenho.Linha a, @NonNull Desempenho.Linha b) {
                return a.equals(b);
            }
        });
        this.aoEditar = aoEditar;
        this.aoRemover = aoRemover;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new VH(ItemRegistoBinding.inflate(LayoutInflater.from(pai.getContext()), pai, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int posicao) {
        Desempenho.Linha l = getItem(posicao);
        Context c = h.b.getRoot().getContext();
        String data = Tempo.dataCurta(l.registo().data());
        h.b.tvData.setText(data + " · " + nomeTipo(c, l.registo().tipo()));
        h.b.tvTotal.setText(l.investido() != null
                ? c.getString(R.string.registo_linha_valor, Fmt.eur(l.valor()), Fmt.eur(l.investido()))
                : c.getString(R.string.registo_linha_valor_sem, Fmt.eur(l.valor())));
        if (l.ganho() != null) {
            h.b.tvGanho.setVisibility(View.VISIBLE);
            h.b.tvGanho.setText(c.getString(R.string.registo_linha_ganho, sinal(l.ganho()), Fmt.spct(l.ganhoPct() == null ? 0 : l.ganhoPct(), 1)));
            h.b.tvGanho.setTextColor(cor(c, l.ganho()));
        } else {
            h.b.tvGanho.setVisibility(View.GONE);
        }
        h.b.tvPeriodos.setText(c.getString(R.string.registo_linha_rent, Fmt.spct(l.periodo(), 1), Fmt.spct(l.acumulada(), 1)));
        h.b.tvPeriodos.setTextColor(cor(c, l.acumulada()));
        h.b.getRoot().setOnClickListener(v -> aoEditar.accept(l.registo().data()));
        h.b.btnRemover.setContentDescription(c.getString(R.string.desc_remover_registo, data));
        h.b.btnRemover.setOnClickListener(v -> aoRemover.accept(l.registo().data()));
    }

    static String sinal(double v) {
        return (v < 0 ? "−" : "+") + Fmt.eur(Math.abs(v));
    }

    private static int cor(Context c, double v) {
        return ContextCompat.getColor(c, v > 0 ? R.color.positivo : v < 0 ? R.color.negativo : R.color.apagado);
    }

    static String nomeTipo(Context c, String tipo) {
        switch (tipo) {
            case "inicio": return c.getString(R.string.tipo_inicio);
            case "31dez": return c.getString(R.string.tipo_31dez);
            default: return c.getString(R.string.tipo_aniversario);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemRegistoBinding b;

        VH(ItemRegistoBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
