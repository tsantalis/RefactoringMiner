/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.ufmg.dcc.labsoft.refactoringanalyzer.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author hique
 */
public class GenerateHash {
    public static String StringHashOperacaoCompleta(String stringOperacaoCompleta) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(stringOperacaoCompleta.getBytes());
        byte[] hashMd5 = md.digest();

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < hashMd5.length; i++) {
            int parteAlta = ((hashMd5[i] >> 4) & 0xf) << 4;
            int parteBaixa = hashMd5[i] & 0xf;
            if (parteAlta == 0) {
                s.append('0');
            }
            s.append(Integer.toHexString(parteAlta | parteBaixa));
        }
        return s.toString();
    }

}
