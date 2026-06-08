package gr.uom.java.xmi;

import java.util.List;

public interface CommentProvider extends LocationInfoProvider {
	List<UMLComment> getComments();
}
