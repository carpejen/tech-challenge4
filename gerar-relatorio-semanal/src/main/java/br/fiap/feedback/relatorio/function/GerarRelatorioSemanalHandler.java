package br.fiap.feedback.relatorio.function;

import br.fiap.feedback.relatorio.model.RelatorioSemanal;
import br.fiap.feedback.relatorio.service.RelatorioSemanalService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Função Lambda {@code gerar-relatorio-semanal}.
 *
 * <p><b>Gatilho:</b> EventBridge Scheduler, toda segunda-feira às 08h em America/Sao_Paulo.<br>
 * <b>Responsabilidade:</b> disparar o caso de uso que consolida os sete dias anteriores e envia
 * o relatório ao administrador. Exceções são propagadas para o Scheduler aplicar retry/DLQ.
 */
@Named("relatorio")
@Dependent
public class GerarRelatorioSemanalHandler implements RequestHandler<Map<String, Object>, Void> {

    private static final Logger LOG = Logger.getLogger(GerarRelatorioSemanalHandler.class);

    private final RelatorioSemanalService service;

    @Inject
    public GerarRelatorioSemanalHandler(RelatorioSemanalService service) {
        this.service = service;
    }

    @Override
    public Void handleRequest(Map<String, Object> evento, Context contexto) {
        RelatorioSemanal relatorio = service.gerarEEnviar();
        LOG.infof("Execucao do relatorio concluida periodo=%s..%s total=%d",
                relatorio.inicio(), relatorio.fim(), relatorio.totalAvaliacoes());
        return null;
    }
}
