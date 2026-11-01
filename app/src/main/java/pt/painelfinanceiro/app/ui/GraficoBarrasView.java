package pt.painelfinanceiro.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import pt.painelfinanceiro.app.R;

/**
 * Gráfico de barras desenhado à mão (View personalizada): para cada registo, uma barra com o
 * capital investido e outra com o valor que tinha.
 */
public class GraficoBarrasView extends View {

    /** Um grupo de barras: rótulo por baixo, capital investido (pode faltar) e valor. */
    public record Barra(String rotulo, Double investido, double valor) {
    }

    private final List<Barra> barras = new ArrayList<>();
    private final Paint pInvestido = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pValor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pEixo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    public GraficoBarrasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        pInvestido.setColor(ContextCompat.getColor(context, R.color.pilar_reserva));
        pValor.setColor(ContextCompat.getColor(context, R.color.primaria));
        pTexto.setColor(ContextCompat.getColor(context, R.color.apagado));
        pTexto.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11, getResources().getDisplayMetrics()));
        pTexto.setTextAlign(Paint.Align.CENTER);
        pEixo.setColor(ContextCompat.getColor(context, R.color.linha));
        pEixo.setStrokeWidth(2f);
    }

    public void setBarras(List<Barra> novas) {
        barras.clear();
        barras.addAll(novas);
        invalidate();
    }

    public List<Barra> getBarras() {
        return new ArrayList<>(barras);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (barras.isEmpty()) return;

        float dp = getResources().getDisplayMetrics().density;
        float alturaTexto = pTexto.getTextSize() * 1.6f;
        float base = getHeight() - getPaddingBottom() - alturaTexto;
        float topo = getPaddingTop() + 4 * dp;
        float esquerda = getPaddingLeft();
        float largura = getWidth() - getPaddingLeft() - getPaddingRight();

        double max = 0;
        for (Barra b : barras) {
            max = Math.max(max, b.valor());
            if (b.investido() != null) max = Math.max(max, b.investido());
        }
        if (max <= 0) max = 1;

        float grupo = largura / barras.size();
        float barra = Math.min(28 * dp, grupo * 0.32f);
        c.drawLine(esquerda, base, esquerda + largura, base, pEixo);

        for (int i = 0; i < barras.size(); i++) {
            Barra b = barras.get(i);
            float centro = esquerda + grupo * i + grupo / 2;
            if (b.investido() != null) {
                float h = (float) (b.investido() / max) * (base - topo);
                r.set(centro - barra - 2 * dp, base - h, centro - 2 * dp, base);
                c.drawRoundRect(r, 4 * dp, 4 * dp, pInvestido);
            }
            float h = (float) (b.valor() / max) * (base - topo);
            r.set(centro + 2 * dp, base - h, centro + barra + 2 * dp, base);
            c.drawRoundRect(r, 4 * dp, 4 * dp, pValor);
            c.drawText(b.rotulo(), centro, base + pTexto.getTextSize() * 1.3f, pTexto);
        }
    }
}
