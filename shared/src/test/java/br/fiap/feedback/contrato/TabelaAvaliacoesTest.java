package br.fiap.feedback.contrato;

import br.fiap.feedback.domain.Avaliacao;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabelaAvaliacoesTest {

    @Test
    void particaoEhODiaEmBrasiliaEOrdenacaoEhDataMaisId() {
        // 01:30 UTC do dia 4 = 22:30 do dia 3 em Brasília
        Avaliacao avaliacao = Avaliacao.nova("abc", "ok", 6, Instant.parse("2026-11-04T01:30:00Z"));

        assertEquals("DIA#2026-11-03", TabelaAvaliacoes.chaveParticao(avaliacao));
        assertEquals("2026-11-04T01:30:00Z#abc", TabelaAvaliacoes.chaveOrdenacao(avaliacao));
    }

    @Test
    void particaoDeUmDia() {
        assertEquals("DIA#2026-11-10", TabelaAvaliacoes.chaveParticao(LocalDate.of(2026, 11, 10)));
    }

    @Test
    void mensagemCriticaCopiaOsCamposDaAvaliacao() {
        Avaliacao avaliacao = Avaliacao.nova("abc", "Áudio ruim", 1, Instant.parse("2026-11-03T14:22:10Z"));

        MensagemAvaliacaoCritica mensagem = MensagemAvaliacaoCritica.de(avaliacao);

        assertEquals(new MensagemAvaliacaoCritica("abc", "Áudio ruim", 1, "CRITICA", "2026-11-03T14:22:10Z"), mensagem);
    }
}
