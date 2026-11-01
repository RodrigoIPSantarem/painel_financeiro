package pt.painelfinanceiro.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Registo de rentabilidade (31 de dezembro ou aniversário). */
@Entity(tableName = "registos")
public class RegistoEntity {
    @PrimaryKey
    @NonNull
    public String data = "";
    @NonNull
    public String tipo = "31dez";
    public double etfs;
    public double acoes;
    public double terras;
    public double cripto;
    public double aportes;
    public double levantamentos;
    @Nullable
    public String atualizadoEm;
    /** Capital realmente investido até esta data (versão 2 da base de dados). */
    @Nullable
    public Double investido;
}
