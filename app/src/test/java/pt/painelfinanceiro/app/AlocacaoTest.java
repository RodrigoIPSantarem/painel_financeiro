package pt.painelfinanceiro.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import pt.painelfinanceiro.app.domain.Alocacao;

public class AlocacaoTest {

    /** Exemplo de métodos e percentagens por defeito (troca pelos nomes que a app usa). */
    private static Alocacao exemplo() {
        Map<String, Integer> p = new LinkedHashMap<>();
        p.put("ETFs", 35);
        p.put("Criptoativos", 20);
        p.put("Ações", 20);
        p.put("REITs", 15);
        p.put("Reserva", 10);
        return new Alocacao(p);
    }

    @Test
    public void somaDe100EValida() {
        Alocacao a = exemplo();
        assertEquals(100, a.total());
        assertTrue(a.valida());
    }

    @Test
    public void somaAbaixoDe100NaoEValidaEDizQuantoFalta() {
        Alocacao a = exemplo().comPercentagem("ETFs", 30);
        assertFalse(a.valida());
        assertEquals(5, a.emFalta());
    }

    @Test
    public void somaAcimaDe100NaoEValidaEDizQuantoSobra() {
        Alocacao a = exemplo().comPercentagem("Reserva", 15);
        assertFalse(a.valida());
        assertEquals(-5, a.emFalta());
    }

    @Test
    public void zeroPorCentoEPermitido() {
        Alocacao a = exemplo().comPercentagem("Criptoativos", 0).comPercentagem("ETFs", 55);
        assertTrue(a.valida());
        assertEquals(0L, (long) a.repartir(10_000).get("Criptoativos"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void percentagemNegativaERecusada() {
        exemplo().comPercentagem("ETFs", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void percentagemAcimaDe100ERecusada() {
        exemplo().comPercentagem("ETFs", 101);
    }

    @Test(expected = IllegalStateException.class)
    public void naoRepartirSeASomaNaoDer100() {
        exemplo().comPercentagem("ETFs", 30).repartir(10_000);
    }

    @Test
    public void repartirDaOsValoresCertosAoCentimo() {
        Map<String, Long> partes = exemplo().repartir(25_000);          // 250,00 €
        assertEquals(8_750L, (long) partes.get("ETFs"));                 // 87,50 €
        assertEquals(5_000L, (long) partes.get("Criptoativos"));
        assertEquals(5_000L, (long) partes.get("Ações"));
        assertEquals(3_750L, (long) partes.get("REITs"));
        assertEquals(2_500L, (long) partes.get("Reserva"));
    }

    @Test
    public void asPartesSomamSempreOMontanteExato() {
        Map<String, Integer> p = new LinkedHashMap<>();
        p.put("A", 33);
        p.put("B", 33);
        p.put("C", 34);
        Alocacao a = new Alocacao(p);
        for (long montante = 0; montante <= 2_000; montante++) {        // de 0,00 € a 20,00 €
            long soma = 0;
            for (long parte : a.repartir(montante).values()) soma += parte;
            assertEquals("montante " + montante, montante, soma);
        }
        assertEquals(3L, (long) a.repartir(10).get("A"));                // 0,10 € -> 0,03 + 0,03 + 0,04
        assertEquals(4L, (long) a.repartir(10).get("C"));
    }
}
