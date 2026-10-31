package br.fiap.feedback.notificar.service;

import br.fiap.feedback.contrato.MensagemAvaliacaoCritica;
import br.fiap.feedback.notificar.email.EnviadorEmail;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class NotificacaoUrgenciaService {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EnviadorEmail enviador;
    private final String emailAdmin;

    @Inject
    public NotificacaoUrgenciaService(EnviadorEmail enviador,
                                      @ConfigProperty(name = "app.email-admin") String emailAdmin) {
        this.enviador = enviador;
        this.emailAdmin = emailAdmin;
    }

    public void notificar(MensagemAvaliacaoCritica mensagem) {
        enviador.enviar(emailAdmin, montarAssunto(mensagem), montarCorpo(mensagem));
    }

    String montarAssunto(MensagemAvaliacaoCritica mensagem) {
        return "[Feedback] Avaliação " + mensagem.urgencia() + " recebida (nota " + mensagem.nota() + ")";
    }

    String montarCorpo(MensagemAvaliacaoCritica mensagem) {
        return """
                Uma avaliação crítica foi recebida e precisa de atenção.

                Descrição: %s
                Nota: %d
                Urgência: %s
                Data de envio: %s (horário de Brasília)
                ID: %s
                """.formatted(
                mensagem.descricao(),
                mensagem.nota(),
                mensagem.urgencia(),
                Instant.parse(mensagem.dataEnvio()).atZone(BRASILIA).format(FORMATO),
                mensagem.id());
    }
}
