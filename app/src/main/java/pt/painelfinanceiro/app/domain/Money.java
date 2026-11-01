package pt.painelfinanceiro.app.domain;

/** Arredondamentos monetários. */
public final class Money {
    private Money() {
    }

    /** Arredonda a 2 casas (meio para cima), tolerando ruído de vírgula flutuante. */
    public static double arredondar(double valor) {
        return Math.signum(valor) * (Math.round((Math.abs(valor) + 1e-9) * 100.0) / 100.0);
    }
}
