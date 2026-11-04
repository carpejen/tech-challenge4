package br.fiap.feedback.relatorio.service;

import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.domain.Urgencia;
import br.fiap.feedback.relatorio.model.RelatorioSemanal;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioSemanalServiceTest {

    private record Email(String destinatario, String assunto, String corpo) {}

    @Test
    void consultaSeteDiasAnterioresConsolidaEEnviaEmail() {
        List<LocalDate> diasConsultados = new ArrayList<>();
        Map<LocalDate, List<Avaliacao>> dados = new HashMap<>();
        dados.put(LocalDate.of(2026, 10, 26), List.of(
                avaliacao("1", "Áudio ruim", 2, Urgencia.CRITICA, "2026-10-26T14:00:00Z")));
        dados.put(LocalDate.of(2026, 10, 28), List.of(
                avaliacao("2", "Boa aula", 8, Urgencia.BAIXA, "2026-10-28T16:00:00Z")));

        List<Email> enviados = new ArrayList<>();
        RelatorioSemanalService service = new RelatorioSemanalService(
                dia -> {
                    diasConsultados.add(dia);
                    return dados.getOrDefault(dia, List.of());
                },
                (destinatario, assunto, corpo) -> enviados.add(new Email(destinatario, assunto, corpo)),
                "admin@exemplo.com",
                Clock.fixed(Instant.parse("2026-11-02T11:00:00Z"), ZoneOffset.UTC)); // segunda 08h Brasília

        RelatorioSemanal relatorio = service.gerarEEnviar();

        assertEquals(LocalDate.of(2026, 10, 26), relatorio.inicio());
        assertEquals(LocalDate.of(2026, 11, 1), relatorio.fim());
        assertEquals(7, diasConsultados.size());
        assertEquals(LocalDate.of(2026, 10, 26), diasConsultados.get(0));
        assertEquals(LocalDate.of(2026, 11, 1), diasConsultados.get(6));
        assertEquals(2, relatorio.totalAvaliacoes());
        assertEquals(5.0, relatorio.mediaNotas(), 0.0001);

        assertEquals(1, enviados.size());
        assertEquals("admin@exemplo.com", enviados.get(0).destinatario());
        assertTrue(enviados.get(0).assunto().contains("26/10/2026 a 01/11/2026"));
        assertTrue(enviados.get(0).corpo().contains("Média das notas: 5.00"));
        assertTrue(enviados.get(0).corpo().contains("CRITICA: 1"));
        assertTrue(enviados.get(0).corpo().contains("BAIXA: 1"));
        assertTrue(enviados.get(0).corpo().contains("Descrição: Áudio ruim"));
    }

    @Test
    void enviaRelatorioMesmoQuandoNaoHaAvaliacoes() {
        List<Email> enviados = new ArrayList<>();
        RelatorioSemanalService service = new RelatorioSemanalService(
                dia -> List.of(),
                (destinatario, assunto, corpo) -> enviados.add(new Email(destinatario, assunto, corpo)),
                "admin@exemplo.com",
                Clock.fixed(Instant.parse("2026-11-02T11:00:00Z"), ZoneOffset.UTC));

        RelatorioSemanal relatorio = service.gerarEEnviar();

        assertEquals(0, relatorio.totalAvaliacoes());
        assertEquals(1, enviados.size());
        assertTrue(enviados.get(0).corpo().contains("Média das notas: N/A"));
        assertTrue(enviados.get(0).corpo().contains("Sem avaliações no período."));
    }

    private static Avaliacao avaliacao(String id, String descricao, int nota, Urgencia urgencia, String data) {
        return new Avaliacao(id, descricao, nota, urgencia, Instant.parse(data));
    }
}
