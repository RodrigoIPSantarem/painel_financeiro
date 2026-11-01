package pt.painelfinanceiro.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Pares chave-valor: carimbos dos valores e parâmetros pessoais do protocolo (JSON). */
@Entity(tableName = "meta")
public class MetaEntity {
    public static final String VALORES_ATUALIZADO_EM = "valoresAtualizadoEm";
    public static final String PROTOCOLO_JSON = "protocoloJson";
    public static final String PROTOCOLO_ATUALIZADO_EM = "protocoloAtualizadoEm";

    @PrimaryKey
    @NonNull
    public String chave = "";
    @Nullable
    public String valor;

    public MetaEntity() {
    }

    public MetaEntity(@NonNull String chave, @Nullable String valor) {
        this.chave = chave;
        this.valor = valor;
    }
}
