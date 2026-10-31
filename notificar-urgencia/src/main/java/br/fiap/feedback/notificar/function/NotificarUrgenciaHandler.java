package br.fiap.feedback.notificar.function;

import br.fiap.feedback.contrato.MensagemAvaliacaoCritica;
import br.fiap.feedback.notificar.service.NotificacaoUrgenciaService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

/**
 * Função Lambda {@code notificar-urgencia}.
 *
 * <p><b>Gatilho:</b> fila SQS {@code avaliacoes-criticas} (batch de 1 mensagem).<br>
 * <b>Responsabilidade:</b> traduzir a mensagem da fila numa chamada ao
 * {@link NotificacaoUrgenciaService}. Qualquer exceção é propagada de propósito: a mensagem
 * volta para a fila e, depois de 3 tentativas, vai para a DLQ.
 */
@Named("notificar")
@Dependent
public class NotificarUrgenciaHandler implements RequestHandler<SQSEvent, Void> {

    private static final Logger LOG = Logger.getLogger(NotificarUrgenciaHandler.class);

    private final NotificacaoUrgenciaService service;
    private final ObjectMapper json;

    @Inject
    public NotificarUrgenciaHandler(NotificacaoUrgenciaService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @Override
    public Void handleRequest(SQSEvent evento, Context contexto) {
        for (SQSEvent.SQSMessage registro : evento.getRecords()) {
            MensagemAvaliacaoCritica mensagem = lerMensagem(registro.getBody());
            service.notificar(mensagem);
            // Sem a descrição no log (texto livre do aluno)
            LOG.infof("Notificacao de urgencia enviada: id=%s nota=%d urgencia=%s",
                    mensagem.id(), mensagem.nota(), mensagem.urgencia());
        }
        return null;
    }

    MensagemAvaliacaoCritica lerMensagem(String corpo) {
        try {
            return json.readValue(corpo, MensagemAvaliacaoCritica.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Mensagem inválida na fila de avaliações críticas", e);
        }
    }
}
