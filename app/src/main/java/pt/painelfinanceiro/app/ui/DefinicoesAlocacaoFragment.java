package pt.painelfinanceiro.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;

import java.util.LinkedHashMap;
import java.util.Map;

import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.data.AlocacaoRepositorio;
import pt.painelfinanceiro.app.domain.Alocacao;

/**
 * Definições: quanto do investimento mensal vai para cada método.
 * Um slider por método (0 a 100), o total em direto e o botão Guardar só ativo quando a soma dá 100%.
 */
public class DefinicoesAlocacaoFragment extends Fragment {

    /**
     * EXEMPLO: troca pelos métodos e percentagens de origem da tua app (têm de somar 100).
     * A chave é um identificador fixo (não muda com a língua) e também é a chave gravada nas definições.
     */
    public static Map<String, Integer> porDefeito() {
        Map<String, Integer> p = new LinkedHashMap<>();
        p.put("etfs", 35);
        p.put("cripto", 20);
        p.put("acoes", 20);
        p.put("reits", 15);
        p.put("reserva", 10);
        return p;
    }

    /** Nome de cada método no ecrã, a partir de res/values/strings_alocacao.xml (traduzível). */
    private static final Map<String, Integer> NOMES = new LinkedHashMap<>();
    static {
        NOMES.put("etfs", R.string.metodo_etfs);
        NOMES.put("cripto", R.string.metodo_cripto);
        NOMES.put("acoes", R.string.metodo_acoes);
        NOMES.put("reits", R.string.metodo_reits);
        NOMES.put("reserva", R.string.metodo_reserva);
    }

    private AlocacaoRepositorio repositorio;
    private Alocacao alocacao;
    private LinearLayout lista;
    private TextView textoTotal;
    private Button botaoGuardar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View raiz = inflater.inflate(R.layout.fragment_definicoes_alocacao, container, false);
        lista = raiz.findViewById(R.id.lista_metodos);
        textoTotal = raiz.findViewById(R.id.texto_total);
        botaoGuardar = raiz.findViewById(R.id.botao_guardar);
        Button botaoRepor = raiz.findViewById(R.id.botao_repor);

        repositorio = new AlocacaoRepositorio(requireContext(), porDefeito());
        alocacao = repositorio.ler();
        mostrarLinhas(inflater);

        botaoGuardar.setOnClickListener(v -> {
            if (repositorio.guardar(alocacao)) {
                Toast.makeText(requireContext(), R.string.alocacao_guardada, Toast.LENGTH_SHORT).show();
            }
        });
        botaoRepor.setOnClickListener(v -> {
            repositorio.reporPorDefeito();
            alocacao = repositorio.ler();
            mostrarLinhas(inflater);
        });
        return raiz;
    }

    private void mostrarLinhas(LayoutInflater inflater) {
        lista.removeAllViews();
        for (Map.Entry<String, Integer> e : alocacao.percentagens().entrySet()) {
            String metodo = e.getKey();
            View linha = inflater.inflate(R.layout.item_alocacao, lista, false);
            TextView nome = linha.findViewById(R.id.texto_metodo);
            TextView valor = linha.findViewById(R.id.texto_percentagem);
            Slider slider = linha.findViewById(R.id.slider_percentagem);

            Integer idNome = NOMES.get(metodo);
            String nomeVisivel = idNome != null ? getString(idNome) : metodo;
            nome.setText(nomeVisivel);
            slider.setContentDescription(nomeVisivel);
            slider.setValue(e.getValue());
            valor.setText(getString(R.string.alocacao_percentagem, e.getValue()));

            slider.addOnChangeListener((s, novoValor, doUtilizador) -> {
                int percentagem = Math.round(novoValor);
                alocacao = alocacao.comPercentagem(metodo, percentagem);
                valor.setText(getString(R.string.alocacao_percentagem, percentagem));
                atualizarTotal();
            });
            lista.addView(linha);
        }
        atualizarTotal();
    }

    private void atualizarTotal() {
        int total = alocacao.total();
        int falta = alocacao.emFalta();
        if (falta == 0) {
            textoTotal.setText(getString(R.string.alocacao_total, total));
        } else if (falta > 0) {
            textoTotal.setText(getString(R.string.alocacao_total_falta, total, falta));
        } else {
            textoTotal.setText(getString(R.string.alocacao_total_sobra, total, -falta));
        }
        int cor = falta == 0 ? android.R.color.holo_green_dark : android.R.color.holo_red_dark;
        textoTotal.setTextColor(ContextCompat.getColor(requireContext(), cor));
        botaoGuardar.setEnabled(falta == 0);
    }
}
