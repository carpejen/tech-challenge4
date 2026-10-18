package br.com.fiap.techchallenge4.notificacao.service;

import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class NotificacaoService {

    private final SnsClient snsClient;
    private final String adminTopicArn;

    public NotificacaoService(SnsClient snsClient, String adminTopicArn) {
        this.snsClient = snsClient;
        this.adminTopicArn = adminTopicArn;
    }

    public void notificarCritico(Avaliacao avaliacao) {
        String mensagem = """
                Foi recebido um feedback crítico.

                Descrição: %s
                Urgência: %s
                Data de envio: %s
                Nota: %d
                """.formatted(
                avaliacao.descricao(),
                avaliacao.urgencia(),
                avaliacao.dataEnvio(),
                avaliacao.nota()
        );

        publicar("Alerta de feedback crítico", mensagem);
    }

    public void enviarRelatorio(String relatorio) {
        publicar("Relatório semanal de feedbacks", relatorio);
    }

    private void publicar(String assunto, String mensagem) {
        snsClient.publish(PublishRequest.builder()
                .topicArn(adminTopicArn)
                .subject(assunto)
                .message(mensagem)
                .build());
    }
}
