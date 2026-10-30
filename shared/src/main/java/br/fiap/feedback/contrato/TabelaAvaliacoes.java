package br.fiap.feedback.contrato;

import br.fiap.feedback.domain.Avaliacao;

import java.time.LocalDate;

/**
 * Contrato da tabela DynamoDB de avaliações (nomes dos atributos e formato das chaves).
 *
 * <p>Usado por {@code receber-avaliacao} (grava) e {@code gerar-relatorio-semanal} (consulta).
 * <ul>
 *   <li>{@code pk} = {@code DIA#yyyy-MM-dd}: dia da avaliação em America/Sao_Paulo</li>
 *   <li>{@code sk} = {@code <dataEnvio ISO-8601>#<id>}</li>
 * </ul>
 * Com a partição por dia, o relatório faz 7 {@code Query} (uma por dia) em vez de um {@code Scan}.
 */
public final class TabelaAvaliacoes {

    public static final String PK = "pk";
    public static final String SK = "sk";
    public static final String ID = "id";
    public static final String DESCRICAO = "descricao";
    public static final String NOTA = "nota";
    public static final String URGENCIA = "urgencia";
    public static final String DATA_ENVIO = "dataEnvio";

    static final String PREFIXO_PARTICAO = "DIA#";

    private TabelaAvaliacoes() {
    }

    /** Chave de partição de um dia (usada pelo relatório para consultar dia a dia). */
    public static String chaveParticao(LocalDate dia) {
        return PREFIXO_PARTICAO + dia;
    }

    /** Chave de partição de uma avaliação: o dia dela no horário de Brasília. */
    public static String chaveParticao(Avaliacao avaliacao) {
        return chaveParticao(avaliacao.dia());
    }

    /** Chave de ordenação: data de envio + id (ordena por horário dentro do dia, sem colisão). */
    public static String chaveOrdenacao(Avaliacao avaliacao) {
        return avaliacao.dataEnvio() + "#" + avaliacao.id();
    }
}
