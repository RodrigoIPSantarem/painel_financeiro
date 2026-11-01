package pt.painelfinanceiro.app.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/** Formatação de valores à portuguesa (1500,00 €, 5,7%). */
public final class Fmt {
    private static final Locale PT = Locale.forLanguageTag("pt-PT");

    private Fmt() {
    }

    public static String eur(double valor) {
        NumberFormat f = NumberFormat.getCurrencyInstance(PT);
        f.setCurrency(Currency.getInstance("EUR"));
        return f.format(valor);
    }

    /** 0.057 → "5,7%". */
    public static String pct(double fracao, int casas) {
        NumberFormat f = NumberFormat.getNumberInstance(PT);
        f.setMinimumFractionDigits(casas);
        f.setMaximumFractionDigits(casas);
        return f.format(fracao * 100) + "%";
    }

    /** 0.057 → "+5,7%"; −0.006 → "−0,6%". */
    public static String spct(double fracao, int casas) {
        String t = pct(Math.abs(fracao), casas);
        return (fracao < 0 ? "−" : "+") + t;
    }

    /** Lê o que o utilizador escreveu ("1500,5" ou "1500.5"); texto vazio ou inválido dá null. */
    public static Double lerNumero(String texto) {
        if (texto == null) return null;
        String t = texto.trim().replace(',', '.');
        if (t.isEmpty()) return null;
        try {
            double v = Double.parseDouble(t);
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Número sem zeros inúteis, para pôr num campo de edição (1500.0 → "1500"). */
    public static String paraCampo(Double valor) {
        if (valor == null) return "";
        return BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString();
    }
}
