package org.refactoringminer.astDiff.utils;

/* Created by pourya on 2022-12-30 11:35 a.m. */
public class URLHelper{
    public static String clean(String url)
    {
        String result;
        int index;
        index = nthIndexOf(url,'#',1);
        if (index == -1)
            result = url;
        else
            result =  url.substring(0,index);
        index = nthIndexOf(result,'?',1);
        if (index == -1)
            result = url;
        else
            result =  url.substring(0,index);
        return result;
    }
    public static String getRepo(String url) {
        url = removeAdditionalPart(url);
        int index = nthIndexOf(url,'/',5);
        return url.substring(0,index) + ".git";
    }

    public static String getCommit(String url) {
        url = removeAdditionalPart(url);
        int index = nthIndexOf(url,'/',6);
        return url.substring(index+1);
    }
    private static String removeAdditionalPart(String url){
        int index = url.lastIndexOf('#');
        if (index == -1)
            return url;
        else
            return url.substring(0,index);
    }
    public static int nthIndexOf(String text, char needle, int n)
    {
        for (int i = 0; i < text.length(); i++)
        {
            if (text.charAt(i) == needle)
            {
                n--;
                if (n == 0)
                {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int getPullRequestID(String prURL) {
        prURL = removeAdditionalPart(prURL);
        int index = nthIndexOf(prURL,'/',6);
        return Integer.parseInt(prURL.substring(index+1));
    }

    public static boolean isPR(String url) {
        return url.contains("/pull/");
    }
}