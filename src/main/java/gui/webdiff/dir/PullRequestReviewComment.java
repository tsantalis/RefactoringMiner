package gui.webdiff.dir;

import java.util.Date;

public class PullRequestReviewComment {
    private String author;
	private String text;
	private String createdAt; //Keep it String for easier serialization
    private int line;
    private String avatarUrl;
    private Side side;
    public enum Side {LEFT, RIGHT}

	public PullRequestReviewComment(String author, String text, Date date, int line, String avatarUrl, org.kohsuke.github.GHPullRequestReviewComment.Side side) {
		this.author = author;
		this.text = text;
		this.createdAt = String.valueOf(date);
        this.line = line;
        this.avatarUrl = avatarUrl;
        this.side = side.equals(org.kohsuke.github.GHPullRequestReviewComment.Side.RIGHT) ? Side.RIGHT : Side.LEFT;
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

	public Side getSide() {
		return side;
	}
}
