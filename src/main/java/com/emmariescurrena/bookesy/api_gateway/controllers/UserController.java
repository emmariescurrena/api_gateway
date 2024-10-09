package com.emmariescurrena.bookesy.api_gateway.controllers;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
    public Mono<ResponseEntity<?>> createUser(@RequestBody CreateUserDto userDto) {
        return circuitBreaker.run(
                webClientBuilder.build()
                    .post()
                    .uri("http://user-service/users")
                    .bodyValue(userDto)
                    .retrieve()
                    .bodyToMono(User.class)
                    .map(ResponseEntity::ok),
                throwable -> {
                    log.error("Error during request to User Service", throwable);
                    if (throwable instanceof TimeoutException) {
                        return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Timeout occurred while contacting User Service"));
                    }
                    if (throwable instanceof WebClientResponseException e) {
                        // Forward error status code from User Service
                        return Mono.just(ResponseEntity.status(e.getStatusCode()).body("Error: " + e.getMessage()));
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unknown error"));
                });
    }
    

    @GetMapping("/byId/{id}")
    public Mono<ResponseEntity<User>> getUserById(@PathVariable Long id) {
        return webClientBuilder.build()
            .get()
            .uri("http://user-service/users/byId/{id}", id)
            .retrieve()
            .toEntity(User.class)
            .map(response -> {
                HttpHeaders headers = new HttpHeaders();
                headers.addAll(response.getHeaders());
                return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
            });
    }

    @GetMapping("/byEmail/{email}")
    public Mono<ResponseEntity<User>> getUserByEmail(@PathVariable String email) {
        return webClientBuilder.build()
            .get()
            .uri("http://user-service/users/byEmail/{email}", email)
            .retrieve()
            .toEntity(User.class)
            .map(response -> {
                HttpHeaders headers = new HttpHeaders();
                headers.addAll(response.getHeaders());
                return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
            });
    }

}
