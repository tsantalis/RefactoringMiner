package org.refactoringminer.util;

import gr.uom.java.xmi.Constants;

public class PathFileUtils {
    public static boolean isSupportedFile(String path){
        return path.endsWith(".java") || path.endsWith(".py") || path.endsWith(".kt");
    }

    public static boolean isJavaFile(String path){
        return path.endsWith(".java");
    }

    public static boolean isPythonFile(String path){
        return path.endsWith(".py");
    }

    public static boolean isKotlinFile(String path){
        return path.endsWith(".kt");
    }

    public static boolean isLangSupportedFile(String path){
        // Add new languages in the future
        return isPythonFile(path);
    }

    public static String filePathWithoutExtension(String path) {
        if(path.contains("."))
            return path.substring(0, path.lastIndexOf("."));
        return path;
    }

    public static Constants getLang(String path) {
        if (isJavaFile(path))
            return Constants.JAVA;
        else if (isPythonFile(path))
            return Constants.PYTHON;
        else if (isKotlinFile(path))
            return Constants.KOTLIN;
        return Constants.JAVA;
    }
}
