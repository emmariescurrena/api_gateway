package com.emmariescurrena.bookesy.api_gateway.controllers;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/users")
public class UserController {

    Logger log = LoggerFactory.getLogger(UserController.class);

    private final WebClient.Builder webClientBuilder;
    private final ReactiveCircuitBreaker circuitBreaker;

    public UserController(WebClient.Builder webClientBuilder,
    ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory) {
        this.webClientBuilder = webClientBuilder;
        this.circuitBreaker = circuitBreakerFactory.create("circuit-breaker");
    }

    record CreateUserDto(String auth0UserId, String email, String name, String surname, String bio) {
    }

    record User(Long id,String auth0UserId, String email, String name, String surname, String bio, Date creationDate, String role) {
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createUser(@RequestBody CreateUserDto userDto) {
        return circuitBreaker.run(
            webClientBuilder.build()
                .post()
                .uri("http://user-service/users")
                .bodyValue(userDto)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(User.class)
                            .map(user -> ResponseEntity.status(clientResponse.statusCode()).body(user));
                    } else {
                        return clientResponse.bodyToMono(String.class)
                            .map(body -> ResponseEntity.status(clientResponse.statusCode()).body(body));
                    }
                })
        );
    }
    

    @GetMapping("/byId/{id}")
    public Mono<ResponseEntity<?>> getUserById(@PathVariable Long id) {
        return circuitBreaker.run(
            webClientBuilder.build()
            .get()
            .uri("http://user-service/users/byId/{id}", id)
            .exchangeToMono(clientResponse -> {
                if (clientResponse.statusCode().is2xxSuccessful()) {
                    return clientResponse.bodyToMono(User.class)
                        .map(user -> ResponseEntity.status(clientResponse.statusCode()).body(user));
                } else {
                    return clientResponse.bodyToMono(String.class)
                        .map(body -> ResponseEntity.status(clientResponse.statusCode()).body(body));
                }
            })
        );
    }

    @GetMapping("/byEmail/{email}")
    public Mono<ResponseEntity<?>> getUserByEmail(@PathVariable String email) {
        return circuitBreaker.run(
            webClientBuilder.build()
            .get()
            .uri("http://user-service/users/byEmail/{email}", email)
            .exchangeToMono(clientResponse -> {
                if (clientResponse.statusCode().is2xxSuccessful()) {
                    return clientResponse.bodyToMono(User.class)
                        .map(user -> ResponseEntity.status(clientResponse.statusCode()).body(user));
                } else {
                    return clientResponse.bodyToMono(String.class)
                        .map(body -> ResponseEntity.status(clientResponse.statusCode()).body(body));
                }
            })
        );
    }

}
