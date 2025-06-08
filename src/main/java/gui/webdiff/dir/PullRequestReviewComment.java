package gui.webdiff.dir;

import java.util.Date;

public class PullRequestReviewComment {
	private final String authorLogin;
	private final String comment;
	private final Date createdAt;

	public PullRequestReviewComment(String authorLogin, String comment, Date date) {
		this.authorLogin = authorLogin;
		this.comment = comment;
		this.createdAt = date;
	}
	@Override
	public String toString() {
		return authorLogin + " @" + createdAt + ": " + comment;
	}
}
