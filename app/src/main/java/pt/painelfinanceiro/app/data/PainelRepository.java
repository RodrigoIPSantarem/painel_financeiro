package pt.painelfinanceiro.app.data;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.RegraException;
import pt.painelfinanceiro.app.domain.Tempo;

/**
 * Única porta de acesso aos dados. A interface só observa {@link #dados()}; para alterar,
 * passa uma {@link Operacao} (uma regra pura) que corre fora do thread principal e grava no Room.
 */
public class PainelRepository {
    /** Uma alteração aos dados: recebe os atuais e devolve os novos (ou lança RegraException). */
    public interface Operacao {
        Dados aplicar(Dados atual);
    }

    private final PainelDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final MediatorLiveData<Dados> dados = new MediatorLiveData<>();
    private final MutableLiveData<Evento<String>> mensagens = new MutableLiveData<>();

    private List<DefinicoesEntity> definicoes;
    private List<ValorEntity> valores;
    private List<MesEntity> meses;
    private List<RegistoEntity> registos;
    private List<MetaEntity> meta;

    public PainelRepository(Context contexto) {
        dao = PainelDatabase.obter(contexto).dao();
        dados.addSource(dao.observarDefinicoes(), v -> { definicoes = v; combinar(); });
        dados.addSource(dao.observarValores(), v -> { valores = v; combinar(); });
        dados.addSource(dao.observarMeses(), v -> { meses = v; combinar(); });
        dados.addSource(dao.observarRegistos(), v -> { registos = v; combinar(); });
        dados.addSource(dao.observarMeta(), v -> { meta = v; combinar(); });
    }

    /** Só emite quando as cinco tabelas já responderam pelo menos uma vez. */
    private void combinar() {
        if (definicoes == null || valores == null || meses == null || registos == null || meta == null) return;
        PainelDao.Entidades e = new PainelDao.Entidades();
        e.definicoes = definicoes;
        e.valores = valores;
        e.meses = meses;
        e.registos = registos;
        e.meta = meta;
        dados.setValue(Mapeamento.paraDados(e));
    }

    public LiveData<Dados> dados() {
        return dados;
    }

    public LiveData<Evento<String>> mensagens() {
        return mensagens;
    }

    public ExecutorService io() {
        return io;
    }

    public void mensagem(String texto) {
        mensagens.postValue(new Evento<>(texto));
    }

    /** Lê os dados diretamente da base de dados (só fora do thread principal). */
    public Dados lerAtual() {
        return Mapeamento.paraDados(dao.lerTudo());
    }

    /** Corre a operação em segundo plano; se violar uma regra, mostra a mensagem em vez de gravar. */
    public void executar(Operacao operacao) {
        executar(operacao, null);
    }

    public void executar(Operacao operacao, String mensagemSucesso) {
        io.execute(() -> {
            try {
                Dados novo = operacao.aplicar(lerAtual());
                dao.substituirTudo(Mapeamento.deDados(novo));
                if (mensagemSucesso != null) mensagem(mensagemSucesso);
            } catch (RegraException e) {
                mensagem(e.getMessage());
            } catch (RuntimeException e) {
                mensagem("Não foi possível guardar: " + e.getMessage());
            }
        });
    }

    public static String agora() {
        return Tempo.agora();
    }
}
