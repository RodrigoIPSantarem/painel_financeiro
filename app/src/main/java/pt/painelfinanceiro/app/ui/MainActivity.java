package pt.painelfinanceiro.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import pt.painelfinanceiro.app.PainelApp;
import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.databinding.ActivityMainBinding;

/** Única Activity: separadores em cima e um ecrã (Fragment) por separador. */
public class MainActivity extends AppCompatActivity {
    public static final int SEPARADOR_ESTE_MES = 0;
    public static final int SEPARADOR_HISTORICO = 1;
    public static final int SEPARADOR_RENTABILIDADE = 2;
    public static final int SEPARADOR_VALORES = 3;
    public static final int SEPARADOR_DADOS = 4;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.pager.setAdapter(new PaginasAdapter(this));
        binding.pager.setOffscreenPageLimit(1);
        int[] titulos = {R.string.tab_este_mes, R.string.tab_historico, R.string.tab_rentabilidade, R.string.tab_valores, R.string.tab_dados};
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, posicao) -> tab.setText(titulos[posicao])).attach();

        // Mensagens (erros e confirmações) de qualquer ecrã aparecem aqui, uma só vez.
        ((PainelApp) getApplication()).repositorio().mensagens().observe(this, evento -> {
            String texto = evento == null ? null : evento.ler();
            if (texto != null) Snackbar.make(binding.getRoot(), texto, Snackbar.LENGTH_LONG).show();
        });
    }

    /** Usado pelos ecrãs para levar o utilizador a outro separador. */
    public void irPara(int separador) {
        binding.pager.setCurrentItem(separador, true);
    }
}
