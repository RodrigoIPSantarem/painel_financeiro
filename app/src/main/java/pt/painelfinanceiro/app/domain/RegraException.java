package pt.painelfinanceiro.app.domain;

/** Violação de uma regra de negócio; a mensagem está pronta a mostrar ao utilizador. */
public class RegraException extends RuntimeException {
    public RegraException(String mensagem) {
        super(mensagem);
    }
}
