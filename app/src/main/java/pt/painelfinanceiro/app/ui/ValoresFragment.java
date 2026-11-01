package pt.painelfinanceiro.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.FragmentValoresBinding;
import pt.painelfinanceiro.app.databinding.ItemCampoValorBinding;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.Protocolo;

public class ValoresFragment extends Fragment {
    private static final String[] IDS = {"etfs", "acoes", "terras", "cripto", "reserva", "renda_fixa"};
    private static final int[] ROTULOS = {R.string.campo_etfs, R.string.campo_acoes, R.string.campo_terras,
            R.string.campo_cripto, R.string.campo_reserva, R.string.campo_renda_fixa};

    private FragmentValoresBinding b;
    private ValoresViewModel vm;
    private final Map<String, ItemCampoValorBinding> campos = new LinkedHashMap<>();
    private final List<String> chavesZona = new ArrayList<>();
    private final List<String> chavesFase = new ArrayList<>();
    /** true quando o utilizador mexeu num campo e ainda não guardou: não se sobrepõe o que escreveu. */
    private boolean sujo;
    private boolean aPreencher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle estado) {
        b = FragmentValoresBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle estado) {
        vm = new ViewModelProvider(this).get(ValoresViewModel.class);

        TextWatcher alteracao = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int i, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int antes, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!aPreencher) sujo = true;
                atualizarTotal();
            }
        };

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < IDS.length; i++) {
            ItemCampoValorBinding campo = ItemCampoValorBinding.inflate(inflater, b.listaCampos, false);
            campo.til.setHint(ROTULOS[i]);
            campo.et.addTextChangedListener(alteracao);
            b.listaCampos.addView(campo.getRoot());
            campos.put(IDS[i], campo);
        }
        b.etDespesas.addTextChangedListener(alteracao);
        b.etFundo.addTextChangedListener(alteracao);
        AdapterView.OnItemSelectedListener selecao = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> pai, View v, int posicao, long id) {
                if (!aPreencher) sujo = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> pai) {
            }
        };
        b.spZona.setOnItemSelectedListener(selecao);
        b.spFase.setOnItemSelectedListener(selecao);

        b.btnGuardar.setOnClickListener(v -> guardar());
        vm.dados().observe(getViewLifecycleOwner(), d -> {
            if (!sujo) preencher(d);
        });
    }

    private void preencher(Dados d) {
        aPreencher = true;
        Protocolo p = d.protocolo();
        String fase = d.definicoes.faseIdade();

        for (int i = 0; i < IDS.length; i++) {
            String id = IDS[i];
            ItemCampoValorBinding campo = campos.get(id);
            double meta = p.matrizIdade.getOrDefault(fase, new LinkedHashMap<>()).getOrDefault(id, 0.0);
            double valor = d.valores.getOrDefault(id, 0.0);
            campo.getRoot().setVisibility(meta > 0 || valor > 0 ? View.VISIBLE : View.GONE);
            String texto = d.valores.containsKey(id) ? Fmt.paraCampo(valor) : "";
            if (!campo.et.getText().toString().equals(texto)) campo.et.setText(texto);
        }

        b.etDespesas.setText(Fmt.paraCampo(d.definicoes.despesasFixas()));
        b.etFundo.setText(Fmt.paraCampo(d.definicoes.fundoEmergencia()));

        chavesZona.clear();
        List<String> nomesZona = new ArrayList<>();
        for (Map.Entry<String, String> z : p.zonasDivida.entrySet()) {
            chavesZona.add(z.getKey());
            nomesZona.add(z.getKey().equals("nenhuma") ? getString(R.string.sem_dividas) : getString(R.string.zona_x, z.getValue()));
        }
        b.spZona.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, nomesZona));
        b.spZona.setSelection(Math.max(0, chavesZona.indexOf(d.definicoes.zonaDivida())));

        chavesFase.clear();
        chavesFase.addAll(p.matrizIdade.keySet());
        b.spFase.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, chavesFase));
        b.spFase.setSelection(Math.max(0, chavesFase.indexOf(fase)));

        aPreencher = false;
        atualizarTotal();
    }

    private void atualizarTotal() {
        if (b == null) return;
        double total = 0;
        for (ItemCampoValorBinding campo : campos.values()) {
            if (campo.getRoot().getVisibility() != View.VISIBLE) continue;
            Double v = Fmt.lerNumero(campo.et.getText().toString());
            if (v != null) total += v;
        }
        b.tvTotal.setText(getString(R.string.total_valor, Fmt.eur(total)));
    }

    private void guardar() {
        Map<String, Double> valores = new LinkedHashMap<>();
        for (Map.Entry<String, ItemCampoValorBinding> e : campos.entrySet()) {
            if (e.getValue().getRoot().getVisibility() != View.VISIBLE) continue;
            Double v = Fmt.lerNumero(e.getValue().et.getText().toString());
            valores.put(e.getKey(), v == null ? 0.0 : v);
        }
        String zona = chavesZona.isEmpty() ? "nenhuma" : chavesZona.get(Math.max(0, b.spZona.getSelectedItemPosition()));
        String fase = chavesFase.isEmpty() ? "23-30" : chavesFase.get(Math.max(0, b.spFase.getSelectedItemPosition()));
        sujo = false;
        vm.guardar(valores, fase, zona, Fmt.lerNumero(b.etDespesas.getText().toString()), Fmt.lerNumero(b.etFundo.getText().toString()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        campos.clear();
        b = null;
    }
}
