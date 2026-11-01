package pt.painelfinanceiro.app.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Junta as regras: dada a receita, quanto vai para cada fim e em que tipos de ativo investir. */
public final class PlanoService {
    private static final Map<String, String> NOMES = new LinkedHashMap<>();

    static {
        NOMES.put("despesas", "Necessidades");
        NOMES.put("desejos", "Desejos");
        NOMES.put("fundo", "Fundo de emergência");
        NOMES.put("investimento", "Investir");
        NOMES.put("amortizacao", "Amortizar dívida");
    }

    private PlanoService() {
    }

    /** @param receitaOuNula se nula, usa a receita guardada para o mês */
    public static Plano calcular(Dados d, Protocolo p, String chave, Double receitaOuNula) {
        Dados.Definicoes def = d.definicoes;
        Dados.Mes mes = d.meses.get(chave);
        double rec = Math.max(0, receitaOuNula != null ? receitaOuNula : (mes != null ? mes.receita() : 0));

        List<String> avisos = new ArrayList<>();
        double despesas = def.despesasFixas() != null ? def.despesasFixas() : 0;
        Double fundoMeses = null;
        if (def.fundoEmergencia() != null && despesas > 0) {
            fundoMeses = def.fundoEmergencia() / despesas;
        } else {
            avisos.add("Indique as despesas fixas mensais e o fundo de emergência em \"Os meus valores\" para o plano ser exato. Até lá assumo um fundo de 4 meses.");
        }

        Allocation.Resultado aloc = Allocation.calcular(rec, def.zonaDivida(), fundoMeses != null ? fundoMeses : 4.0, def.faseIdade(), p);

        List<Plano.Linha> divisao = new ArrayList<>();
        for (Map.Entry<String, Allocation.Quota> e : aloc.quotas().entrySet()) {
            if (e.getValue().pct() > 0) {
                divisao.add(new Plano.Linha(e.getKey(), NOMES.get(e.getKey()), e.getValue().pct(), e.getValue().valor()));
            }
        }

        double investir = aloc.quotas().get("investimento").valor();
        Map<String, Double> metas = p.matrizIdade.get(def.faseIdade());
        if (d.valores.isEmpty()) {
            avisos.add("Ainda não indicou quanto tem em cada tipo de ativo. Preencha em \"Os meus valores\"; até lá o aporte reparte-se pelas metas.");
        }

        List<Plano.DestinoPlano> destinos = new ArrayList<>();
        for (Contribution.Destino x : Contribution.distribuir(d.valores, investir, metas)) {
            List<Plano.PieItem> pie = null;
            if (x.id().equals("etfs") && x.aporte() > 0) {
                pie = new ArrayList<>();
                for (Map.Entry<String, Double> e : p.pieCore.entrySet()) {
                    pie.add(new Plano.PieItem(e.getKey(), e.getValue(), Money.arredondar(x.aporte() * e.getValue())));
                }
            }
            destinos.add(new Plano.DestinoPlano(x.id(), p.pilares.get(x.id()), x.meta(), x.atual(), x.real(), x.aporte(), x.estado(), pie));
        }

        if (investir <= 0 && rec > 0) {
            avisos.add("Este mês o protocolo não manda investir (fundo de emergência ou dívida em primeiro lugar).");
        }
        avisos.addAll(aloc.avisos());

        List<Plano.Historico> historico = new ArrayList<>();
        boolean atualTemReceita = mes != null && mes.receita() > 0;
        for (Map.Entry<String, Dados.Mes> e : d.meses.descendingMap().entrySet()) {
            if (e.getKey().equals(chave) && !atualTemReceita) continue;
            if (historico.size() >= 12) break;
            historico.add(new Plano.Historico(e.getKey(), Tempo.rotuloMes(e.getKey()), e.getValue().receita(), e.getValue().investido()));
        }

        double total = 0;
        for (double v : d.valores.values()) total += v;

        Plano.MesInfo info = new Plano.MesInfo(chave, Tempo.rotuloMes(chave),
                mes != null && mes.jaInvestido(), mes != null ? mes.investido() : null,
                mes != null ? mes.aportes() : null, mes != null && mes.somado());

        return new Plano(info, rec, divisao, investir, destinos, Money.arredondar(total), historico, avisos);
    }
}
