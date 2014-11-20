/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufmg.dcc.labsoft.refactoringanalyzer.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author hique
 */
public class FormatDate {

    private static SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",Locale.ENGLISH);
    
    public static Date StringToDate(String stringDate) throws ParseException {
        return (Date) df.parse(stringDate);
    }
    
    public static Date formatToDate(Date date) throws ParseException {
        return (Date)df.parse(df.format(date));
    }

}
