package pt.painelfinanceiro.app.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {DefinicoesEntity.class, ValorEntity.class, MesEntity.class, RegistoEntity.class, MetaEntity.class},
        version = 2,
        exportSchema = false)
public abstract class PainelDatabase extends RoomDatabase {
    private static volatile PainelDatabase instancia;

    /** Versão 1 → 2: os registos passam a guardar o capital realmente investido. Os dados existentes mantêm-se. */
    public static final Migration MIGRACAO_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE registos ADD COLUMN investido REAL");
        }
    };

    public abstract PainelDao dao();

    public static PainelDatabase obter(Context contexto) {
        if (instancia == null) {
            synchronized (PainelDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(contexto.getApplicationContext(), PainelDatabase.class, "painel.db")
                            .addMigrations(MIGRACAO_1_2)
                            .build();
                }
            }
        }
        return instancia;
    }

    /** Só para testes: fecha a base de dados para a seguinte começar do zero. */
    @VisibleForTesting
    public static synchronized void reiniciar() {
        if (instancia != null) {
            instancia.close();
            instancia = null;
        }
    }
}
