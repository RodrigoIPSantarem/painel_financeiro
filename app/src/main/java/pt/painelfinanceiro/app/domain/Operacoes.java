package pt.painelfinanceiro.app.domain;

import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Todas as alterações aos dados. Cada operação recebe os dados atuais e devolve uma cópia nova,
 * ou lança {@link RegraException} com a mensagem para o utilizador.
 */
public final class Operacoes {
    private Operacoes() {
    }

    public enum Modo {JUNTAR, SUBSTITUIR}

    public static Dados definirValores(Dados d, Map<String, Double> pilares, String agora) {
        Protocolo p = d.protocolo();
        Dados n = d.copia();
        for (Map.Entry<String, Double> e : pilares.entrySet()) {
            if (!p.pilares.containsKey(e.getKey())) {
                throw new RegraException("Tipo de ativo desconhecido: " + e.getKey() + ".");
            }
            Double v = e.getValue();
            if (v == null || v.isNaN() || v < 0) {
                throw new RegraException("Os valores têm de ser números iguais ou superiores a zero.");
            }
            n.valores.put(e.getKey(), Money.arredondar(v));
        }
        n.valoresAtualizadoEm = agora;
        return n;
    }

    public static Dados definirDefinicoes(Dados d, String fase, String zona, Double despesasFixas, Double fundo, String agora) {
        Protocolo p = d.protocolo();
        if (!p.matrizIdade.containsKey(fase)) throw new RegraException("Fase de idade inválida.");
        if (!p.zonasDivida.containsKey(zona)) throw new RegraException("Zona de dívida inválida.");
        if ((despesasFixas != null && despesasFixas < 0) || (fundo != null && fundo < 0)) {
            throw new RegraException("Os valores têm de ser números iguais ou superiores a zero.");
        }
        Dados n = d.copia();
        n.definicoes = new Dados.Definicoes(fase, zona, despesasFixas, fundo, agora);
        return n;
    }

    /** Parâmetros pessoais do protocolo (parcial); valida antes de guardar. */
    public static Dados definirProtocolo(Dados d, JsonObject parcial, String agora) {
        Protocolo efetivo = Protocolo.padrao().fundir(parcial);
        if (parcial.has("pie_core")) {
            double soma = 0;
            for (double v : efetivo.pieCore.values()) soma += v;
            if (Math.abs(soma - 1) > 1e-6) throw new RegraException("Os pesos do pie_core têm de somar 100%.");
        }
        for (Map.Entry<String, Map<String, Double>> fase : efetivo.matrizIdade.entrySet()) {
            for (Map.Entry<String, Double> v : fase.getValue().entrySet()) {
                if (!efetivo.pilares.containsKey(v.getKey()) || v.getValue() < 0) {
                    throw new RegraException("Matriz inválida na fase " + fase.getKey() + " (" + v.getKey() + ").");
                }
            }
        }
        Dados n = d.copia();
        n.protocoloValores = parcial.deepCopy();
        n.protocoloAtualizadoEm = agora;
        return n;
    }

    public static Dados guardarReceita(Dados d, String chave, double receita, String agora) {
        Dados.Mes atual = d.meses.get(chave);
        if (atual != null && atual.jaInvestido()) {
            throw new RegraException("Este mês já foi marcado como investido. Anule primeiro para alterar a receita.");
        }
        validarReceita(receita);
        Dados n = d.copia();
        n.meses.put(chave, new Dados.Mes(Money.arredondar(receita), null, null, false, agora));
        return n;
    }

    /**
     * Regista que investiu o plano do mês. Se {@code somar}, soma cada aporte ao total do respetivo tipo.
     * Em meses passados normalmente {@code somar} é falso: os valores atuais já incluem esse dinheiro.
     */
    public static Dados marcarInvestido(Dados d, String chave, double receita, boolean somar, String agora) {
        Dados.Mes atual = d.meses.get(chave);
        if (atual != null && atual.jaInvestido()) {
            throw new RegraException("Este mês já foi marcado como investido. Anule primeiro se quiser refazer.");
        }
        validarReceita(receita);

        Plano plano = PlanoService.calcular(d, d.protocolo(), chave, receita);
        Map<String, Double> aportes = new LinkedHashMap<>();
        for (Plano.DestinoPlano x : plano.destinos()) aportes.put(x.id(), x.aporte());

        Dados n = d.copia();
        if (somar) {
            for (Map.Entry<String, Double> e : aportes.entrySet()) {
                double antes = n.valores.getOrDefault(e.getKey(), 0.0);
                n.valores.put(e.getKey(), Money.arredondar(antes + e.getValue()));
            }
            n.valoresAtualizadoEm = agora;
        }
        n.meses.put(chave, new Dados.Mes(Money.arredondar(receita), plano.investir(), aportes, somar, agora));
        return n;
    }

    public static Dados desfazerInvestido(Dados d, String chave, String agora) {
        Dados.Mes atual = d.meses.get(chave);
        if (atual == null || !atual.jaInvestido()) {
            throw new RegraException("Este mês ainda não foi marcado como investido.");
        }
        Dados n = d.copia();
        if (atual.somado() && atual.aportes() != null) {
            for (Map.Entry<String, Double> e : atual.aportes().entrySet()) {
                double antes = n.valores.getOrDefault(e.getKey(), 0.0);
                n.valores.put(e.getKey(), Math.max(0, Money.arredondar(antes - e.getValue())));
            }
            n.valoresAtualizadoEm = agora;
        }
        n.meses.put(chave, new Dados.Mes(atual.receita(), null, null, false, agora));
        return n;
    }

    private static final List<String> TIPOS_REGISTO = List.of("inicio", "31dez", "aniversario");

    /** Versão curta: usa os valores atuais e não indica o capital investido. */
    public static Dados gerarRegisto(Dados d, String data, String tipo, Double aportes, Double levantamentos, String agora) {
        return gerarRegisto(d, data, tipo, null, null, aportes, levantamentos, agora);
    }

    /**
     * Cria (ou substitui, se a data já existir) o registo de uma data.
     *
     * @param investido capital realmente investido até essa data (opcional)
     * @param valores   valor de cada tipo de ativo nessa data; se nulo, usa "Os meus valores" (sem Reserva/Caixa)
     * @param aportes   aportes do período; se nulo, calcula-os: pela diferença do capital investido face ao
     *                  registo anterior, ou, sem esse dado, pela soma do que marcou como investido nos meses
     */
    public static Dados gerarRegisto(Dados d, String data, String tipo, Double investido, Map<String, Double> valores,
                                     Double aportes, Double levantamentos, String agora) {
        LocalDate dia;
        try {
            dia = LocalDate.parse(data);
        } catch (RuntimeException e) {
            throw new RegraException("Indique uma data válida.");
        }
        if (!TIPOS_REGISTO.contains(tipo)) throw new RegraException("Tipo de registo inválido.");
        if (investido != null && (investido.isNaN() || investido < 0)) {
            throw new RegraException("O capital investido tem de ser um número igual ou superior a zero.");
        }
        Map<String, Double> v = valores != null ? valores : d.valores;
        for (Double x : v.values()) {
            if (x != null && (x.isNaN() || x < 0)) throw new RegraException("Os valores têm de ser números iguais ou superiores a zero.");
        }

        Dados.Registo anterior = null;
        for (Dados.Registo r : d.registos) {
            if (r.data().compareTo(data) < 0 && (anterior == null || r.data().compareTo(anterior.data()) > 0)) anterior = r;
        }

        double ap;
        double lev = levantamentos != null ? Math.max(0, levantamentos) : 0;
        if (aportes != null) {
            ap = Math.max(0, aportes);
        } else if (anterior == null) {
            ap = 0;
        } else if (investido != null && anterior.investido() != null) {
            double delta = Money.arredondar(investido - anterior.investido());
            ap = Math.max(0, delta);
            if (delta < 0 && levantamentos == null) lev = -delta;
        } else {
            String de = anterior.data().substring(0, 7);
            String ate = data.substring(0, 7);
            double soma = 0;
            for (Map.Entry<String, Dados.Mes> e : d.meses.entrySet()) {
                if (e.getKey().compareTo(de) > 0 && e.getKey().compareTo(ate) <= 0 && e.getValue().jaInvestido()) {
                    soma += e.getValue().investido();
                }
            }
            ap = Money.arredondar(soma);
        }

        Dados n = d.copia();
        Dados.Registo novo = new Dados.Registo(dia.toString(), tipo, val(v, "etfs"), val(v, "acoes"), val(v, "terras"), val(v, "cripto"),
                ap, lev, agora, investido == null ? null : Money.arredondar(investido));
        n.registos.removeIf(r -> r.data().equals(novo.data()));
        n.registos.add(novo);
        n.registos.sort((x, y) -> x.data().compareTo(y.data()));
        return n;
    }

    /** Altera um registo existente (pode mudar a data). */
    public static Dados editarRegisto(Dados d, String dataOriginal, String data, String tipo, Double investido,
                                      Map<String, Double> valores, Double aportes, String agora) {
        boolean existe = d.registos.stream().anyMatch(r -> r.data().equals(dataOriginal));
        if (!existe) throw new RegraException("Esse registo já não existe.");
        if (!dataOriginal.equals(data) && d.registos.stream().anyMatch(r -> r.data().equals(data))) {
            throw new RegraException("Já existe um registo nessa data.");
        }
        return gerarRegisto(removerRegisto(d, dataOriginal), data, tipo, investido, valores, aportes, null, agora);
    }

    private static double val(Map<String, Double> v, String id) {
        Double x = v.get(id);
        return x == null ? 0 : Money.arredondar(x);
    }

    public static Dados removerRegisto(Dados d, String data) {
        Dados n = d.copia();
        n.registos.removeIf(r -> r.data().equals(data));
        return n;
    }

    /**
     * SUBSTITUIR troca tudo pelo ficheiro. JUNTAR combina os dois aparelhos: por mês, por registo e por bloco
     * (definições, valores, protocolo) ganha o mais recente.
     */
    public static Dados importar(Dados atual, Dados novo, Modo modo) {
        if (modo == Modo.SUBSTITUIR) return novo.copia();

        Dados r = atual.copia();
        for (Map.Entry<String, Dados.Mes> e : novo.meses.entrySet()) {
            Dados.Mes existente = r.meses.get(e.getKey());
            if (existente == null || nz(e.getValue().atualizadoEm()).compareTo(nz(existente.atualizadoEm())) > 0) {
                r.meses.put(e.getKey(), e.getValue());
            }
        }
        List<Dados.Registo> juntos = new ArrayList<>(r.registos);
        for (Dados.Registo x : novo.registos) {
            Dados.Registo o = null;
            for (Dados.Registo y : juntos) if (y.data().equals(x.data())) o = y;
            if (o == null) {
                juntos.add(x);
            } else if (nz(x.atualizadoEm()).compareTo(nz(o.atualizadoEm())) > 0) {
                juntos.remove(o);
                juntos.add(x);
            }
        }
        juntos.sort((a, b) -> a.data().compareTo(b.data()));
        r.registos.clear();
        r.registos.addAll(juntos);

        if (nz(novo.definicoes.atualizadoEm()).compareTo(nz(atual.definicoes.atualizadoEm())) > 0) r.definicoes = novo.definicoes;
        if (nz(novo.valoresAtualizadoEm).compareTo(nz(atual.valoresAtualizadoEm)) > 0) {
            r.valores.clear();
            r.valores.putAll(novo.valores);
            r.valoresAtualizadoEm = novo.valoresAtualizadoEm;
        }
        if (nz(novo.protocoloAtualizadoEm).compareTo(nz(atual.protocoloAtualizadoEm)) > 0) {
            r.protocoloValores = novo.protocoloValores.deepCopy();
            r.protocoloAtualizadoEm = novo.protocoloAtualizadoEm;
        }
        return r;
    }

    /** Dados FICTÍCIOS para experimentar a app. */
    public static Dados exemploFicticio(LocalDate hoje, String agora) {
        Dados d = Dados.vazios(agora);
        d = definirDefinicoes(d, "23-30", "verde", 585.0, 3000.0, agora);
        Map<String, Double> v = new LinkedHashMap<>();
        v.put("etfs", 1440.0);
        v.put("acoes", 560.0);
        v.put("cripto", 1200.0);
        v.put("terras", 1050.0);
        v.put("reserva", 1500.0);
        d = definirValores(d, v, agora);
        d = guardarReceita(d, Tempo.chaveMes(hoje), 1500, agora);
        d.registos.add(new Dados.Registo("2024-01-01", "inicio", 3000, 1000, 1000, 1000, 0, 0, agora, 6000.0));
        d.registos.add(new Dados.Registo("2024-12-31", "31dez", 3600, 1200, 1100, 1300, 800, 0, agora, 6800.0));
        d.registos.add(new Dados.Registo("2025-12-31", "31dez", 4200, 1400, 1250, 1500, 600, 0, agora, 7400.0));
        return d;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static void validarReceita(double r) {
        if (Double.isNaN(r) || r < 0) throw new RegraException("A receita tem de ser um número igual ou superior a zero.");
    }
}
