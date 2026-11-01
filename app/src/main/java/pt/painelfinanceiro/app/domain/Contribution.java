package pt.painelfinanceiro.app.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reparte um aporte novo pelos tipos de ativo para aproximar o portefólio das metas,
 * sem nunca vender (rebalanceamento só com dinheiro novo).
 * <ol>
 *   <li>Calcula quanto cada tipo deveria valer depois do aporte (meta × novo total).</li>
 *   <li>Quem está abaixo recebe primeiro o que lhe falta; quem já está acima não recebe nada.</li>
 *   <li>Se o aporte chega para tudo o que falta, o resto reparte-se pelas metas;
 *       se não chega, reparte-se proporcionalmente ao que falta a cada um.</li>
 * </ol>
 */
public final class Contribution {
    private Contribution() {
    }

    public record Destino(String id, double meta, double atual, double real, double aporte, String estado) {
    }

    public static List<Destino> distribuir(Map<String, Double> atual, double aporte, Map<String, Double> metas) {
        Map<String, Double> ativas = new LinkedHashMap<>();
        double somaMetas = 0;
        for (Map.Entry<String, Double> e : metas.entrySet()) {
            if (e.getValue() > 0) {
                ativas.put(e.getKey(), e.getValue());
                somaMetas += e.getValue();
            }
        }
        if (somaMetas <= 0) {
            return new ArrayList<>();
        }
        Map<String, Double> pesos = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : ativas.entrySet()) {
            pesos.put(e.getKey(), e.getValue() / somaMetas);
        }

        double totalAntes = 0;
        for (String id : pesos.keySet()) {
            totalAntes += valor(atual, id);
        }
        double totalDepois = totalAntes + aporte;

        Map<String, Double> falta = new LinkedHashMap<>();
        double somaFalta = 0;
        for (Map.Entry<String, Double> e : pesos.entrySet()) {
            double f = Math.max(0, e.getValue() * totalDepois - valor(atual, e.getKey()));
            falta.put(e.getKey(), f);
            somaFalta += f;
        }

        Map<String, Double> bruto = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : pesos.entrySet()) {
            String id = e.getKey();
            double b;
            if (aporte <= 0) {
                b = 0;
            } else if (somaFalta <= aporte + 1e-9) {
                b = falta.get(id) + (aporte - somaFalta) * e.getValue();
            } else {
                b = aporte * falta.get(id) / somaFalta;
            }
            bruto.put(id, b);
        }
        Map<String, Double> arredondado = arredondarSomaExata(bruto, aporte);

        List<Destino> saida = new ArrayList<>();
        for (Map.Entry<String, Double> e : pesos.entrySet()) {
            String id = e.getKey();
            double v = valor(atual, id);
            double real = totalAntes > 0 ? v / totalAntes : 0;
            double diff = real - e.getValue();
            String estado = totalAntes <= 0 ? "abaixo" : diff < -0.03 ? "abaixo" : diff > 0.03 ? "acima" : "perto";
            saida.add(new Destino(id, arred6(e.getValue()), Math.round(v * 100.0) / 100.0, arred6(real), arredondado.get(id), estado));
        }
        return saida;
    }

    private static double valor(Map<String, Double> m, String id) {
        Double v = m.get(id);
        return v == null ? 0 : Math.max(0, v);
    }

    private static double arred6(double v) {
        return Math.round(v * 1e6) / 1e6;
    }

    /** Arredonda a cêntimos garantindo que a soma é exatamente o aporte (maiores restos). */
    private static Map<String, Double> arredondarSomaExata(Map<String, Double> valores, double total) {
        long alvo = Math.round(total * 100);
        Map<String, Long> cent = new LinkedHashMap<>();
        List<Map.Entry<String, Double>> restos = new ArrayList<>();
        long soma = 0;
        for (Map.Entry<String, Double> e : valores.entrySet()) {
            long c = (long) Math.floor(e.getValue() * 100 + 1e-9);
            cent.put(e.getKey(), c);
            soma += c;
            restos.add(Map.entry(e.getKey(), e.getValue() * 100 - c));
        }
        restos.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        long faltam = alvo - soma;
        for (Map.Entry<String, Double> r : restos) {
            if (faltam <= 0) break;
            cent.merge(r.getKey(), 1L, Long::sum);
            faltam--;
        }
        Map<String, Double> saida = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : cent.entrySet()) {
            saida.put(e.getKey(), e.getValue() / 100.0);
        }
        return saida;
    }
}
