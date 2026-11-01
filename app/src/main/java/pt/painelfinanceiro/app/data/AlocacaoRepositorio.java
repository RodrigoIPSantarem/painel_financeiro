package pt.painelfinanceiro.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

import pt.painelfinanceiro.app.domain.Alocacao;

/**
 * Guarda as percentagens escolhidas pelo utilizador nas definições do telemóvel (SharedPreferences).
 * Não altera a base de dados Room, por isso não é preciso migração nem se perdem registos.
 */
public final class AlocacaoRepositorio {

    private static final String FICHEIRO = "alocacao_metodos";

    private final SharedPreferences prefs;
    private final Map<String, Integer> porDefeito;

    public AlocacaoRepositorio(Context contexto, Map<String, Integer> porDefeito) {
        this.prefs = contexto.getApplicationContext().getSharedPreferences(FICHEIRO, Context.MODE_PRIVATE);
        this.porDefeito = new LinkedHashMap<>(porDefeito);
    }

    /** Percentagens guardadas; se não houver (ou estiverem inválidas), as de origem. Nunca devolve uma alocação inválida. */
    public Alocacao ler() {
        Map<String, Integer> valores = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : porDefeito.entrySet()) {
            valores.put(e.getKey(), prefs.getInt(e.getKey(), e.getValue()));
        }
        try {
            Alocacao guardada = new Alocacao(valores);
            if (guardada.valida()) return guardada;
        } catch (IllegalArgumentException ignorada) {
            // valor fora de 0..100 nas definições: usar as percentagens de origem
        }
        return new Alocacao(porDefeito);
    }

    /** Só grava se a soma der 100%. Devolve false (e não grava nada) se não der. */
    public boolean guardar(Alocacao alocacao) {
        if (!alocacao.valida()) return false;
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, Integer> e : alocacao.percentagens().entrySet()) {
            editor.putInt(e.getKey(), e.getValue());
        }
        editor.apply();
        return true;
    }

    public void reporPorDefeito() {
        prefs.edit().clear().apply();
    }
}
