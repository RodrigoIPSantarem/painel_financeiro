package pt.painelfinanceiro.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.FragmentEsteMesBinding;
import pt.painelfinanceiro.app.databinding.ItemLinhaDivisaoBinding;
import pt.painelfinanceiro.app.domain.Plano;
import pt.painelfinanceiro.app.domain.Tempo;

public class EsteMesFragment extends Fragment {
    private FragmentEsteMesBinding b;
    private EsteMesViewModel vm;
    private DestinosAdapter destinos;
    private HistoricoAdapter historico;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle estado) {
        b = FragmentEsteMesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle estado) {
        // Partilhado com a Activity: o Histórico usa-o para abrir um mês aqui.
        vm = new ViewModelProvider(requireActivity()).get(EsteMesViewModel.class);

        destinos = new DestinosAdapter();
        b.rvDestinos.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvDestinos.setAdapter(destinos);
        historico = new HistoricoAdapter(vm::selecionarMes);
        b.rvHistorico.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvHistorico.setAdapter(historico);

        // Receita: o campo e o ViewModel mantêm-se em sintonia sem se chamarem um ao outro em ciclo.
        b.etReceita.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int i, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int antes, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                vm.setReceitaTexto(s.toString());
            }
        });
        vm.receitaTexto().observe(getViewLifecycleOwner(), texto -> {
            if (texto != null && !texto.contentEquals(b.etReceita.getText())) b.etReceita.setText(texto);
        });
        vm.somar().observe(getViewLifecycleOwner(), v -> {
            if (v != null && b.cbSomar.isChecked() != v) b.cbSomar.setChecked(v);
        });
        b.cbSomar.setOnCheckedChangeListener((cb, marcado) -> vm.setSomar(marcado));

        b.btnAnterior.setOnClickListener(v -> mudarMes(-1));
        b.btnSeguinte.setOnClickListener(v -> mudarMes(1));
        b.btnHoje.setOnClickListener(v -> vm.selecionarMes(vm.hoje()));
        b.btnGuardarReceita.setOnClickListener(v -> vm.guardarReceita());
        b.btnEscolherMes.setOnClickListener(v -> escolherMes());
        b.btnInvestir.setOnClickListener(v -> confirmarInvestir());
        b.btnAnular.setOnClickListener(v -> confirmarAnular());

        vm.estado().observe(getViewLifecycleOwner(), this::mostrar);
    }

    private EsteMesViewModel.Estado atual() {
        return vm.estado().getValue();
    }

    private void mudarMes(int passo) {
        EsteMesViewModel.Estado e = atual();
        if (e != null) vm.selecionarMes(Tempo.somarMeses(e.plano.mes().chave(), passo));
    }

    private void escolherMes() {
        EsteMesViewModel.Estado e = atual();
        if (e == null) return;
        List<String> chaves = e.mesesDisponiveis;
        String[] rotulos = chaves.stream().map(Tempo::rotuloMes).toArray(String[]::new);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.escolher_mes)
                .setItems(rotulos, (d, i) -> vm.selecionarMes(chaves.get(i)))
                .show();
    }

    private void confirmarInvestir() {
        EsteMesViewModel.Estado e = atual();
        if (e == null) return;
        boolean somar = b.cbSomar.isChecked();
        String extra = getString(somar ? R.string.confirmar_investir_somar : R.string.confirmar_investir_nao_somar);
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.confirmar_investir, Fmt.eur(e.plano.investir()), e.plano.mes().rotulo(), extra))
                .setPositiveButton(R.string.confirmar, (d, i) -> vm.investir(somar))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void confirmarAnular() {
        EsteMesViewModel.Estado e = atual();
        if (e == null) return;
        String extra = e.plano.mes().somado() ? getString(R.string.confirmar_anular_somado) : "";
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.confirmar_anular, e.plano.mes().rotulo(), extra))
                .setPositiveButton(R.string.confirmar, (d, i) -> vm.anular())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void mostrar(EsteMesViewModel.Estado e) {
        Plano p = e.plano;
        Plano.MesInfo mes = p.mes();
        boolean investido = mes.jaInvestido();
        boolean temReceita = !investido && p.receita() > 0;

        b.tvTitulo.setText(e.eHoje ? getString(R.string.este_mes) : mes.rotulo());
        b.tvSubtitulo.setText(e.eHoje ? mes.rotulo() : getString(e.futuro ? R.string.mes_futuro : R.string.mes_passado));
        b.btnEscolherMes.setText(mes.rotulo());
        b.btnHoje.setVisibility(e.eHoje ? View.GONE : View.VISIBLE);
        b.tvPerguntaReceita.setText(getString(R.string.quanto_recebeu, mes.rotulo()));
        b.etReceita.setEnabled(!investido);
        b.btnGuardarReceita.setVisibility(e.alterada ? View.VISIBLE : View.GONE);

        // Mês já registado
        b.cardInvestido.setVisibility(investido ? View.VISIBLE : View.GONE);
        if (investido) {
            b.tvInvestidoTitulo.setText(getString(R.string.mes_registado, mes.rotulo()));
            String onde = mes.aportes() == null ? "" : mes.aportes().entrySet().stream()
                    .filter(x -> x.getValue() > 0)
                    .map((Map.Entry<String, Double> x) -> nomeDe(p, x.getKey()) + " " + Fmt.eur(x.getValue()))
                    .collect(Collectors.joining(", "));
            String texto = getString(R.string.mes_registado_texto, Fmt.eur(mes.investido()), onde.isEmpty() ? "nada" : onde);
            if (!mes.somado()) texto += " " + getString(R.string.totais_nao_alterados);
            b.tvInvestidoTexto.setText(texto);
        }

        // Divisão da receita
        b.cardDivisao.setVisibility(temReceita ? View.VISIBLE : View.GONE);
        if (temReceita) preencherDivisao(p);

        // Onde investir
        boolean investe = temReceita && p.investir() > 0;
        b.cardInvestir.setVisibility(investe ? View.VISIBLE : View.GONE);
        b.cardSemInvestimento.setVisibility(temReceita && !investe ? View.VISIBLE : View.GONE);
        if (investe) {
            b.tvOndeInvestir.setText(getString(R.string.onde_investir, Fmt.eur(p.investir())));
            List<Plano.DestinoPlano> ordenados = new ArrayList<>(p.destinos());
            ordenados.sort((x, y) -> Double.compare(y.aporte(), x.aporte()));
            destinos.submitList(ordenados);
            String somarTexto = getString(R.string.somar_aos_totais);
            if (!e.eHoje && !e.futuro) somarTexto += " " + getString(R.string.somar_passado);
            b.cbSomar.setText(somarTexto);
        }

        preencherAvisos(p.avisos());

        b.cardHistorico.setVisibility(p.historico().isEmpty() ? View.GONE : View.VISIBLE);
        historico.definirChaveAberta(mes.chave());
        historico.submitList(p.historico());
    }

    private static String nomeDe(Plano p, String id) {
        return p.destinos().stream().filter(d -> d.id().equals(id)).map(Plano.DestinoPlano::nome).findFirst().orElse(id);
    }

    private void preencherDivisao(Plano p) {
        b.barraDivisao.removeAllViews();
        b.listaDivisao.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Plano.Linha l : p.divisao()) {
            int cor = Cores.divisao(requireContext(), l.id());
            View parte = new View(requireContext());
            parte.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) l.pct()));
            parte.setBackgroundColor(cor);
            b.barraDivisao.addView(parte);

            ItemLinhaDivisaoBinding linha = ItemLinhaDivisaoBinding.inflate(inflater, b.listaDivisao, false);
            linha.cor.setBackgroundColor(cor);
            int casas = Math.abs(l.pct() * 100 - Math.round(l.pct() * 100)) > 1e-6 ? 1 : 0;
            linha.tvNome.setText(l.nome() + " (" + Fmt.pct(l.pct(), casas) + ")");
            linha.tvValor.setText(Fmt.eur(l.valor()));
            b.listaDivisao.addView(linha.getRoot());
        }
    }

    private void preencherAvisos(List<String> avisos) {
        b.listaAvisos.removeAllViews();
        for (String aviso : avisos) {
            TextView t = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
            t.setLayoutParams(lp);
            int pad = (int) (12 * getResources().getDisplayMetrics().density);
            t.setPadding(pad, pad, pad, pad);
            t.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.aviso_fundo));
            t.setTextColor(ContextCompat.getColor(requireContext(), R.color.aviso_texto));
            if (aviso.contains("Os meus valores")) {
                t.setText(aviso + " " + getString(R.string.aviso_abrir_valores));
                t.setOnClickListener(v -> ((MainActivity) requireActivity()).irPara(MainActivity.SEPARADOR_VALORES));
            } else {
                t.setText(aviso);
            }
            b.listaAvisos.addView(t);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
