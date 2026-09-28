package learning.permission;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.*;
import static learning.permission.ApiTypes.*;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorView> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ErrorView(e.getReason()));
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorView> denied() { return ResponseEntity.status(403).body(new ErrorView("没有所需操作权限")); }
    @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class,
        HandlerMethodValidationException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorView> invalid() { return ResponseEntity.badRequest().body(new ErrorView("参数格式或取值不合法")); }
    @ExceptionHandler({DataIntegrityViolationException.class,OptimisticLockingFailureException.class})
    ResponseEntity<ErrorView> conflict() { return ResponseEntity.status(409).body(new ErrorView("数据冲突，请刷新后重试")); }
}
