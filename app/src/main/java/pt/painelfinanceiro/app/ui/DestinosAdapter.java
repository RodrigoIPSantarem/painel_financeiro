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

import java.util.stream.Collectors;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.ItemDestinoBinding;
import pt.painelfinanceiro.app.domain.Plano;

/** Lista dos tipos de ativo, com o valor a investir em cada um (maior primeiro). */
public class DestinosAdapter extends ListAdapter<Plano.DestinoPlano, DestinosAdapter.VH> {

    public DestinosAdapter() {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Plano.DestinoPlano a, @NonNull Plano.DestinoPlano b) {
                return a.id().equals(b.id());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Plano.DestinoPlano a, @NonNull Plano.DestinoPlano b) {
                return a.equals(b);
            }
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new VH(ItemDestinoBinding.inflate(LayoutInflater.from(pai.getContext()), pai, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int posicao) {
        h.ligar(getItem(posicao));
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ItemDestinoBinding b;

        VH(ItemDestinoBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void ligar(Plano.DestinoPlano d) {
            Context c = b.getRoot().getContext();
            b.tvNome.setText(d.nome());
            b.tvAporte.setText(Fmt.eur(d.aporte()));
            b.barra.setIndicatorColor(Cores.pilar(c, d.id()));
            b.barra.setProgress((int) Math.round(Math.min(1.0, d.real() / 0.6) * 100));
            b.tvDetalhe.setText(c.getString(R.string.tem_meta, Fmt.eur(d.atual()), Fmt.pct(d.real(), 0), Fmt.pct(d.meta(), 0)));

            int texto;
            int cor;
            switch (d.estado()) {
                case "abaixo": texto = R.string.estado_abaixo; cor = R.color.estado_abaixo; break;
                case "acima": texto = R.string.estado_acima; cor = R.color.estado_acima; break;
                default: texto = R.string.estado_perto; cor = R.color.apagado; break;
            }
            b.tvEstado.setText(texto);
            b.tvEstado.setTextColor(ContextCompat.getColor(c, cor));

            if (d.pieCore() != null && !d.pieCore().isEmpty()) {
                b.tvPie.setVisibility(View.VISIBLE);
                b.tvPie.setText(d.pieCore().stream().map(p -> p.ticker() + " " + Fmt.eur(p.valor())).collect(Collectors.joining("   ")));
            } else {
                b.tvPie.setVisibility(View.GONE);
            }
        }
    }
}
