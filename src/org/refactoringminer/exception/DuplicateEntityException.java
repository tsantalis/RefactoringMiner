package org.refactoringminer.exception;

public class DuplicateEntityException extends RuntimeException {

    public DuplicateEntityException(String entity) {
        super(entity);
    }
    
}
