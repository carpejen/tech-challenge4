package br.fiap.feedback.relatorio.service;

import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.domain.Urgencia;
import br.fiap.feedback.relatorio.email.EnviadorRelatorio;
import br.fiap.feedback.relatorio.model.RelatorioSemanal;
import br.fiap.feedback.relatorio.repository.AvaliacaoConsultaRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Caso de uso: consolidar os sete dias anteriores e enviar o relatório ao administrador. */
@Singleton
public class RelatorioSemanalService {

    private static final Logger LOG = Logger.getLogger(RelatorioSemanalService.class);
    private static final ZoneId BRASILIA = Avaliacao.FUSO_REFERENCIA;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AvaliacaoConsultaRepository repository;
    private final EnviadorRelatorio enviador;
    private final String emailAdmin;
    private final Clock relogio;

    @Inject
    public RelatorioSemanalService(AvaliacaoConsultaRepository repository,
                                   EnviadorRelatorio enviador,
                                   @ConfigProperty(name = "app.email-admin") String emailAdmin,
                                   Clock relogio) {
        this.repository = repository;
        this.enviador = enviador;
        this.emailAdmin = emailAdmin;
        this.relogio = relogio;
    }

    /**
     * Gera o relatório dos sete dias completos anteriores ao dia da execução.
     * Como o Scheduler executa segunda às 08h (Brasília), o período normal é segunda a domingo.
     */
    public RelatorioSemanal gerarEEnviar() {
        LocalDate hoje = LocalDate.now(relogio.withZone(BRASILIA));
        LocalDate fim = hoje.minusDays(1);
        LocalDate inicio = fim.minusDays(6);

        List<Avaliacao> avaliacoes = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            avaliacoes.addAll(repository.buscarPorDia(dia));
        }

        RelatorioSemanal relatorio = RelatorioSemanal.consolidar(inicio, fim, avaliacoes);
        enviador.enviar(emailAdmin, montarAssunto(relatorio), montarCorpo(relatorio));

        LOG.infof("Relatorio semanal enviado periodo=%s..%s total=%d",
                relatorio.inicio(), relatorio.fim(), relatorio.totalAvaliacoes());
        return relatorio;
    }

    String montarAssunto(RelatorioSemanal relatorio) {
        return "[Feedback] Relatório semanal "
                + DATA.format(relatorio.inicio()) + " a " + DATA.format(relatorio.fim());
    }

    String montarCorpo(RelatorioSemanal relatorio) {
        StringBuilder corpo = new StringBuilder();
        corpo.append("Relatório semanal de feedbacks\n\n")
                .append("Período: ").append(DATA.format(relatorio.inicio()))
                .append(" a ").append(DATA.format(relatorio.fim())).append("\n")
                .append("Total de avaliações: ").append(relatorio.totalAvaliacoes()).append("\n")
                .append("Média das notas: ").append(formatarMedia(relatorio.mediaNotas())).append("\n\n")
                .append("Quantidade por dia:\n");

        relatorio.quantidadePorDia().forEach((dia, quantidade) ->
                corpo.append("- ").append(DATA.format(dia)).append(": ").append(quantidade).append("\n"));

        corpo.append("\nQuantidade por urgência:\n");
        for (Urgencia urgencia : Urgencia.values()) {
            corpo.append("- ").append(urgencia).append(": ")
                    .append(relatorio.quantidadePorUrgencia().getOrDefault(urgencia, 0L)).append("\n");
        }

        corpo.append("\nAvaliações:\n");
        if (relatorio.avaliacoes().isEmpty()) {
            corpo.append("Sem avaliações no período.\n");
        } else {
            for (Avaliacao avaliacao : relatorio.avaliacoes()) {
                corpo.append("\n- Data de envio: ")
                        .append(formatarDataHora(avaliacao.dataEnvio())).append(" (horário de Brasília)\n")
                        .append("  Urgência: ").append(avaliacao.urgencia()).append("\n")
                        .append("  Nota: ").append(avaliacao.nota()).append("\n")
                        .append("  Descrição: ").append(avaliacao.descricao()).append("\n");
            }
        }
        return corpo.toString();
    }

    private static String formatarMedia(Double media) {
        return media == null ? "N/A" : String.format(Locale.ROOT, "%.2f", media);
    }

    private static String formatarDataHora(Instant instante) {
        return instante.atZone(BRASILIA).format(DATA_HORA);
    }
}
