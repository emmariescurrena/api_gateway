package com.emmariescurrena.bookesy.api_gateway.exceptions;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@Order(Integer.MIN_VALUE)
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public GlobalExceptionHandler(
        final ErrorAttributes errorAttributes,
        final WebProperties.Resources resources,
        final ApplicationContext applicationContext,
        final ServerCodecConfigurer configurer
    ) {
        super(errorAttributes, resources, applicationContext);
        setMessageReaders(configurer.getReaders());
        setMessageWriters(configurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        ErrorAttributeOptions options = ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE);
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, options);
        Throwable throwable = getError(request);

        HttpStatusCode statusCode = determineHttpStatus(throwable);
        errorPropertiesMap.put("status", statusCode.value());
        errorPropertiesMap.put("message", throwable.getMessage());
    
        return ServerResponse.status((HttpStatusCode) errorPropertiesMap.get("status"))
              .contentType(MediaType.APPLICATION_JSON)
              .body(BodyInserters.fromValue(errorPropertiesMap));
    }

    private HttpStatusCode determineHttpStatus(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            return ((WebClientResponseException) throwable).getStatusCode();
        } else if (throwable instanceof ResponseStatusException) {
            return ((ResponseStatusException) throwable).getStatusCode();
        } else if (throwable instanceof ClientAuthorizationRequiredException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (throwable instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (throwable instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        } else {
            log.error("Unknown error occurred: ", throwable);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}

