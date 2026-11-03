package br.fiap.feedback.relatorio.email;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

/** Envia o relatório semanal usando Amazon SES. */
@Singleton
public class SesEnviadorRelatorio implements EnviadorRelatorio {

    private static final String UTF8 = "UTF-8";

    private final SesClient ses;
    private final String remetente;

    @Inject
    public SesEnviadorRelatorio(SesClient ses,
                                @ConfigProperty(name = "app.email-remetente") String remetente) {
        this.ses = ses;
        this.remetente = remetente;
    }

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        ses.sendEmail(SendEmailRequest.builder()
                .source(remetente)
                .destination(d -> d.toAddresses(destinatario))
                .message(m -> m
                        .subject(c -> c.data(assunto).charset(UTF8))
                        .body(b -> b.text(c -> c.data(corpo).charset(UTF8))))
                .build());
    }
}
