package pt.painelfinanceiro.app.domain;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tudo o que a app guarda, num só objeto. As regras alteram uma CÓPIA (ver {@link Operacoes}),
 * por isso o que a interface está a observar nunca muda "por baixo".
 */
public final class Dados {
    public static final int VERSAO = 1;

    public record Definicoes(String faseIdade, String zonaDivida, Double despesasFixas, Double fundoEmergencia, String atualizadoEm) {
    }

    /** Um mês: receita e, se já investiu, quanto e onde. */
    public record Mes(double receita, Double investido, Map<String, Double> aportes, boolean somado, String atualizadoEm) {
        public boolean jaInvestido() {
            return investido != null;
        }
    }

    /**
     * Fotografia do portefólio numa data (31 de dezembro ou aniversário).
     * {@code investido} é o capital que realmente investiu até essa data (pode faltar em registos antigos).
     */
    public record Registo(String data, String tipo, double etfs, double acoes, double terras, double cripto,
                          double aportes, double levantamentos, String atualizadoEm, Double investido) {
        /** Reserva/Caixa fica de fora, tal como no cálculo da rentabilidade. */
        public double valorTotal() {
            return etfs + acoes + terras + cripto;
        }

        /** Diferença entre o que tem e o que investiu (null se não indicou o capital investido). */
        public Double ganho() {
            return investido == null ? null : Money.arredondar(valorTotal() - investido);
        }
    }

    public Definicoes definicoes;
    public final Map<String, Double> valores = new LinkedHashMap<>();
    public String valoresAtualizadoEm;
    public JsonObject protocoloValores = new JsonObject();
    public String protocoloAtualizadoEm;
    /** Ordenados por chave ("2026-10"). */
    public final TreeMap<String, Mes> meses = new TreeMap<>();
    public final List<Registo> registos = new ArrayList<>();

    public static Dados vazios(String agora) {
        Dados d = new Dados();
        d.definicoes = new Definicoes("23-30", "nenhuma", null, null, agora);
        d.valoresAtualizadoEm = agora;
        d.protocoloAtualizadoEm = agora;
        return d;
    }

    public Dados copia() {
        Dados c = new Dados();
        c.definicoes = definicoes;
        c.valores.putAll(valores);
        c.valoresAtualizadoEm = valoresAtualizadoEm;
        c.protocoloValores = protocoloValores.deepCopy();
        c.protocoloAtualizadoEm = protocoloAtualizadoEm;
        for (Map.Entry<String, Mes> e : meses.entrySet()) {
            Mes m = e.getValue();
            c.meses.put(e.getKey(), new Mes(m.receita(), m.investido(),
                    m.aportes() == null ? null : new LinkedHashMap<>(m.aportes()), m.somado(), m.atualizadoEm()));
        }
        c.registos.addAll(registos);
        return c;
    }

    /** Protocolo em vigor: exemplo + parâmetros pessoais guardados. */
    public Protocolo protocolo() {
        return Protocolo.padrao().fundir(protocoloValores);
    }
}
