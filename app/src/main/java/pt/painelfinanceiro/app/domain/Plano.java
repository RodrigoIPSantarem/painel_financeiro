package pt.painelfinanceiro.app.domain;

import java.util.List;
import java.util.Map;

/** O plano de um mês, pronto a mostrar. */
public record Plano(
        MesInfo mes,
        double receita,
        List<Linha> divisao,
        double investir,
        List<DestinoPlano> destinos,
        double totalAtual,
        List<Historico> historico,
        List<String> avisos) {

    public record MesInfo(String chave, String rotulo, boolean jaInvestido, Double investido,
                          Map<String, Double> aportes, boolean somado) {
    }

    /** Uma linha da divisão da receita (necessidades, desejos, fundo, investir...). */
    public record Linha(String id, String nome, double pct, double valor) {
    }

    public record PieItem(String ticker, double pct, double valor) {
    }

    public record DestinoPlano(String id, String nome, double meta, double atual, double real,
                               double aporte, String estado, List<PieItem> pieCore) {
    }

    public record Historico(String chave, String rotulo, double receita, Double investido) {
    }
}
