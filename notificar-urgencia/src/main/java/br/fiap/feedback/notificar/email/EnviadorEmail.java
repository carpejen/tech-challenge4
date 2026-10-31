package br.fiap.feedback.notificar.email;

public interface EnviadorEmail {

    void enviar(String destinatario,String assunto, String corpo);
}
