package br.com.fiap.techchallenge4.relatorio.handler;

import br.com.fiap.techchallenge4.config.AwsClientFactory;
import br.com.fiap.techchallenge4.avaliacao.repository.AvaliacaoRepository;
import br.com.fiap.techchallenge4.notificacao.service.NotificacaoService;
import br.com.fiap.techchallenge4.relatorio.service.RelatorioService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

import java.time.Clock;

public class GerarRelatorioSemanalHandler implements RequestHandler<ScheduledEvent, Void> {

    private final RelatorioService relatorioService;
    private final NotificacaoService notificacaoService;
    private final Clock clock;

    public GerarRelatorioSemanalHandler() {
        var repository = new AvaliacaoRepository(
                AwsClientFactory.dynamoDbClient(),
                requiredEnv("TABLE_NAME")
        );
        this.relatorioService = new RelatorioService(repository);
        this.notificacaoService = new NotificacaoService(
                AwsClientFactory.snsClient(),
                requiredEnv("ADMIN_TOPIC_ARN")
        );
        this.clock = Clock.systemUTC();
    }

    @Override
    public Void handleRequest(ScheduledEvent event, Context context) {
        var relatorio = relatorioService.gerar(clock.instant());
        String texto = relatorioService.formatar(relatorio);
        notificacaoService.enviarRelatorio(texto);
        context.getLogger().log("Relatório semanal gerado com " + relatorio.totalAvaliacoes() + " avaliações.");
        return null;
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória não configurada: " + name);
        }
        return value;
    }
}
