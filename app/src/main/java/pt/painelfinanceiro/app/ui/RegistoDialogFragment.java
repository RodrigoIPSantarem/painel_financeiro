package pt.painelfinanceiro.app.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.DialogRegistoBinding;
import pt.painelfinanceiro.app.databinding.ItemCampoValorBinding;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.Tempo;

/** Diálogo para criar ou editar um registo de 31 de dezembro ou de aniversário. */
public class RegistoDialogFragment extends DialogFragment {
    private static final String ARG_DATA = "dataOriginal";
    static final List<String> TIPOS = List.of("31dez", "aniversario", "inicio");
    private static final String[] IDS = {"etfs", "acoes", "terras", "cripto"};
    private static final int[] ROTULOS = {R.string.campo_etfs, R.string.campo_acoes, R.string.campo_terras, R.string.campo_cripto};

    private String data;
    private DialogRegistoBinding b;
    private final Map<String, ItemCampoValorBinding> campos = new LinkedHashMap<>();

    public static RegistoDialogFragment novo() {
        return new RegistoDialogFragment();
    }

    public static RegistoDialogFragment editar(String data) {
        RegistoDialogFragment f = new RegistoDialogFragment();
        Bundle a = new Bundle();
        a.putString(ARG_DATA, data);
        f.setArguments(a);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle estado) {
        RentabilidadeViewModel vm = new ViewModelProvider(requireActivity()).get(RentabilidadeViewModel.class);
        String original = getArguments() == null ? null : getArguments().getString(ARG_DATA);
        Dados dados = vm.atuais();

        b = DialogRegistoBinding.inflate(LayoutInflater.from(requireContext()));
        String[] nomes = TIPOS.stream().map(t -> RegistosAdapter.nomeTipo(requireContext(), t)).toArray(String[]::new);
        b.spTipo.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, nomes));
        for (int i = 0; i < IDS.length; i++) {
            ItemCampoValorBinding c = ItemCampoValorBinding.inflate(LayoutInflater.from(requireContext()), b.listaValores, false);
            c.til.setHint(ROTULOS[i]);
            b.listaValores.addView(c.getRoot());
            campos.put(IDS[i], c);
        }

        Dados.Registo existente = null;
        if (original != null && dados != null) {
            for (Dados.Registo r : dados.registos) if (r.data().equals(original)) existente = r;
        }
        if (existente != null) {
            data = existente.data();
            b.spTipo.setSelection(Math.max(0, TIPOS.indexOf(existente.tipo())));
            b.etCapital.setText(Fmt.paraCampo(existente.investido()));
            campos.get("etfs").et.setText(Fmt.paraCampo(existente.etfs()));
            campos.get("acoes").et.setText(Fmt.paraCampo(existente.acoes()));
            campos.get("terras").et.setText(Fmt.paraCampo(existente.terras()));
            campos.get("cripto").et.setText(Fmt.paraCampo(existente.cripto()));
        } else {
            LocalDate hoje = LocalDate.now();
            data = LocalDate.of(hoje.getYear(), 12, 31).toString();
            if (dados != null) {
                for (String id : IDS) campos.get(id).et.setText(Fmt.paraCampo(dados.valores.getOrDefault(id, 0.0)));
            }
        }
        mostrarData();
        b.btnData.setOnClickListener(v -> {
            LocalDate base = LocalDate.parse(data);
            new DatePickerDialog(requireContext(), (p, ano, mes, dia) -> {
                data = LocalDate.of(ano, mes + 1, dia).toString();
                mostrarData();
            }, base.getYear(), base.getMonthValue() - 1, base.getDayOfMonth()).show();
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existente != null ? R.string.editar_registo : R.string.novo_registo_botao)
                .setView(b.getRoot())
                .setPositiveButton(R.string.guardar, (d, i) -> {
                    Map<String, Double> valores = new LinkedHashMap<>();
                    for (Map.Entry<String, ItemCampoValorBinding> e : campos.entrySet()) {
                        Double v = Fmt.lerNumero(e.getValue().et.getText().toString());
                        valores.put(e.getKey(), v == null ? 0.0 : v);
                    }
                    vm.guardar(original, data, TIPOS.get(b.spTipo.getSelectedItemPosition()),
                            Fmt.lerNumero(b.etCapital.getText().toString()), valores,
                            Fmt.lerNumero(b.etAportes.getText().toString()));
                })
                .setNegativeButton(R.string.cancelar, null)
                .create();
    }

    private void mostrarData() {
        b.btnData.setText(getString(R.string.data_x, Tempo.dataCurta(data)));
    }
}
