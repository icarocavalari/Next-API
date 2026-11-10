package io.github.icarocavalari.nextapi.exception;

import io.github.icarocavalari.nextapi.auth.EmailAlreadyExistsException;
import io.github.icarocavalari.nextapi.auth.NicknameAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        return detail;
    }

    @ExceptionHandler
    public ProblemDetail handleNicknameAlreadyExists(NicknameAlreadyExistsException e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        return detail;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError error: exception.getBindingResult().getFieldErrors()) {
            if (error.getDefaultMessage() != null) {
                errors.putIfAbsent(error.getField(), error.getDefaultMessage());
            }
        }

        ProblemDetail body = exception.getBody();
        body.setProperty("errors", errors);

        return handleExceptionInternal(exception, body, headers, status, request);
    }

    @ExceptionHandler
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e) {
        if (e.getMessage().contains("uk_users_email")) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email is already being used");
        }
        log.error("Unexpected error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    @ExceptionHandler
    public ProblemDetail handleAuthentication(AuthenticationException e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Email/Password is incorrect.");
        return detail;
    }

    @ExceptionHandler
    public ProblemDetail handleException(Exception e) {
        log.error("Unexpected error", e);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        return detail;
    }
}
