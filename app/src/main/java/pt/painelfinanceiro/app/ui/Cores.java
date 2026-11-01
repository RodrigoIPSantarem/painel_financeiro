package pt.painelfinanceiro.app.ui;

import android.content.Context;

import androidx.core.content.ContextCompat;

import pt.painelfinanceiro.app.R;

/** Cor associada a cada tipo de ativo e a cada parte da divisão da receita. */
public final class Cores {
    private Cores() {
    }

    public static int pilar(Context c, String id) {
        int cor;
        switch (id) {
            case "etfs": cor = R.color.pilar_etfs; break;
            case "acoes": cor = R.color.pilar_acoes; break;
            case "cripto": cor = R.color.pilar_cripto; break;
            case "terras": cor = R.color.pilar_terras; break;
            case "renda_fixa": cor = R.color.pilar_renda_fixa; break;
            default: cor = R.color.pilar_reserva; break;
        }
        return ContextCompat.getColor(c, cor);
    }

    public static int divisao(Context c, String id) {
        int cor;
        switch (id) {
            case "desejos": cor = R.color.div_desejos; break;
            case "fundo": cor = R.color.div_fundo; break;
            case "investimento": cor = R.color.div_investimento; break;
            case "amortizacao": cor = R.color.div_amortizacao; break;
            default: cor = R.color.div_despesas; break;
        }
        return ContextCompat.getColor(c, cor);
    }
}
