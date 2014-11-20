/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;


import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 *
 * @author hique
 */
@Entity
@NamedQueries({
     @NamedQuery(name = "refactoringGit.findRefactoringDuplicado", query = "SELECT i FROM RefactoringGit i where i.hashOperacao = :hashOperacao")
 })
public class RefactoringGit extends Entidade implements Serializable {

    
    private String revisionOrderV0;

    private String revisionOrderV1;

    private String tipoOperacao;

    @Column(length = 15000)
    private String operacaoCompleta;
   
    @Column(length = 700)
    private String hashOperacao;
    
    @ManyToOne
    @JoinColumn(name = "revisiongit_id")
    private RevisionGit revisiongit;

    
   @Override
   public Long getId() {
        return id;
    }

    @Override
    public void setId(final Long id) {
        this.id = id;
    }
    
    public String getRevisionOrderV0() {
        return revisionOrderV0;
    }

    public void setRevisionOrderV0(String revisionOrderV0) {
        this.revisionOrderV0 = revisionOrderV0;
    }

    public String getRevisionOrderV1() {
        return revisionOrderV1;
    }

    public void setRevisionOrderV1(String revisionOrderV1) {
        this.revisionOrderV1 = revisionOrderV1;
    }

    public String getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(String tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getOperacaoCompleta() {
        return operacaoCompleta;
    }

    public void setOperacaoCompleta(String operacaoCompleta) {
        this.operacaoCompleta = operacaoCompleta;
    }

    public RevisionGit getRevisiongit() {
        return revisiongit;
    }

    public void setRevisiongit(RevisionGit revisiongit) {
        this.revisiongit = revisiongit;
    }

    

    public String getHashOperacao() {
        return hashOperacao;
    }

    public void setHashOperacao(String hashOperacao) {
        this.hashOperacao = hashOperacao;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.revisionOrderV0);
        hash = 89 * hash + Objects.hashCode(this.revisionOrderV1);
        hash = 89 * hash + Objects.hashCode(this.tipoOperacao);
        hash = 89 * hash + Objects.hashCode(this.hashOperacao);
        hash = 89 * hash + Objects.hashCode(this.revisiongit);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RefactoringGit other = (RefactoringGit) obj;
        if (!Objects.equals(this.revisionOrderV0, other.revisionOrderV0)) {
            return false;
        }
        if (!Objects.equals(this.revisionOrderV1, other.revisionOrderV1)) {
            return false;
        }
        if (!Objects.equals(this.tipoOperacao, other.tipoOperacao)) {
            return false;
        }
        if (!Objects.equals(this.hashOperacao, other.hashOperacao)) {
            return false;
        }
        return true;
    }

}
