package pt.painelfinanceiro.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Percentagem do investimento mensal que vai para cada método (ex.: ETFs, criptoativos, ações).
 *
 * Regras:
 * - cada percentagem está entre 0 e 100 (0 = o utilizador não usa esse método);
 * - a alocação só é válida quando a soma dá exatamente 100.
 *
 * As percentagens são inteiras para a soma ser exata: com double, 33.3 + 33.3 + 33.4 pode não dar 100.
 * A classe é imutável: cada alteração devolve uma nova Alocacao.
 * Não tem texto para o ecrã: as mensagens ficam em res/values/strings.xml (português e inglês).
 */
public final class Alocacao {

    public static final int TOTAL = 100;

    private final Map<String, Integer> percentagens;

    public Alocacao(Map<String, Integer> percentagens) {
        if (percentagens == null || percentagens.isEmpty()) {
            throw new IllegalArgumentException("Tem de haver pelo menos um método.");
        }
        Map<String, Integer> copia = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : percentagens.entrySet()) {
            Integer valor = e.getValue();
            if (valor == null || valor < 0 || valor > TOTAL) {
                throw new IllegalArgumentException("Percentagem inválida para " + e.getKey() + ": " + valor);
            }
            copia.put(e.getKey(), valor);
        }
        this.percentagens = Collections.unmodifiableMap(copia);
    }

    /** Percentagens por método, pela ordem em que foram definidas. */
    public Map<String, Integer> percentagens() {
        return percentagens;
    }

    public int percentagem(String metodo) {
        Integer valor = percentagens.get(metodo);
        if (valor == null) throw new IllegalArgumentException("Método desconhecido: " + metodo);
        return valor;
    }

    public int total() {
        int soma = 0;
        for (int valor : percentagens.values()) soma += valor;
        return soma;
    }

    /** Quanto falta para 100: positivo se falta distribuir, negativo se passa de 100. */
    public int emFalta() {
        return TOTAL - total();
    }

    public boolean valida() {
        return total() == TOTAL;
    }

    /** Nova alocação com a percentagem de um método alterada (os outros ficam iguais). */
    public Alocacao comPercentagem(String metodo, int valor) {
        if (!percentagens.containsKey(metodo)) throw new IllegalArgumentException("Método desconhecido: " + metodo);
        Map<String, Integer> nova = new LinkedHashMap<>(percentagens);
        nova.put(metodo, valor);
        return new Alocacao(nova);
    }

    /**
     * Reparte um montante (em cêntimos) pelos métodos, segundo as percentagens.
     * A soma das partes é sempre igual ao montante: os cêntimos que sobram dos arredondamentos
     * vão para os métodos com maior resto (método do maior resto).
     * Ex.: 100,00 € com 33/33/34 -> 33,00 € + 33,00 € + 34,00 €; 0,10 € com 33/33/34 -> 0,03 + 0,03 + 0,04.
     */
    public Map<String, Long> repartir(long montanteCentimos) {
        if (!valida()) {
            throw new IllegalStateException("A soma das percentagens tem de ser 100% (está em " + total() + "%).");
        }
        if (montanteCentimos < 0) throw new IllegalArgumentException("O montante não pode ser negativo.");
        Map<String, Long> partes = new LinkedHashMap<>();
        List<Map.Entry<String, Long>> restos = new ArrayList<>();
        long atribuido = 0;
        for (Map.Entry<String, Integer> e : percentagens.entrySet()) {
            long bruto = montanteCentimos * e.getValue();
            long parte = bruto / TOTAL;
            partes.put(e.getKey(), parte);
            restos.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey(), bruto % TOTAL));
            atribuido += parte;
        }
        long sobra = montanteCentimos - atribuido;            // sempre menor do que o n.º de métodos
        restos.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));   // ordenação estável: empates pela ordem original
        for (int i = 0; i < sobra; i++) {
            String metodo = restos.get(i).getKey();
            partes.put(metodo, partes.get(metodo) + 1);
        }
        return Collections.unmodifiableMap(partes);
    }
}
