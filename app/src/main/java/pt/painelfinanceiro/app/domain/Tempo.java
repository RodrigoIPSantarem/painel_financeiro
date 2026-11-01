package pt.painelfinanceiro.app.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Datas, chaves de mês ("2026-10") e rótulos em português. */
public final class Tempo {
    private static final String[] MESES = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
    private static final String[] MESES_CURTOS = {"jan", "fev", "mar", "abr", "mai", "jun",
            "jul", "ago", "set", "out", "nov", "dez"};
    // Largura fixa: os carimbos comparam-se como texto, por isso têm de ter sempre o mesmo comprimento.
    private static final DateTimeFormatter CARIMBO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);

    private Tempo() {
    }

    public static String agora() {
        return CARIMBO.format(Instant.now());
    }

    public static String carimbo(Instant instante) {
        return CARIMBO.format(instante);
    }

    public static String chaveMes(LocalDate data) {
        return YearMonth.from(data).toString();
    }

    public static String rotuloMes(String chave) {
        YearMonth ym = YearMonth.parse(chave);
        return MESES[ym.getMonthValue() - 1] + " " + ym.getYear();
    }

    /** Soma n meses (negativo para recuar) a uma chave "AAAA-MM". */
    public static String somarMeses(String chave, int n) {
        return YearMonth.parse(chave).plusMonths(n).toString();
    }

    /** "2026-12-31" → "31 dez 2026". */
    public static String dataCurta(String iso) {
        LocalDate d = LocalDate.parse(iso);
        return d.getDayOfMonth() + " " + MESES_CURTOS[d.getMonthValue() - 1] + " " + d.getYear();
    }
}
