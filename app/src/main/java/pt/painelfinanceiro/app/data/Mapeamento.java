package pt.painelfinanceiro.app.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.Tempo;

/** Converte entre as tabelas do Room e o objeto {@link Dados} usado pelas regras. */
public final class Mapeamento {
    private static final Gson GSON = new Gson();
    private static final Type MAPA = new TypeToken<LinkedHashMap<String, Double>>() {
    }.getType();

    private Mapeamento() {
    }

    public static Dados paraDados(PainelDao.Entidades e) {
        Dados d = Dados.vazios(Tempo.agora());

        if (e.definicoes != null && !e.definicoes.isEmpty()) {
            DefinicoesEntity x = e.definicoes.get(0);
            d.definicoes = new Dados.Definicoes(x.faseIdade, x.zonaDivida, x.despesasFixas, x.fundoEmergencia, x.atualizadoEm);
        }
        for (ValorEntity v : e.valores) d.valores.put(v.pilar, v.valor);
        for (MesEntity m : e.meses) {
            Map<String, Double> aportes = m.aportesJson == null ? null : GSON.<LinkedHashMap<String, Double>>fromJson(m.aportesJson, MAPA);
            d.meses.put(m.chave, new Dados.Mes(m.receita, m.investido, aportes, m.somado, m.atualizadoEm));
        }
        for (RegistoEntity r : e.registos) {
            d.registos.add(new Dados.Registo(r.data, r.tipo, r.etfs, r.acoes, r.terras, r.cripto, r.aportes, r.levantamentos, r.atualizadoEm, r.investido));
        }
        for (MetaEntity m : e.meta) {
            switch (m.chave) {
                case MetaEntity.VALORES_ATUALIZADO_EM:
                    d.valoresAtualizadoEm = m.valor;
                    break;
                case MetaEntity.PROTOCOLO_ATUALIZADO_EM:
                    d.protocoloAtualizadoEm = m.valor;
                    break;
                case MetaEntity.PROTOCOLO_JSON:
                    d.protocoloValores = m.valor == null ? new JsonObject() : JsonParser.parseString(m.valor).getAsJsonObject();
                    break;
                default:
                    break;
            }
        }
        return d;
    }

    public static PainelDao.Entidades deDados(Dados d) {
        PainelDao.Entidades e = new PainelDao.Entidades();

        DefinicoesEntity def = new DefinicoesEntity();
        def.faseIdade = d.definicoes.faseIdade();
        def.zonaDivida = d.definicoes.zonaDivida();
        def.despesasFixas = d.definicoes.despesasFixas();
        def.fundoEmergencia = d.definicoes.fundoEmergencia();
        def.atualizadoEm = d.definicoes.atualizadoEm();
        e.definicoes = List.of(def);

        e.valores = new ArrayList<>();
        for (Map.Entry<String, Double> v : d.valores.entrySet()) {
            ValorEntity x = new ValorEntity();
            x.pilar = v.getKey();
            x.valor = v.getValue();
            e.valores.add(x);
        }
        e.meses = new ArrayList<>();
        for (Map.Entry<String, Dados.Mes> m : d.meses.entrySet()) {
            MesEntity x = new MesEntity();
            x.chave = m.getKey();
            x.receita = m.getValue().receita();
            x.investido = m.getValue().investido();
            x.aportesJson = m.getValue().aportes() == null ? null : GSON.toJson(m.getValue().aportes());
            x.somado = m.getValue().somado();
            x.atualizadoEm = m.getValue().atualizadoEm();
            e.meses.add(x);
        }
        e.registos = new ArrayList<>();
        for (Dados.Registo r : d.registos) {
            RegistoEntity x = new RegistoEntity();
            x.data = r.data();
            x.tipo = r.tipo();
            x.etfs = r.etfs();
            x.acoes = r.acoes();
            x.terras = r.terras();
            x.cripto = r.cripto();
            x.aportes = r.aportes();
            x.levantamentos = r.levantamentos();
            x.atualizadoEm = r.atualizadoEm();
            x.investido = r.investido();
            e.registos.add(x);
        }
        e.meta = new ArrayList<>();
        e.meta.add(new MetaEntity(MetaEntity.VALORES_ATUALIZADO_EM, d.valoresAtualizadoEm));
        e.meta.add(new MetaEntity(MetaEntity.PROTOCOLO_JSON, d.protocoloValores.toString()));
        e.meta.add(new MetaEntity(MetaEntity.PROTOCOLO_ATUALIZADO_EM, d.protocoloAtualizadoEm));
        return e;
    }
}
