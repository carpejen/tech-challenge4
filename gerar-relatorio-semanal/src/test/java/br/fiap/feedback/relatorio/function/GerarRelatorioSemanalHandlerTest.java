package br.fiap.feedback.relatorio.function;

import br.fiap.feedback.relatorio.service.RelatorioSemanalService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GerarRelatorioSemanalHandlerTest {

    @Test
    void executaCasoDeUsoAoReceberEventoDoScheduler() {
        List<String> destinatarios = new ArrayList<>();
        RelatorioSemanalService service = new RelatorioSemanalService(
                dia -> List.of(),
                (destinatario, assunto, corpo) -> destinatarios.add(destinatario),
                "admin@exemplo.com",
                Clock.fixed(Instant.parse("2026-11-02T11:00:00Z"), ZoneOffset.UTC));

        GerarRelatorioSemanalHandler handler = new GerarRelatorioSemanalHandler(service);

        assertDoesNotThrow(() -> handler.handleRequest(Map.of("source", "aws.scheduler"), null));
        assertEquals(List.of("admin@exemplo.com"), destinatarios);
    }
}
