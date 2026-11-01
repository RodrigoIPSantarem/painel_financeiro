package pt.painelfinanceiro.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class PainelDao {

    // --- observação (a interface reage sozinha a qualquer alteração) ---
    @Query("SELECT * FROM definicoes")
    public abstract LiveData<List<DefinicoesEntity>> observarDefinicoes();

    @Query("SELECT * FROM valores")
    public abstract LiveData<List<ValorEntity>> observarValores();

    @Query("SELECT * FROM meses")
    public abstract LiveData<List<MesEntity>> observarMeses();

    @Query("SELECT * FROM registos ORDER BY data")
    public abstract LiveData<List<RegistoEntity>> observarRegistos();

    @Query("SELECT * FROM meta")
    public abstract LiveData<List<MetaEntity>> observarMeta();

    // --- leitura síncrona (só fora do thread principal) ---
    @Query("SELECT * FROM definicoes")
    public abstract List<DefinicoesEntity> definicoes();

    @Query("SELECT * FROM valores")
    public abstract List<ValorEntity> valores();

    @Query("SELECT * FROM meses")
    public abstract List<MesEntity> meses();

    @Query("SELECT * FROM registos ORDER BY data")
    public abstract List<RegistoEntity> registos();

    @Query("SELECT * FROM meta")
    public abstract List<MetaEntity> meta();

    // --- escrita ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void inserirDefinicoes(List<DefinicoesEntity> itens);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void inserirValores(List<ValorEntity> itens);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void inserirMeses(List<MesEntity> itens);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void inserirRegistos(List<RegistoEntity> itens);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void inserirMeta(List<MetaEntity> itens);

    @Query("DELETE FROM definicoes")
    protected abstract void apagarDefinicoes();

    @Query("DELETE FROM valores")
    protected abstract void apagarValores();

    @Query("DELETE FROM meses")
    protected abstract void apagarMeses();

    @Query("DELETE FROM registos")
    protected abstract void apagarRegistos();

    @Query("DELETE FROM meta")
    protected abstract void apagarMeta();

    /** Troca tudo de uma vez: ou grava tudo, ou nada (transação). */
    @Transaction
    public void substituirTudo(Entidades e) {
        apagarDefinicoes();
        apagarValores();
        apagarMeses();
        apagarRegistos();
        apagarMeta();
        inserirDefinicoes(e.definicoes);
        inserirValores(e.valores);
        inserirMeses(e.meses);
        inserirRegistos(e.registos);
        inserirMeta(e.meta);
    }

    /** O conteúdo de todas as tabelas. */
    public static class Entidades {
        public List<DefinicoesEntity> definicoes;
        public List<ValorEntity> valores;
        public List<MesEntity> meses;
        public List<RegistoEntity> registos;
        public List<MetaEntity> meta;
    }

    public Entidades lerTudo() {
        Entidades e = new Entidades();
        e.definicoes = definicoes();
        e.valores = valores();
        e.meses = meses();
        e.registos = registos();
        e.meta = meta();
        return e;
    }
}
