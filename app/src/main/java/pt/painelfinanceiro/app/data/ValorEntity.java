package pt.painelfinanceiro.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Quanto tem em cada tipo de ativo (uma linha por tipo). */
@Entity(tableName = "valores")
public class ValorEntity {
    @PrimaryKey
    @NonNull
    public String pilar = "";
    public double valor;
}
