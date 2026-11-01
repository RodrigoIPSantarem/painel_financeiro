package pt.painelfinanceiro.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.FragmentRentabilidadeBinding;
import pt.painelfinanceiro.app.domain.Desempenho;
import pt.painelfinanceiro.app.domain.Tempo;

public class RentabilidadeFragment extends Fragment {
    private FragmentRentabilidadeBinding b;
    private RentabilidadeViewModel vm;
    private RegistosAdapter registos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle estado) {
        b = FragmentRentabilidadeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle estado) {
        vm = new ViewModelProvider(requireActivity()).get(RentabilidadeViewModel.class);

        registos = new RegistosAdapter(
                data -> RegistoDialogFragment.editar(data).show(getChildFragmentManager(), "registo"),
                this::confirmarRemocao);
        b.rvRegistos.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvRegistos.setAdapter(registos);
        b.btnNovo.setOnClickListener(v -> RegistoDialogFragment.novo().show(getChildFragmentManager(), "registo"));

        vm.resumo().observe(getViewLifecycleOwner(), this::mostrar);
    }

    private void confirmarRemocao(String data) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.confirmar_remover_registo, Tempo.dataCurta(data)))
                .setPositiveButton(R.string.remover, (d, i) -> vm.remover(data))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void mostrar(Desempenho.Resumo r) {
        Desempenho.Linha ultima = r.ultima();
        if (ultima == null) {
            b.tvValor.setText("—");
            b.tvInvestido.setText("—");
            b.tvGanho.setText("—");
            b.tvAcumulada.setText("—");
            b.tvUltimo.setText("—");
            b.tvAcumuladaNota.setText(R.string.sem_registos);
            b.tvUltimoNota.setText("");
        } else {
            b.tvValor.setText(Fmt.eur(ultima.valor()));
            if (ultima.investido() != null) {
                b.tvInvestido.setText(Fmt.eur(ultima.investido()));
                b.tvGanho.setText(getString(R.string.ganho_valor, RegistosAdapter.sinal(ultima.ganho()),
                        Fmt.spct(ultima.ganhoPct() == null ? 0 : ultima.ganhoPct(), 1)));
                b.tvGanho.setTextColor(cor(ultima.ganho()));
            } else {
                b.tvInvestido.setText("—");
                b.tvGanho.setText(R.string.sem_capital);
                b.tvGanho.setTextColor(ContextCompat.getColor(requireContext(), R.color.apagado));
            }
            b.tvAcumulada.setText(Fmt.spct(ultima.acumulada(), 1));
            b.tvAcumulada.setTextColor(cor(ultima.acumulada()));
            b.tvAcumuladaNota.setText(getString(R.string.desde, Tempo.dataCurta(r.primeira().registo().data())));
            b.tvUltimo.setText(Fmt.spct(ultima.periodo(), 1));
            b.tvUltimo.setTextColor(cor(ultima.periodo()));
            b.tvUltimoNota.setText(getString(R.string.ate, Tempo.dataCurta(ultima.registo().data())));
        }

        List<GraficoBarrasView.Barra> barras = new ArrayList<>();
        for (Desempenho.Linha l : r.linhas()) {
            barras.add(new GraficoBarrasView.Barra(Tempo.dataCurta(l.registo().data()).replaceFirst("^\\d+ ", ""), l.investido(), l.valor()));
        }
        b.cardGrafico.setVisibility(barras.isEmpty() ? View.GONE : View.VISIBLE);
        b.grafico.setBarras(barras);
        if (ultima != null) {
            b.grafico.setContentDescription(getString(R.string.grafico_descricao, barras.size(),
                    ultima.investido() == null ? "—" : Fmt.eur(ultima.investido()), Fmt.eur(ultima.valor())));
        }

        // Lista do mais recente para o mais antigo
        List<Desempenho.Linha> lista = new ArrayList<>(r.linhas());
        Collections.reverse(lista);
        b.cardRegistos.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
        registos.submitList(lista);
    }

    private int cor(double v) {
        return ContextCompat.getColor(requireContext(), v > 0 ? R.color.positivo : v < 0 ? R.color.negativo : R.color.tinta);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
