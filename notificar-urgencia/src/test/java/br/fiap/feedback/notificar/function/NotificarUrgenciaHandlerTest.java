package br.fiap.feedback.notificar.function;

import br.fiap.feedback.notificar.service.NotificacaoUrgenciaService;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificarUrgenciaHandlerTest {

    private final List<String> assuntos = new ArrayList<>();
    private final NotificarUrgenciaHandler handler = new NotificarUrgenciaHandler(
            new NotificacaoUrgenciaService((d, assunto, c) -> assuntos.add(assunto), "admin@exemplo.com"),
            new ObjectMapper());

    @Test
    void enviaUmEmailPorMensagemDaFila() {
        handler.handleRequest(evento("""
                {"id":"abc","descricao":"Áudio ruim","nota":2,"urgencia":"CRITICA","dataEnvio":"2026-11-03T14:22:10Z"}
                """), null);

        assertEquals(1, assuntos.size());
    }

    @Test
    void mensagemInvalidaLancaExcecaoParaIrParaADlq() {
        assertThrows(IllegalArgumentException.class, () -> handler.handleRequest(evento("não é json"), null));
        assertEquals(0, assuntos.size());
    }

    private static SQSEvent evento(String corpo) {
        SQSEvent.SQSMessage mensagem = new SQSEvent.SQSMessage();
        mensagem.setBody(corpo);
        SQSEvent evento = new SQSEvent();
        evento.setRecords(List.of(mensagem));
        return evento;
    }
}
