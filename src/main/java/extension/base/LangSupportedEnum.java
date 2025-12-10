package extension.base;

import java.util.Arrays;

/**
 * Enum representing supported programming languages.
 */
public enum LangSupportedEnum {

    PYTHON("py", "python"),
    CSHARP("cs", "csharp");

    private final String fileExtension;
    private final String langName;

    LangSupportedEnum(String fileExtension, String langName) {
        this.fileExtension = fileExtension;
        this.langName = langName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getLangName() {
        return langName;
    }

    public static LangSupportedEnum fromFileExtension(String extension) {
        String cleanExt = extension.startsWith(".") ? extension.substring(1) : extension;
        return Arrays.stream(values())
                .filter(lang -> lang.fileExtension.equals(cleanExt))
                .findFirst()
                .orElse(null);
    }


    public static LangSupportedEnum fromLangName(String langName) {
        return Arrays.stream(values())
                .filter(lang -> lang.langName.equals(langName))
                .findFirst()
                .orElse(null);
    }


    public static LangSupportedEnum fromFileName(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        return fromFileExtension(extension);
    }


}
