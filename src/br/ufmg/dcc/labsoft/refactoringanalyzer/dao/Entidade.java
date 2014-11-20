/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;


import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 *
 * @author hique
 */
@MappedSuperclass
public abstract class Entidade {
    @Transient
    private static final long serialVersionUID = -3038903536445432584L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    
    
    public Long getId() {
        id = null;
        return id;
    }

    public void setId(final Long id) {
        this.id = null;
    }

}
