package br.fiap.feedback.notificar.email;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Singleton
public class SesEnviadorEmail implements EnviadorEmail {

    private static final String UTF8 = "UTF-8";

    private final SesClient ses;
    private final String remetente;

    @Inject
    public SesEnviadorEmail(SesClient ses, @ConfigProperty(name = "app.email-remetente") String remetente) {
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
