/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.ufmg.dcc.labsoft.refactoringanalyzer.util;

import java.io.File;

/**
 *
 * @author hique
 */
public class DeleteDiretorios {
   
    public void remover (File f) {  
        if (f.isDirectory()) {  
            File[] files = f.listFiles();  
            for (int i = 0; i < files.length; ++i) {  
                remover (files[i]);  
            }  
        }  
        f.delete();  
        
    }
    
    public boolean deleteDir(File dir) {  
    if (dir.isDirectory()) {  
        String[] children = dir.list();  
        for (int i=0; i<children.length; i++) {  
            boolean success = deleteDir(new File(dir, children[i]));  
            if (!success) {  
                return false;  
            }  
        }  
    }  
  
    return dir.delete();  
}  
}
