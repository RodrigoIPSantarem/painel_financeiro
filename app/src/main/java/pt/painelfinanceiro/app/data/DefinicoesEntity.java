package pt.painelfinanceiro.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Definições que quase nunca mudam (uma única linha, id = 1). */
@Entity(tableName = "definicoes")
public class DefinicoesEntity {
    @PrimaryKey
    public int id = 1;
    @NonNull
    public String faseIdade = "23-30";
    @NonNull
    public String zonaDivida = "nenhuma";
    @Nullable
    public Double despesasFixas;
    @Nullable
    public Double fundoEmergencia;
    @Nullable
    public String atualizadoEm;
}
