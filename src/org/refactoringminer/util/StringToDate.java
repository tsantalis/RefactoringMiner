/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.refactoringminer.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 
 * @author hique
 */
public class StringToDate {

	public static Date parseDateEEE(String stringDate) throws ParseException {
		SimpleDateFormat formatoTexto = new SimpleDateFormat(
				"EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
		Date d = (Date) formatoTexto.parse(stringDate);
		return d;
	}

	
	public static Date parseDatePatterns(String stringDate) {
		Date pubdate = null;
		
		String[] patterns = {//"EEE, dd MMM yyyy hh:mm:ss UTC",
				"yyyy-MM-dd'T'HH:mm:ss",
	            "yyyy.MM.dd G 'at' HH:mm:ss z",
	            "EEE, MMM d, ''yy",
	            "yyyyy.MMMMM.dd GGG hh:mm aaa",
	            "EEE, d MMM yyyy HH:mm:ss Z",
	            "yyMMddHHmmssZ",
	            "d MMM yyyy HH:mm:ss z",
	            "yyyy-MM-dd'T'HH:mm:ss'Z'",
	            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
	            "yyyy-MM-dd'T'HH:mm:ssZ",
	            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
	            "yyyy-MM-dd'T'HH:mm:ssz",
	            "yyyy-MM-dd'T'HH:mm:ss.SSSz",
	            "EEE, d MMM yy HH:mm:ssz",
	            "EEE, d MMM yy HH:mm:ss",
	            "EEE, d MMM yy HH:mm z",
	            "EEE, d MMM yy HH:mm Z",
	            "EEE, d MMM yyyy HH:mm:ss z",
	            "EEE, d MMM yyyy HH:mm:ss Z",
	            "EEE, d MMM yyyy HH:mm:ss ZZZZ",
	            "EEE, d MMM yyyy HH:mm z",
	            "EEE, d MMM yyyy HH:mm Z",
	            "d MMM yy HH:mm z",
	            "d MMM yy HH:mm:ss z",
	            "d MMM yyyy HH:mm z",
	            "d MMM yyyy HH:mm:ss z"};

	    for (int i = 0; i < patterns.length; i++) {
	        SimpleDateFormat sdf = new SimpleDateFormat(patterns[i], Locale.ENGLISH);
	        try {
	            pubdate = sdf.parse(stringDate);
	            break;
	        } catch (Exception e) {
	        }
	    }
		return pubdate;
	}
	
	public static void main(String [] args) throws ParseException{
		

		String value = "2010-02-08T13:20:56Z";

		

	    String[] patterns = {//"EEE, dd MMM yyyy hh:mm:ss UTC",
	            "yyyy.MM.dd G 'at' HH:mm:ss z",
	            "EEE, MMM d, ''yy",
	            "yyyyy.MMMMM.dd GGG hh:mm aaa",
	            "EEE, d MMM yyyy HH:mm:ss Z",
	            "yyMMddHHmmssZ",
	            "d MMM yyyy HH:mm:ss z",
	            "yyyy-MM-dd'T'HH:mm:ss",
	            "yyyy-MM-dd'T'HH:mm:ss'Z'",
	            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
	            "yyyy-MM-dd'T'HH:mm:ssZ",
	            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
	            "yyyy-MM-dd'T'HH:mm:ssz",
	            "yyyy-MM-dd'T'HH:mm:ss.SSSz",
	            "EEE, d MMM yy HH:mm:ssz",
	            "EEE, d MMM yy HH:mm:ss",
	            "EEE, d MMM yy HH:mm z",
	            "EEE, d MMM yy HH:mm Z",
	            "EEE, d MMM yyyy HH:mm:ss z",
	            "EEE, d MMM yyyy HH:mm:ss Z",
	            "EEE, d MMM yyyy HH:mm:ss ZZZZ",
	            "EEE, d MMM yyyy HH:mm z",
	            "EEE, d MMM yyyy HH:mm Z",
	            "d MMM yy HH:mm z",
	            "d MMM yy HH:mm:ss z",
	            "d MMM yyyy HH:mm z",
	            "d MMM yyyy HH:mm:ss z"};

	    for (int i = 0; i < patterns.length; i++) {
	        SimpleDateFormat sdf = new SimpleDateFormat(patterns[i], Locale.ENGLISH);
	        try {
	            Date pubdate = sdf.parse(value);
	            System.out.println(pubdate);

	            break;
	        } catch (Exception e) {
	        	 System.out.println(e);
	            
	            
	        }
	    }
	}

}
