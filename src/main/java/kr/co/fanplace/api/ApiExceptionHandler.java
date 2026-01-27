package kr.co.fanplace.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler
{
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e)
    { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> serverError(Exception e)
    { return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류가 발생했습니다.")); }
}