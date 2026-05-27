package com.authservice.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class ResourceNotFoundException extends ServiceException {
    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + " not found with id: " + id, "NOT_FOUND_001", HttpStatus.NOT_FOUND);
    }
}
