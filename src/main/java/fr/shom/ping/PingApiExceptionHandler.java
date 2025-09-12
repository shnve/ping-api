package fr.shom.ping;

import fr.shom.ping.exception.S124Exception;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_XML;

@ControllerAdvice
public class PingApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(S124Exception.class)
    protected ResponseEntity<Object> handleS124Exception(S124Exception exception) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_XML);
        return new ResponseEntity<>(exception, headers, BAD_REQUEST);
    }

    @ExceptionHandler({ RequestNotPermitted.class })
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public void handleRequestNotPermitted() {
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, @NotNull HttpHeaders headers, @NotNull HttpStatusCode status,
        @NotNull WebRequest request) {
        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof DateTimeParseException) {
            Map<String, Object> responseBody = buildResponseBody("validation.date-time.invalid-format", status, request);
            return handleExceptionInternal(ex, responseBody, headers, BAD_REQUEST, request);
        }
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    private Map<String, Object> buildResponseBody(String message, HttpStatusCode status, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", status.value());
        body.put("error", status);
        body.put("message", message);
        if (request instanceof ServletWebRequest servletWebRequest) {
            body.put("path", servletWebRequest.getRequest().getServletPath());
        }
        return body;
    }

    @SneakyThrows
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status,
        WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(fieldError -> {
            String fieldName = ((FieldError) fieldError).getField();
            String errorMessage = fieldError.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", status.value());
        body.put("error", status);
        body.put("invalid-arguments", errors);
        if (request instanceof ServletWebRequest servletWebRequest) {
            body.put("path", servletWebRequest.getRequest().getServletPath());
        }
        return handleExceptionInternal(ex, body, headers, BAD_REQUEST, request);
    }
}
