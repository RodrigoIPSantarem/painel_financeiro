package pt.painelfinanceiro.app.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cópia de segurança em JSON. O formato é o mesmo da versão web da app, por isso os ficheiros
 * exportados de uma servem na outra.
 */
public final class DadosJson {
    private static final Pattern CHAVE_MES = Pattern.compile("\\d{4}-\\d{2}");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private DadosJson() {
    }

    public static String paraJson(Dados d) {
        JsonObject o = new JsonObject();
        o.addProperty("versao", Dados.VERSAO);

        JsonObject def = new JsonObject();
        def.addProperty("fase_idade", d.definicoes.faseIdade());
        def.addProperty("zona_divida", d.definicoes.zonaDivida());
        def.addProperty("despesas_fixas_mensais", d.definicoes.despesasFixas());
        def.addProperty("fundo_emergencia_saldo", d.definicoes.fundoEmergencia());
        def.addProperty("atualizadoEm", d.definicoes.atualizadoEm());
        o.add("definicoes", def);

        JsonObject valores = new JsonObject();
        for (Map.Entry<String, Double> e : d.valores.entrySet()) valores.addProperty(e.getKey(), e.getValue());
        o.add("valores", valores);
        o.addProperty("valoresAtualizadoEm", d.valoresAtualizadoEm);

        JsonObject prot = new JsonObject();
        prot.add("valores", d.protocoloValores.deepCopy());
        prot.addProperty("atualizadoEm", d.protocoloAtualizadoEm);
        o.add("protocolo", prot);

        JsonObject meses = new JsonObject();
        for (Map.Entry<String, Dados.Mes> e : d.meses.entrySet()) {
            Dados.Mes m = e.getValue();
            JsonObject j = new JsonObject();
            j.addProperty("receita", m.receita());
            if (m.jaInvestido()) {
                j.addProperty("investido", m.investido());
                JsonObject ap = new JsonObject();
                if (m.aportes() != null) for (Map.Entry<String, Double> a : m.aportes().entrySet()) ap.addProperty(a.getKey(), a.getValue());
                j.add("aportes", ap);
                j.addProperty("somado", m.somado());
            }
            j.addProperty("atualizadoEm", m.atualizadoEm());
            meses.add(e.getKey(), j);
        }
        o.add("meses", meses);

        JsonArray regs = new JsonArray();
        for (Dados.Registo r : d.registos) {
            JsonObject j = new JsonObject();
            j.addProperty("data", r.data());
            j.addProperty("tipo", r.tipo());
            j.addProperty("etfs", r.etfs());
            j.addProperty("acoes", r.acoes());
            j.addProperty("terras", r.terras());
            j.addProperty("cripto", r.cripto());
            j.addProperty("aportes", r.aportes());
            j.addProperty("levantamentos", r.levantamentos());
            j.addProperty("atualizadoEm", r.atualizadoEm());
            j.addProperty("investido", r.investido());
            regs.add(j);
        }
        o.add("registos", regs);
        return GSON.toJson(o);
    }

    /** Lê e valida uma cópia de segurança; preenche o que faltar. */
    public static Dados deJson(String texto) {
        JsonObject o;
        try {
            JsonElement el = com.google.gson.JsonParser.parseString(texto);
            if (!el.isJsonObject()) throw new RegraException("Ficheiro inválido: não é uma cópia de segurança do Painel Financeiro.");
            o = el.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new RegraException("Ficheiro inválido: não é um ficheiro .json legível.");
        }
        try {
            return converter(o);
        } catch (RegraException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RegraException("Ficheiro inválido: o conteúdo não tem o formato esperado.");
        }
    }

    /** Lê um ficheiro de parâmetros do protocolo (um objeto JSON). */
    public static JsonObject objeto(String texto) {
        try {
            JsonElement el = com.google.gson.JsonParser.parseString(texto);
            if (el.isJsonObject()) return el.getAsJsonObject();
        } catch (JsonParseException ignorado) {
            // cai na mensagem abaixo
        }
        throw new RegraException("Ficheiro inválido: não é um ficheiro .json legível.");
    }

    private static Dados converter(JsonObject o) {
        if (!o.has("versao") || !o.get("versao").isJsonPrimitive() || !o.get("versao").getAsJsonPrimitive().isNumber()) {
            throw new RegraException("Ficheiro inválido: não é uma cópia de segurança do Painel Financeiro.");
        }
        if (o.get("versao").getAsInt() > Dados.VERSAO) {
            throw new RegraException("Ficheiro inválido ou de uma versão mais recente da app. Atualize a app e tente de novo.");
        }
        if (!o.has("meses") || !o.get("meses").isJsonObject()) throw new RegraException("Ficheiro inválido: faltam os meses.");
        if (!o.has("registos") || !o.get("registos").isJsonArray()) throw new RegraException("Ficheiro inválido: faltam os registos.");

        Dados d = Dados.vazios("1970-01-01T00:00:00.000Z");

        if (o.has("definicoes") && o.get("definicoes").isJsonObject()) {
            JsonObject j = o.getAsJsonObject("definicoes");
            d.definicoes = new Dados.Definicoes(
                    texto(j, "fase_idade", d.definicoes.faseIdade()),
                    texto(j, "zona_divida", d.definicoes.zonaDivida()),
                    numeroOuNulo(j, "despesas_fixas_mensais"),
                    numeroOuNulo(j, "fundo_emergencia_saldo"),
                    texto(j, "atualizadoEm", d.definicoes.atualizadoEm()));
        }
        if (o.has("valores") && o.get("valores").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("valores").entrySet()) {
                d.valores.put(e.getKey(), e.getValue().getAsDouble());
            }
        }
        d.valoresAtualizadoEm = texto(o, "valoresAtualizadoEm", d.valoresAtualizadoEm);
        if (o.has("protocolo") && o.get("protocolo").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("protocolo");
            if (p.has("valores") && p.get("valores").isJsonObject()) d.protocoloValores = p.getAsJsonObject("valores").deepCopy();
            d.protocoloAtualizadoEm = texto(p, "atualizadoEm", d.protocoloAtualizadoEm);
        }

        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("meses").entrySet()) {
            if (!CHAVE_MES.matcher(e.getKey()).matches() || !e.getValue().isJsonObject()) {
                throw new RegraException("Ficheiro inválido: mês \"" + e.getKey() + "\".");
            }
            JsonObject j = e.getValue().getAsJsonObject();
            Double investido = numeroOuNulo(j, "investido");
            Map<String, Double> aportes = null;
            if (investido != null && j.has("aportes") && j.get("aportes").isJsonObject()) {
                aportes = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> a : j.getAsJsonObject("aportes").entrySet()) aportes.put(a.getKey(), a.getValue().getAsDouble());
            }
            double receita = numeroOuNulo(j, "receita") != null ? numeroOuNulo(j, "receita") : 0.0;
            boolean somado = j.has("somado") && j.get("somado").isJsonPrimitive() && j.get("somado").getAsBoolean();
            d.meses.put(e.getKey(), new Dados.Mes(receita, investido, aportes, somado, texto(j, "atualizadoEm", "")));
        }

        for (JsonElement el : o.getAsJsonArray("registos")) {
            JsonObject j = el.getAsJsonObject();
            String data = texto(j, "data", "");
            if (!Pattern.matches("\\d{4}-\\d{2}-\\d{2}", data)) throw new RegraException("Ficheiro inválido: data de registo \"" + data + "\".");
            d.registos.add(new Dados.Registo(data, texto(j, "tipo", "aniversario"),
                    num(j, "etfs"), num(j, "acoes"), num(j, "terras"), num(j, "cripto"),
                    num(j, "aportes"), num(j, "levantamentos"), texto(j, "atualizadoEm", ""), numeroOuNulo(j, "investido")));
        }
        d.registos.sort((a, b) -> a.data().compareTo(b.data()));
        return d;
    }

    private static String texto(JsonObject j, String chave, String omissao) {
        return j.has(chave) && j.get(chave).isJsonPrimitive() ? j.get(chave).getAsString() : omissao;
    }

    private static Double numeroOuNulo(JsonObject j, String chave) {
        return j.has(chave) && j.get(chave).isJsonPrimitive() ? j.get(chave).getAsDouble() : null;
    }

    private static double num(JsonObject j, String chave) {
        Double v = numeroOuNulo(j, chave);
        return v == null ? 0 : v;
    }
}
