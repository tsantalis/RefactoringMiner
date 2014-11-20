/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmg.dcc.labsoft.refactoringanalyzer.util;

import java.io.IOException;
import java.net.*;

/**
 *
 * @author hique
 */
public class InternetReachable {

    public boolean isInternetReachable() {
        try {
            // URL do destino escolhido
            URL url = new URL("https://github.com");

            // abre a conexão
            HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();

               // tenta buscar conteúdo da URL
            // se não tiver conexão, essa linha irá falhar
            Object objData = urlConnect.getContent();

        } catch (IOException ex) {
            System.out.println("ERRO SEM CONEX�O COM A INTERNET");
            return false;
        }
        return true;

    }
}
