package br.fiap.feedback.relatorio.email;

/** Porta de saída para envio do relatório consolidado. */
public interface EnviadorRelatorio {

    void enviar(String destinatario, String assunto, String corpo);
}
