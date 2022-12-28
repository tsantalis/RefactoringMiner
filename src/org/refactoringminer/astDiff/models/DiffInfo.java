package org.refactoringminer.astDiff.models;


import com.github.gumtreediff.utils.Pair;

public class DiffInfo extends Pair<String,String> {

    public DiffInfo(String a, String b) {
        super(a, b);
    }
    public String getInfo()
    {
        if (first.equals(second))
            return first;
        else
            return first + " --> " + second;
    }
}
