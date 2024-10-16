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
import org.springframework.web.reactive.function.client.WebClientRequestException;
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
        HttpStatusCode httpStatus = determineHttpStatus(throwable);

        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException webClientException = (WebClientResponseException) throwable;
            errorPropertiesMap.put("message", webClientException.getResponseBodyAsString());
            errorPropertiesMap.put("details", webClientException.getResponseBodyAsString());
        } else if (throwable instanceof AccessDeniedException) {
            errorPropertiesMap.put("message", "You do not have permission to access this resource.");
        } else {
            errorPropertiesMap.put("message", throwable.getMessage());
        }

        errorPropertiesMap.put("status", httpStatus.value());
        errorPropertiesMap.remove("error");

        return ServerResponse.status(httpStatus)
               .contentType(MediaType.APPLICATION_JSON)
               .body(BodyInserters.fromValue(errorPropertiesMap));
    }

    private HttpStatusCode determineHttpStatus(Throwable throwable) {

        if (throwable instanceof WebClientResponseException) {
            return ((WebClientResponseException) throwable).getStatusCode();
        } else if (throwable instanceof WebClientRequestException) {
            if (throwable.getCause() instanceof java.net.SocketTimeoutException) {
                return HttpStatus.GATEWAY_TIMEOUT;
            } else if (throwable.getCause() instanceof java.net.UnknownHostException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            } else if (throwable.getCause() instanceof javax.net.ssl.SSLException) {
                return HttpStatus.BAD_GATEWAY;
            } else {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
        } else if (throwable instanceof ResponseStatusException) {
            return ((ResponseStatusException) throwable).getStatusCode();
        } else if (throwable.getCause() instanceof ClientAuthorizationRequiredException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (throwable instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (throwable instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        } else {
            // Fallback for unknown exceptions
            log.error("Unknown error occurred: ", throwable); // Log the exception details
            return HttpStatus.INTERNAL_SERVER_ERROR;  // Keep the 500 status
        }

    }

}
