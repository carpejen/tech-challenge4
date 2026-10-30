package br.fiap.feedback.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Avaliação de uma aula enviada por um estudante.
 *
 * <p>O feedback é anônimo: não há dados do aluno nem da aula, só a descrição e a nota.
 * A urgência é calculada uma única vez, no recebimento, e gravada junto, para o histórico
 * continuar coerente com o que foi notificado mesmo se a regra mudar depois.
 *
 * @param id         identificador único (UUID)
 * @param descricao  texto livre do aluno (1 a {@value #TAMANHO_MAXIMO_DESCRICAO} caracteres)
 * @param nota       nota de 0 a 10
 * @param urgencia   urgência derivada da nota
 * @param dataEnvio  instante do recebimento (UTC)
 */
public record Avaliacao(String id, String descricao, int nota, Urgencia urgencia, Instant dataEnvio) {

    public static final int TAMANHO_MAXIMO_DESCRICAO = 1000;

    /** Fuso usado para decidir a que dia uma avaliação pertence (relatório e partição). */
    public static final ZoneId FUSO_REFERENCIA = ZoneId.of("America/Sao_Paulo");

    public Avaliacao {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(descricao, "descricao");
        Objects.requireNonNull(urgencia, "urgencia");
        Objects.requireNonNull(dataEnvio, "dataEnvio");
    }

    /**
     * Cria uma avaliação nova validando as regras de negócio.
     *
     * @throws AvaliacaoInvalidaException se a descrição ou a nota forem inválidas
     */
    public static Avaliacao nova(String id, String descricao, Integer nota, Instant recebidaEm) {
        if (descricao == null || descricao.isBlank()) {
            throw new AvaliacaoInvalidaException("descricao é obrigatória");
        }
        String descricaoNormalizada = descricao.strip();
        if (descricaoNormalizada.length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new AvaliacaoInvalidaException(
                    "descricao deve ter no máximo " + TAMANHO_MAXIMO_DESCRICAO + " caracteres");
        }
        if (nota == null) {
            throw new AvaliacaoInvalidaException("nota é obrigatória");
        }
        return new Avaliacao(id, descricaoNormalizada, nota, Urgencia.daNota(nota), recebidaEm);
    }

    /** Dia da avaliação no horário de Brasília (uma avaliação das 22h não "vira" o dia por causa do UTC). */
    public LocalDate dia() {
        return LocalDate.ofInstant(dataEnvio, FUSO_REFERENCIA);
    }
}
