package com.foody.reservations.service;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ReservationTimeConfig {

    @Bean
    Clock reservationClock(@Value("${foody.reservations.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
