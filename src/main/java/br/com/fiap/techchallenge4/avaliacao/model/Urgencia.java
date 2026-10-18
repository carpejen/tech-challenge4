package br.com.fiap.techchallenge4.avaliacao.model;

/**
 * Regra de projeto adotada porque o enunciado exige "urgência", mas não define
 * como ela deve ser calculada a partir da nota.
 */
public enum Urgencia {
    CRITICA,
    MEDIA,
    BAIXA;

    public static Urgencia fromNota(int nota) {
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("A nota deve estar entre 0 e 10.");
        }
        if (nota <= 3) {
            return CRITICA;
        }
        if (nota <= 6) {
            return MEDIA;
        }
        return BAIXA;
    }
}
