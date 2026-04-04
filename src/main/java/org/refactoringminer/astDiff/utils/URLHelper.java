package org.refactoringminer.astDiff.utils;

/* Created by pourya on 2022-12-30 11:35 a.m. */
public class URLHelper{
    public static String clean(String url)
    {
        return removeAdditionalPart(url);
    }
    public static String getRepo(String url) {
        url = removeAdditionalPart(url);
        int index = nthIndexOf(url,'/',5);
        return url.substring(0,index) + ".git";
    }

    public static String getCommit(String url) {
        url = removeAdditionalPart(url);
        if (url.contains("/pull/") && url.contains("/commits/")) {
        	int index = nthIndexOf(url,'/',8);
        	return url.substring(index+1);
        }
        int index = nthIndexOf(url,'/',6);
        return url.substring(index+1);
    }
    private static String removeAdditionalPart(String url){
        int fragmentIndex = url.indexOf('#');
        int queryIndex = url.indexOf('?');
        int endIndex = -1;
        if (fragmentIndex != -1 && queryIndex != -1) {
            endIndex = Math.min(fragmentIndex, queryIndex);
        }
        else if (fragmentIndex != -1) {
            endIndex = fragmentIndex;
        }
        else if (queryIndex != -1) {
            endIndex = queryIndex;
        }
        if (endIndex == -1) {
            return url;
        }
        return url.substring(0, endIndex);
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
		if (hasPullRequestContext(prURL)) {
			return Integer.parseInt(getPRID(prURL));
		}
		int index = nthIndexOf(prURL,'/',6);
		String prID = prURL.substring(index+1);
		if(prID.endsWith("/files")) {
			prID = prID.substring(0, prID.length() - "/files".length());
		}
		return Integer.parseInt(prID);
	}

	public static boolean hasPullRequestContext(String url) {
		return removeAdditionalPart(url).contains("/pull/");
	}

	public static boolean isPR(String url) {
		url = removeAdditionalPart(url);
		return url.contains("/pull/") && !url.contains("/commits/");
	}

	public static String getPRID(String url) {
		url = removeAdditionalPart(url);
		int start = nthIndexOf(url,'/',6);
		int end = nthIndexOf(url,'/',7);
		if (end == -1) end = url.length();
		return url.substring(start+1, end);
	}

	public static String shortenCommit(String commit) {
        if (commit.length() > 7) {
            return commit.substring(0, 7);
        }
        return commit;
    }

	public static String getRepoStringOnly(String url) {
        int start = nthIndexOf(url,'/',4);
        int end = nthIndexOf(url,'/',5);
        return url.substring(start+1, end);
    }

}
