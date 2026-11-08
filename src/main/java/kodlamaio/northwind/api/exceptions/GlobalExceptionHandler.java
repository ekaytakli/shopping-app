package kodlamaio.northwind.api.exceptions;

import jakarta.validation.ConstraintViolationException;
import kodlamaio.northwind.core.exceptions.*;
import kodlamaio.northwind.core.utilities.results.ErrorDataResult;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private ResponseEntity<Object> error(HttpStatusCode status, Object details, String message) {
        return ResponseEntity.status(status).body(new ErrorDataResult<>(details, message));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
        HttpHeaders headers, HttpStatusCode status, org.springframework.web.context.request.WebRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
            errors.computeIfAbsent(e.getField(), key -> new ArrayList<>()).add(e.getDefaultMessage()));
        return error(status, errors, "Doğrulama hataları");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
        HttpHeaders headers, HttpStatusCode status, org.springframework.web.context.request.WebRequest request) {
        return new ResponseEntity<>(new ErrorDataResult<>(null, "İstek işlenemedi"), headers, status);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> notFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, null, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> conflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, null, ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<Object> badRequest(RuntimeException ex) {
        // Constraint messages may contain property paths, but never rejected values.
        return error(HttpStatus.BAD_REQUEST, null,
            ex instanceof ConstraintViolationException ? "Doğrulama hataları" : ex.getMessage());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, IncorrectResultSizeDataAccessException.class})
    public ResponseEntity<Object> dataConflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, null, "Kayıt mevcut verilerle çakışıyor");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception ex) {
        logger.error("Beklenmeyen sunucu hatası", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, null, "Beklenmeyen bir hata oluştu");
    }
}
