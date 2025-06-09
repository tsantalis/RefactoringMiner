package gui.webdiff.dir;

import java.util.Date;

public class PullRequestReviewComment {
    private String author;
	private String text;
	private String createdAt; //Keep it String for easier serialization
    private int line;
    private String avatarUrl;
    //

	public PullRequestReviewComment(String author, String text, Date date, int line, String avatarUrl) {
		this.author = author;
		this.text = text;
		this.createdAt = String.valueOf(date);
        this.line = line;
        this.avatarUrl = avatarUrl;
	}
	@Override
	public String toString() {
		return author + " @" + createdAt + ": " + text;
	}


    public int getLine() {
        return line;
    }

    public String getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
