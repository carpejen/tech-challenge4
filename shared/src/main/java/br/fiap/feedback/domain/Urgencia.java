package br.fiap.feedback.domain;

/**
 * Urgência de uma avaliação, derivada da nota.
 *
 * <p>O enunciado não define a regra; esta é a decisão do grupo (documentada no README):
 * <ul>
 *   <li>0 a 4: {@link #CRITICA} (grava e notifica os administradores na hora)</li>
 *   <li>5 a 7: {@link #MEDIA} (só grava; entra no relatório)</li>
 *   <li>8 a 10: {@link #BAIXA} (só grava; entra no relatório)</li>
 * </ul>
 */
public enum Urgencia {
    CRITICA,
    MEDIA,
    BAIXA;

    public static final int NOTA_MINIMA = 0;
    public static final int NOTA_MAXIMA = 10;
    private static final int LIMITE_CRITICA = 4;
    private static final int LIMITE_MEDIA = 7;

    /**
     * Classifica a urgência a partir da nota.
     *
     * @throws AvaliacaoInvalidaException se a nota estiver fora de 0 a 10
     */
    public static Urgencia daNota(int nota) {
        if (nota < NOTA_MINIMA || nota > NOTA_MAXIMA) {
            throw new AvaliacaoInvalidaException(
                    "nota deve estar entre " + NOTA_MINIMA + " e " + NOTA_MAXIMA);
        }
        if (nota <= LIMITE_CRITICA) {
            return CRITICA;
        }
        return nota <= LIMITE_MEDIA ? MEDIA : BAIXA;
    }

    /** Indica se a avaliação deve gerar aviso imediato aos administradores. */
    public boolean exigeNotificacaoImediata() {
        return this == CRITICA;
    }
}
