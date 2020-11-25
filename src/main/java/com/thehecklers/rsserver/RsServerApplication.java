package com.thehecklers.rsserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.time.Instant;

@SpringBootApplication
public class RsServerApplication {

    public static void main(String[] args) {
        Hooks.onErrorDropped(err ->
                System.out.println("Disconnecting client " + err
        .getLocalizedMessage()));
        SpringApplication.run(RsServerApplication.class, args);
    }

}

@Configuration
class ServerConfig {
    @Bean
    WebClient client() {
        return WebClient.create("http://localhost:7634/aircraft");
    }
}

@Controller
@AllArgsConstructor
class AircraftServerController {
    private final WebClient client;

    // Request-Response
    @MessageMapping("reqresp")
    Mono<Aircraft> reqResp(Mono<String> timestampMono) {
        return timestampMono.doOnNext(ts -> System.out.println("⏱ " + ts))
                .then(client.get()
                        .retrieve()
                        .bodyToFlux(Aircraft.class)
                        .next());
    }

    // Request-Stream
    @MessageMapping("reqstream")
    Flux<Aircraft> reqStream(Mono<String> timestampMono) {
        return timestampMono.doOnNext(ts -> System.out.println("⏱ " + ts))
                .thenMany(client.get()
                        .retrieve()
                        .bodyToFlux(Aircraft.class));
    }

    // Fire and Forget
    @MessageMapping("fireforget")
    Mono<Void> fireAndForget(Mono<Weather> weatherMono) {
        weatherMono.subscribe(w -> System.out.println("☀️ " + w));
        return Mono.empty();
    }

    // Bidirectional channel
    @MessageMapping("channel")
    Flux<Aircraft> channel(Flux<Weather> weatherFlux) {
        return weatherFlux
                .doOnSubscribe(w -> System.out.println("SUBSCRIBED TO WX"))
                .doOnNext(w -> System.out.println("🌬 " + w))
                .switchMap(w -> client.get()
                        .retrieve()
                        .bodyToFlux(Aircraft.class));
    }
}

@Data
@AllArgsConstructor
class Weather {
    private Instant when;
    private String observation;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Aircraft {
    private String callsign, reg, flightno, type;
    private int altitude, heading, speed;
    private double lat, lon;
}