package pt.painelfinanceiro.app.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.FragmentDadosBinding;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.Operacoes;
import pt.painelfinanceiro.app.domain.Tempo;

public class DadosFragment extends Fragment {
    private FragmentDadosBinding b;
    private DadosViewModel vm;
    private Operacoes.Modo modoEscolhido = Operacoes.Modo.JUNTAR;

    // O seletor de ficheiros do sistema dispensa permissões de armazenamento.
    private final ActivityResultLauncher<String> escolherDestino =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) vm.exportar(uri);
            });
    private final ActivityResultLauncher<String[]> escolherOrigem =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), (Uri uri) -> {
                if (uri != null) vm.importar(uri, modoEscolhido);
            });
    private final ActivityResultLauncher<String[]> escolherProtocolo =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), (Uri uri) -> {
                if (uri != null) vm.importarProtocolo(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle estado) {
        b = FragmentDadosBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle estado) {
        vm = new ViewModelProvider(this).get(DadosViewModel.class);

        b.btnExportar.setOnClickListener(v -> escolherDestino.launch(nomeFicheiro()));
        b.btnImportar.setOnClickListener(v -> {
            modoEscolhido = b.rbSubstituir.isChecked() ? Operacoes.Modo.SUBSTITUIR : Operacoes.Modo.JUNTAR;
            if (modoEscolhido == Operacoes.Modo.SUBSTITUIR) {
                confirmar(R.string.confirmar_substituir, () -> escolherOrigem.launch(new String[]{"*/*"}));
            } else {
                escolherOrigem.launch(new String[]{"*/*"});
            }
        });
        b.btnImportarProtocolo.setOnClickListener(v -> escolherProtocolo.launch(new String[]{"*/*"}));
        b.btnReporProtocolo.setOnClickListener(v -> confirmar(R.string.confirmar_repor, vm::reporProtocolo));
        b.btnExemplo.setOnClickListener(v -> confirmar(R.string.confirmar_exemplo, vm::carregarExemplo));
        b.btnApagar.setOnClickListener(v -> confirmar(R.string.confirmar_apagar, vm::apagarTudo));

        vm.dados().observe(getViewLifecycleOwner(), this::mostrar);
    }

    private void mostrar(Dados d) {
        b.tvResumoDados.setText(getString(R.string.neste_aparelho, d.meses.size(), d.registos.size()));
        boolean pessoal = d.protocoloValores.size() > 0;
        b.tvProtocolo.setText(pessoal ? R.string.protocolo_pessoal : R.string.protocolo_exemplo);
        b.btnReporProtocolo.setVisibility(pessoal ? View.VISIBLE : View.GONE);
    }

    private void confirmar(int mensagem, Runnable acao) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(mensagem)
                .setPositiveButton(R.string.confirmar, (dialogo, i) -> acao.run())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private static String nomeFicheiro() {
        return "painel-dados-" + java.time.LocalDate.now() + ".json";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
