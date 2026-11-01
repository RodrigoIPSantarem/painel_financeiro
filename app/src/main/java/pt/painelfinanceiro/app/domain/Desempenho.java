package pt.painelfinanceiro.app.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tudo o que se mostra sobre o passado: cada registo com ganho e rentabilidade, e o resumo de cada ano.
 * Os aportes de cada período vêm da diferença do capital investido entre registos, quando o indicou;
 * caso contrário, usam-se os aportes guardados no registo.
 */
public final class Desempenho {
    private Desempenho() {
    }

    public record Linha(Dados.Registo registo, double valor, Double investido, Double ganho, Double ganhoPct,
                        double aportes, double levantamentos, double periodo, double acumulada) {
    }

    public record Ano(int ano, double receita, double investido, int mesesComReceita, int mesesInvestidos) {
    }

    public record Resumo(List<Linha> linhas, List<Ano> anos, double receitaTotal, double investidoNosMeses) {
        public Linha ultima() {
            return linhas.isEmpty() ? null : linhas.get(linhas.size() - 1);
        }

        public Linha primeira() {
            return linhas.isEmpty() ? null : linhas.get(0);
        }
    }

    public static Resumo calcular(Dados d) {
        List<Dados.Registo> regs = new ArrayList<>(d.registos);
        regs.sort((a, b) -> a.data().compareTo(b.data()));

        List<Returns.Ponto> pontos = new ArrayList<>();
        double[] ap = new double[regs.size()];
        double[] lev = new double[regs.size()];
        for (int i = 0; i < regs.size(); i++) {
            Dados.Registo r = regs.get(i);
            if (i == 0) {
                ap[i] = 0;
                lev[i] = 0;
            } else if (r.investido() != null && regs.get(i - 1).investido() != null) {
                double delta = Money.arredondar(r.investido() - regs.get(i - 1).investido());
                ap[i] = Math.max(0, delta);
                lev[i] = Math.max(0, -delta);
            } else {
                ap[i] = r.aportes();
                lev[i] = r.levantamentos();
            }
            pontos.add(new Returns.Ponto(r.valorTotal(), ap[i], lev[i]));
        }
        List<Returns.Retorno> ret = Returns.encadear(pontos);

        List<Linha> linhas = new ArrayList<>();
        for (int i = 0; i < regs.size(); i++) {
            Dados.Registo r = regs.get(i);
            Double ganho = r.ganho();
            Double pct = ganho == null || r.investido() == null || r.investido() == 0 ? null : ganho / r.investido();
            linhas.add(new Linha(r, Money.arredondar(r.valorTotal()), r.investido(), ganho, pct, ap[i], lev[i],
                    ret.get(i).periodo(), ret.get(i).acumulada()));
        }

        TreeMap<Integer, double[]> porAno = new TreeMap<>();
        double receitaTotal = 0;
        double investidoTotal = 0;
        for (Map.Entry<String, Dados.Mes> e : d.meses.entrySet()) {
            int ano = Integer.parseInt(e.getKey().substring(0, 4));
            double[] a = porAno.computeIfAbsent(ano, k -> new double[4]);
            Dados.Mes m = e.getValue();
            if (m.receita() > 0) {
                a[0] += m.receita();
                a[2]++;
                receitaTotal += m.receita();
            }
            if (m.jaInvestido()) {
                a[1] += m.investido();
                a[3]++;
                investidoTotal += m.investido();
            }
        }
        List<Ano> anos = new ArrayList<>();
        for (Map.Entry<Integer, double[]> e : porAno.descendingMap().entrySet()) {
            double[] a = e.getValue();
            anos.add(new Ano(e.getKey(), Money.arredondar(a[0]), Money.arredondar(a[1]), (int) a[2], (int) a[3]));
        }
        return new Resumo(linhas, anos, Money.arredondar(receitaTotal), Money.arredondar(investidoTotal));
    }
}
