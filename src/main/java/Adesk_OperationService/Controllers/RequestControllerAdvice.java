package Adesk_OperationService.Controllers;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@ConditionalOnProperty(
        name = "springdoc.api-docs.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class RequestControllerAdvice {

    @ExceptionHandler(Exception.class)
    @Hidden
    public ResponseEntity<String> badRequest(Exception e){
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
