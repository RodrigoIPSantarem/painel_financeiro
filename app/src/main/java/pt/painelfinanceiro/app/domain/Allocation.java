package pt.painelfinanceiro.app.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Divisão da receita do mês por situação financeira (dívidas e fundo de emergência). */
public final class Allocation {
    private Allocation() {
    }

    public record Quotas(double despesas, double desejos, double fundo, double investimento, double amortizacao) {
    }

    public record Quota(double pct, double valor) {
    }

    public record Resultado(String zona, double fundoMeses, String fase, Map<String, Quota> quotas, List<String> avisos) {
    }

    public static Quotas regras(String zona, double fundoMeses) {
        if ("emergencia".equals(zona)) {
            return new Quotas(0.5, 0.15, 0.14, 0, 0.21); // domina sobre o fundo
        }
        if (fundoMeses < 3) {
            return new Quotas(0.5, 0.15, 0.35, 0, 0); // fundo primeiro
        }
        switch (zona) {
            case "nenhuma":
                return fundoMeses < 6 ? new Quotas(0.5, 0.15, 0.10, 0.25, 0) : new Quotas(0.5, 0.15, 0, 0.35, 0);
            case "verde":
                return new Quotas(0.5, 0.15, 0.10, 0.25, 0);
            case "amarela":
                return new Quotas(0.5, 0.15, 0.10, 0.175, 0.075);
            case "laranja":
                return new Quotas(0.5, 0.15, 0.10, 0, 0.25);
            case "vermelha":
                return fundoMeses >= 6 ? new Quotas(0.5, 0.15, 0, 0, 0.35) : new Quotas(0.5, 0.15, 0.10, 0, 0.25);
            default:
                throw new RegraException("Zona de dívida desconhecida: " + zona);
        }
    }

    /** Zona da dívida a partir da TAEG (em %). */
    public static String zonaPorTaeg(Double taeg) {
        if (taeg == null || taeg <= 0) return "nenhuma";
        if (taeg < 3.5) return "verde";
        if (taeg < 6.0) return "amarela";
        if (taeg < 9.0) return "laranja";
        if (taeg < 15.0) return "vermelha";
        return "emergencia";
    }

    public static Resultado calcular(double receita, String zona, double fundoMeses, String fase, Protocolo protocolo) {
        Map<String, Double> matriz = protocolo.matrizIdade.get(fase);
        if (matriz == null) {
            throw new RegraException("Fase de idade desconhecida: " + fase);
        }
        Quotas q = regras(zona, fundoMeses);
        Map<String, Quota> quotas = new LinkedHashMap<>();
        quotas.put("despesas", quota(receita, q.despesas()));
        quotas.put("desejos", quota(receita, q.desejos()));
        quotas.put("fundo", quota(receita, q.fundo()));
        quotas.put("investimento", quota(receita, q.investimento()));
        quotas.put("amortizacao", quota(receita, q.amortizacao()));

        List<String> avisos = new ArrayList<>();
        double soma = 0;
        for (double v : matriz.values()) soma += v;
        if (Math.abs(soma - 1.0) > 1e-9) {
            String texto = String.format(java.util.Locale.forLanguageTag("pt-PT"), "%.1f", soma * 100).replace(",0", "");
            avisos.add("A matriz da fase " + fase + " soma " + texto + "% em vez de 100%. Verifique os parâmetros do protocolo.");
        }
        return new Resultado(zona, Math.round(fundoMeses * 10.0) / 10.0, fase, quotas, avisos);
    }

    private static Quota quota(double receita, double pct) {
        return new Quota(pct, Money.arredondar(receita * pct));
    }
}
