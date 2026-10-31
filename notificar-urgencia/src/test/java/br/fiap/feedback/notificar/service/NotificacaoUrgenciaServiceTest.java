package br.fiap.feedback.notificar.service;

import br.fiap.feedback.contrato.MensagemAvaliacaoCritica;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacaoUrgenciaServiceTest {

    private record EmailEnviado(String destinatario, String assunto, String corpo) {}

    private final List<EmailEnviado> enviados = new ArrayList<>();
    private final NotificacaoUrgenciaService service = new NotificacaoUrgenciaService(
            (destinatario, assunto, corpo) -> enviados.add(new EmailEnviado(destinatario, assunto, corpo)),
            "admin@exemplo.com");

    private final MensagemAvaliacaoCritica mensagem = new MensagemAvaliacaoCritica(
            "abc", "Áudio ruim", 2, "CRITICA", "2026-11-03T14:22:10Z");

    @Test
    void enviaUmEmailParaOAdministrador() {
        service.notificar(mensagem);

        assertEquals(1, enviados.size());
        assertEquals("admin@exemplo.com", enviados.get(0).destinatario());
        assertTrue(enviados.get(0).assunto().contains("CRITICA"));
    }

    @Test
    void corpoTrazDescricaoUrgenciaEDataDeEnvioEmBrasilia() {
        String corpo = service.montarCorpo(mensagem);

        assertTrue(corpo.contains("Descrição: Áudio ruim"));
        assertTrue(corpo.contains("Urgência: CRITICA"));
        assertTrue(corpo.contains("03/11/2026 11:22:10")); // 14:22 UTC = 11:22 em Brasília
    }
}
