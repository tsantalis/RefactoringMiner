package br.ufmg.dcc.labsoft.refdetector.exception;

public class DuplicateEntityException extends RuntimeException {

    public DuplicateEntityException(String entity) {
        super(entity);
    }
    
}
