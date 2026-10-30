package br.fiap.feedback.receber.service;

import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.receber.messaging.PublicadorAvaliacaoCritica;
import br.fiap.feedback.receber.repository.AvaliacaoRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso "registrar avaliação": valida, classifica a urgência, grava e,
 * se for crítica, encaminha para a fila de notificação.
 */
@Singleton
public class AvaliacaoService {

    /** Marcador procurado pelo filtro de métrica/alarme no CloudWatch. */
    public static final String MARCADOR_FALHA_PUBLICACAO = "FALHA_PUBLICACAO_CRITICA";

    private static final Logger LOG = Logger.getLogger(AvaliacaoService.class);

    private final AvaliacaoRepository repository;
    private final PublicadorAvaliacaoCritica publicador;
    private final Clock relogio;

    @Inject
    public AvaliacaoService(AvaliacaoRepository repository,
                            PublicadorAvaliacaoCritica publicador,
                            Clock relogio) {
        this.repository = repository;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    /**
     * Registra uma avaliação nova.
     *
     * @return a avaliação gravada, com id, urgência e data de envio
     * @throws br.fiap.feedback.domain.AvaliacaoInvalidaException se os dados forem inválidos
     */
    public Avaliacao registrar(String descricao, Integer nota) {
        Avaliacao avaliacao = Avaliacao.nova(UUID.randomUUID().toString(), descricao, nota, Instant.now(relogio));

        repository.salvar(avaliacao);
        // Não logamos a descrição: é texto livre do aluno e pode conter dados pessoais.
        LOG.infof("Avaliacao registrada id=%s nota=%d urgencia=%s",
                avaliacao.id(), avaliacao.nota(), avaliacao.urgencia());

        if (avaliacao.urgencia().exigeNotificacaoImediata()) {
            encaminharParaNotificacao(avaliacao);
        }
        return avaliacao;
    }

    /**
     * A avaliação já está gravada quando chegamos aqui. Se a fila falhar (o SDK já tenta
     * 3 vezes), não devolvemos erro ao aluno: ele reenviaria e criaria uma avaliação duplicada.
     * Em vez disso, registramos um log com {@value #MARCADOR_FALHA_PUBLICACAO}, que vira
     * alarme no CloudWatch para os administradores.
     */
    private void encaminharParaNotificacao(Avaliacao avaliacao) {
        try {
            publicador.publicar(avaliacao);
            LOG.infof("Avaliacao critica enviada para a fila id=%s", avaliacao.id());
        } catch (RuntimeException e) {
            LOG.errorf(e, "%s id=%s", MARCADOR_FALHA_PUBLICACAO, avaliacao.id());
        }
    }
}
