package pt.painelfinanceiro.app.domain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parâmetros do protocolo de investimento. Os valores de fábrica são EXEMPLOS genéricos;
 * os pessoais entram por importação e ficam só no aparelho.
 */
public final class Protocolo {
    public final Map<String, String> zonasDivida = new LinkedHashMap<>();
    public final Map<String, String> pilares = new LinkedHashMap<>();
    /** fase de idade → (tipo de ativo → meta). Cada fase deve somar 100%. */
    public final Map<String, Map<String, Double>> matrizIdade = new LinkedHashMap<>();
    /** Como o valor de ETFs se divide pelos fundos da carteira (pesos somam 1). */
    public final Map<String, Double> pieCore = new LinkedHashMap<>();

    private Protocolo() {
    }

    public static Protocolo padrao() {
        Protocolo p = new Protocolo();
        p.zonasDivida.put("nenhuma", "Sem dívida");
        p.zonasDivida.put("verde", "Verde");
        p.zonasDivida.put("amarela", "Amarela");
        p.zonasDivida.put("laranja", "Laranja");
        p.zonasDivida.put("vermelha", "Vermelha");
        p.zonasDivida.put("emergencia", "Emergência");

        p.pilares.put("etfs", "ETFs Core");
        p.pilares.put("acoes", "Ações Individuais");
        p.pilares.put("cripto", "Cripto");
        p.pilares.put("terras", "Terras/REITs");
        p.pilares.put("reserva", "Reserva/Caixa");
        p.pilares.put("renda_fixa", "Renda Fixa");

        p.matrizIdade.put("23-30", fase(0.50, 0.15, 0.05, 0.15, 0.15, 0.00));
        p.matrizIdade.put("30-40", fase(0.45, 0.15, 0.05, 0.15, 0.10, 0.10));
        p.matrizIdade.put("40-50", fase(0.40, 0.10, 0.05, 0.15, 0.10, 0.20));
        p.matrizIdade.put("50+", fase(0.30, 0.10, 0.00, 0.15, 0.10, 0.35));

        p.pieCore.put("ETF-A", 0.60);
        p.pieCore.put("ETF-B", 0.40);
        return p;
    }

    private static Map<String, Double> fase(double etfs, double acoes, double cripto, double terras, double reserva, double rendaFixa) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("etfs", etfs);
        m.put("acoes", acoes);
        m.put("cripto", cripto);
        m.put("terras", terras);
        m.put("reserva", reserva);
        m.put("renda_fixa", rendaFixa);
        return m;
    }

    /**
     * Funde os parâmetros pessoais por cima dos de exemplo: a matriz funde-se por chave
     * e o pie_core é substituído inteiro (os pesos têm de somar 1).
     */
    public Protocolo fundir(JsonObject local) {
        Protocolo r = padrao();
        r.matrizIdade.clear();
        for (Map.Entry<String, Map<String, Double>> e : matrizIdade.entrySet()) {
            r.matrizIdade.put(e.getKey(), new LinkedHashMap<>(e.getValue()));
        }
        r.pieCore.clear();
        r.pieCore.putAll(pieCore);

        if (local == null) {
            return r;
        }
        if (local.has("matriz_idade") && local.get("matriz_idade").isJsonObject()) {
            for (Map.Entry<String, JsonElement> fase : local.getAsJsonObject("matriz_idade").entrySet()) {
                Map<String, Double> destino = r.matrizIdade.computeIfAbsent(fase.getKey(), k -> new LinkedHashMap<>());
                if (!fase.getValue().isJsonObject()) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> v : fase.getValue().getAsJsonObject().entrySet()) {
                    destino.put(v.getKey(), v.getValue().getAsDouble());
                }
            }
        }
        if (local.has("pie_core") && local.get("pie_core").isJsonObject()) {
            r.pieCore.clear();
            for (Map.Entry<String, JsonElement> v : local.getAsJsonObject("pie_core").entrySet()) {
                r.pieCore.put(v.getKey(), v.getValue().getAsDouble());
            }
        }
        return r;
    }
}
