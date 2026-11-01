package pt.painelfinanceiro.app.domain;

import java.util.ArrayList;
import java.util.List;

/** Rentabilidade pelo método Dietz Modificado, encadeada entre registos. */
public final class Returns {
    private Returns() {
    }

    public record Ponto(double valorTotal, double aportes, double levantamentos) {
    }

    public record Retorno(double periodo, double acumulada) {
    }

    /** (Vf − Vi − aportes + levantamentos) ÷ (Vi + aportes÷2 − levantamentos÷2) */
    public static double periodo(double vi, double vf, double aportes, double levantamentos) {
        double den = vi + aportes / 2 - levantamentos / 2;
        if (den == 0) {
            return 0;
        }
        return (vf - vi - aportes + levantamentos) / den;
    }

    /** Pontos ordenados por data; o primeiro é o ponto de partida (0%). */
    public static List<Retorno> encadear(List<Ponto> pontos) {
        List<Retorno> saida = new ArrayList<>();
        Ponto anterior = null;
        double acumulada = 0;
        for (Ponto p : pontos) {
            if (anterior == null) {
                saida.add(new Retorno(0, 0));
            } else {
                double per = periodo(anterior.valorTotal(), p.valorTotal(), p.aportes(), p.levantamentos());
                acumulada = (1 + acumulada) * (1 + per) - 1;
                saida.add(new Retorno(per, acumulada));
            }
            anterior = p;
        }
        return saida;
    }
}
