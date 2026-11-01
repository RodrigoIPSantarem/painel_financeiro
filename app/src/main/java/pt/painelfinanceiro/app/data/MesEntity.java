package pt.painelfinanceiro.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Um mês ("2026-10"): receita e, se já investiu, quanto e onde. */
@Entity(tableName = "meses")
public class MesEntity {
    @PrimaryKey
    @NonNull
    public String chave = "";
    public double receita;
    @Nullable
    public Double investido;
    /** Aportes por tipo de ativo, em JSON ({"etfs":307.1,...}). */
    @Nullable
    public String aportesJson;
    public boolean somado;
    @Nullable
    public String atualizadoEm;
}
