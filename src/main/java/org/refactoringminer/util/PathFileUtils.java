package org.refactoringminer.util;

import gr.uom.java.xmi.Constants;

public class PathFileUtils {
    public static boolean isSupportedFile(String path){
        return isJavaFile(path) || isPythonFile(path) || isKotlinFile(path) || isTypeScriptFile(path);
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

    public static boolean isTypeScriptFile(String path){
        return path.endsWith(".ts") || path.endsWith(".tsx");
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
        else if (isTypeScriptFile(path))
            return Constants.TYPESCRIPT;
        return Constants.JAVA;
    }
}
