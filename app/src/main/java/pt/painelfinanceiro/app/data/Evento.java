package pt.painelfinanceiro.app.data;

/** Mensagem de uso único (por exemplo, um erro): depois de lida, não volta a aparecer ao rodar o ecrã. */
public class Evento<T> {
    private final T conteudo;
    private boolean tratado;

    public Evento(T conteudo) {
        this.conteudo = conteudo;
    }

    /** Devolve o conteúdo só da primeira vez; depois devolve null. */
    public T ler() {
        if (tratado) return null;
        tratado = true;
        return conteudo;
    }
}
