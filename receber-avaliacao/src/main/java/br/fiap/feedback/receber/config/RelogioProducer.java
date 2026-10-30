package br.fiap.feedback.receber.config;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.time.Clock;

/** Disponibiliza o relógio do sistema para injeção (nos testes usamos um relógio fixo). */
@Singleton
public class RelogioProducer {

    @Produces
    @Singleton
    Clock relogio() {
        return Clock.systemUTC();
    }
}
