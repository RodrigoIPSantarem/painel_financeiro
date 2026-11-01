package pt.painelfinanceiro.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.FragmentHistoricoBinding;

public class HistoricoFragment extends Fragment {
    private FragmentHistoricoBinding b;
    private HistoricoAnosAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle estado) {
        b = FragmentHistoricoBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle estado) {
        HistoricoViewModel vm = new ViewModelProvider(this).get(HistoricoViewModel.class);
        // O ecrã "Este mês" partilha o ViewModel com a Activity: assim "Abrir" muda o mês mostrado lá.
        EsteMesViewModel esteMes = new ViewModelProvider(requireActivity()).get(EsteMesViewModel.class);

        adapter = new HistoricoAnosAdapter(chave -> {
            esteMes.selecionarMes(chave);
            ((MainActivity) requireActivity()).irPara(MainActivity.SEPARADOR_ESTE_MES);
        });
        b.rvHistorico.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvHistorico.setAdapter(adapter);

        vm.lista().observe(getViewLifecycleOwner(), l -> {
            adapter.definir(l.itens);
            b.tvVazio.setVisibility(l.itens.isEmpty() ? View.VISIBLE : View.GONE);
            b.tvTotal.setText(getString(R.string.total_desde_inicio, Fmt.eur(l.receitaTotal), Fmt.eur(l.investidoTotal)));
        });
    }

    public HistoricoAnosAdapter adapter() {
        return adapter;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
