package org.refactoringminer.rm2.exception;

public class DuplicateEntityException extends RuntimeException {

    public DuplicateEntityException(String entity) {
        super(entity);
    }
    
}
