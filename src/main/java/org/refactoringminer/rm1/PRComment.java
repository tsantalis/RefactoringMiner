package org.refactoringminer.rm1;

public class PRComment {
    String author;
    String filePath;
    int line;
    String text;
    //public String time;
    //public Status status;

    public PRComment(String author, String filePath, int line, String text) {
        this.author = author;
        this.filePath = filePath;
        this.line = line;
        this.text = text;
    }

    public String getAuthor() {
        return author;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLine() {
        return line;
    }

    public String getText() {
        return text;
    }
}
