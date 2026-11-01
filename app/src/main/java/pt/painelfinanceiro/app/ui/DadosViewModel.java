package pt.painelfinanceiro.app.ui;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import pt.painelfinanceiro.app.PainelApp;
import pt.painelfinanceiro.app.R;
import pt.painelfinanceiro.app.data.PainelRepository;
import pt.painelfinanceiro.app.domain.Dados;
import pt.painelfinanceiro.app.domain.DadosJson;
import pt.painelfinanceiro.app.domain.Operacoes;
import pt.painelfinanceiro.app.domain.RegraException;

/** Cópia de segurança e importação, feitas com o seletor de ficheiros do sistema (sem permissões). */
public class DadosViewModel extends AndroidViewModel {
    private final PainelRepository repo;

    public DadosViewModel(@NonNull Application app) {
        super(app);
        repo = ((PainelApp) app).repositorio();
    }

    public LiveData<Dados> dados() {
        return repo.dados();
    }

    private String texto(int id) {
        return getApplication().getString(id);
    }

    public void exportar(Uri destino) {
        repo.io().execute(() -> {
            try (OutputStream os = getApplication().getContentResolver().openOutputStream(destino, "wt")) {
                if (os == null) throw new IOException("sem fluxo de escrita");
                os.write(DadosJson.paraJson(repo.lerAtual()).getBytes(StandardCharsets.UTF_8));
                repo.mensagem(texto(R.string.msg_exportado));
            } catch (IOException | RuntimeException e) {
                repo.mensagem(texto(R.string.msg_erro_ficheiro));
            }
        });
    }

    public void importar(Uri origem, Operacoes.Modo modo) {
        repo.io().execute(() -> {
            try {
                Dados novo = DadosJson.deJson(lerTexto(origem));
                repo.executar(atual -> Operacoes.importar(atual, novo, modo),
                        texto(modo == Operacoes.Modo.JUNTAR ? R.string.msg_juntado : R.string.msg_substituido));
            } catch (RegraException e) {
                repo.mensagem(e.getMessage());
            } catch (IOException | RuntimeException e) {
                repo.mensagem(texto(R.string.msg_erro_ficheiro));
            }
        });
    }

    public void importarProtocolo(Uri origem) {
        repo.io().execute(() -> {
            try {
                JsonObject json = DadosJson.objeto(lerTexto(origem));
                repo.executar(d -> Operacoes.definirProtocolo(d, json, PainelRepository.agora()), texto(R.string.msg_protocolo_guardado));
            } catch (RegraException e) {
                repo.mensagem(e.getMessage());
            } catch (IOException | RuntimeException e) {
                repo.mensagem(texto(R.string.msg_erro_ficheiro));
            }
        });
    }

    public void reporProtocolo() {
        repo.executar(d -> Operacoes.definirProtocolo(d, new JsonObject(), PainelRepository.agora()), texto(R.string.msg_protocolo_reposto));
    }

    public void carregarExemplo() {
        repo.executar(d -> Operacoes.exemploFicticio(LocalDate.now(), PainelRepository.agora()), texto(R.string.msg_exemplo));
    }

    public void apagarTudo() {
        repo.executar(d -> Dados.vazios(PainelRepository.agora()), texto(R.string.msg_apagado));
    }

    private String lerTexto(Uri uri) throws IOException {
        try (InputStream in = getApplication().getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("sem fluxo de leitura");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
