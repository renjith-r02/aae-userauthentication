package com.authservice.exception;

import com.authservice.dto.FieldError;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import java.util.List;

@Getter
public class ValidationException extends ServiceException {
    private final List<FieldError> fieldErrors;

    public ValidationException(List<FieldError> fieldErrors) {
        super("Validation failed", "VALID_001", HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors;
    }
}
