package br.fiap.feedback.relatorio.repository;

import br.fiap.feedback.domain.Avaliacao;

import java.time.LocalDate;
import java.util.List;

/** Porta de leitura das avaliações de um dia para geração do relatório. */
public interface AvaliacaoConsultaRepository {

    List<Avaliacao> buscarPorDia(LocalDate dia);
}
