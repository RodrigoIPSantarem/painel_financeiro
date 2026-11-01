package pt.painelfinanceiro.app.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/** Os cinco ecrãs da app, um por separador. */
public class PaginasAdapter extends FragmentStateAdapter {
    public static final int TOTAL = 5;

    public PaginasAdapter(@NonNull FragmentActivity atividade) {
        super(atividade);
    }

    @NonNull
    @Override
    public Fragment createFragment(int posicao) {
        switch (posicao) {
            case MainActivity.SEPARADOR_ESTE_MES: return new EsteMesFragment();
            case MainActivity.SEPARADOR_HISTORICO: return new HistoricoFragment();
            case MainActivity.SEPARADOR_RENTABILIDADE: return new RentabilidadeFragment();
            case MainActivity.SEPARADOR_VALORES: return new ValoresFragment();
            default: return new DadosFragment();
        }
    }

    @Override
    public int getItemCount() {
        return TOTAL;
    }
}
