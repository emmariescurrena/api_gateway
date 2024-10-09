package com.emmariescurrena.bookesy.api_gateway.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleException(Exception exception) {
        ProblemDetail errorDetail = null;

        exception.printStackTrace();

        if (exception instanceof WebClientResponseException) {
            WebClientResponseException webClientEx = (WebClientResponseException) exception;
            errorDetail = ProblemDetail.forStatusAndDetail(webClientEx.getStatusCode(), webClientEx.getMessage());
            errorDetail.setProperty("description", "Error in external service call");
        } else if (exception instanceof WebClientRequestException) {
            WebClientRequestException webClientEx = (WebClientRequestException) exception;
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500), webClientEx.getMessage());
            errorDetail.setProperty("description", "Error in external service call");
        } else if (exception instanceof AccessDeniedException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), exception.getMessage());
            errorDetail.setProperty("description", "You are not authorized to access this resource");
        } else if (exception instanceof CallNotPermittedException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(503), "Service unavailable due to circuit breaker open");
            errorDetail.setProperty("description", "The service is temporarily unavailable, please try again later");
        } else {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500), exception.getMessage());
            errorDetail.setProperty("description", "Unknown internal server error.");
        }

        return Mono.just(ResponseEntity.status(errorDetail.getStatus()).body(errorDetail));
    }
}
