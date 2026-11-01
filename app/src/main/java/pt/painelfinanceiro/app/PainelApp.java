package pt.painelfinanceiro.app;

import android.app.Application;

import pt.painelfinanceiro.app.data.PainelRepository;

/** Cria o repositório uma só vez e partilha-o com todos os ViewModels. */
public class PainelApp extends Application {
    // Estático: mesmo que um ViewModel guarde uma referência a uma Application antiga
    // (acontece nos testes, onde há uma por teste), obtém sempre o repositório em uso.
    private static volatile PainelRepository repositorio;

    @Override
    public void onCreate() {
        super.onCreate();
        repositorio = new PainelRepository(this);
    }

    public PainelRepository repositorio() {
        return repositorio;
    }
}
