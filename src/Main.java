import gr.uom.java.xmi.ASTReader;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.io.File;
import java.util.List;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//SVNDataExtraction extraction = new SVNDataExtraction(false);
		//extraction.diffRevisions();
		//GitDataExtraction extraction = new GitDataExtraction(false);
		//extraction.diffRevisions();
		/*AbstractRepository repository = extraction.getRepository();
		System.out.println(repository.getCommitters());
		
		Map<String, Integer> countMap = repository.getCommitCountPerAuthor();
		for(String author : countMap.keySet()) {
			System.out.println(author + "\t" + countMap.get(author));
		}*/
		/*Map<DateTime, Integer> testPeriods = extraction.detectTestModificationPeriods();
		for(DateTime date : testPeriods.keySet()) {
			System.out.println(date.toString().substring(0, 10) + "\t" + testPeriods.get(date));
		}*/
		//extraction.diffRevisionsForGit();
		//DateTime releaseDate = new DateTime(2001, 1, 18, 0, 0, 0, 0); //3.5
		//DateTime releaseDate = new DateTime(2001, 4, 8, 0, 0, 0, 0); //3.6
		//DateTime releaseDate = new DateTime(2001, 5, 21, 0, 0, 0, 0); //3.7
		//DateTime releaseDate = new DateTime(2002, 8, 24, 0, 0, 0, 0); //3.8
		//DateTime releaseDate = new DateTime(2002, 9, 4, 0, 0, 0, 0); //3.8.1
		//DateTime releaseDate = new DateTime(2006, 2, 16, 0, 0, 0, 0); //4.0
		//DateTime releaseDate = new DateTime(2006, 3, 3, 0, 0, 0, 0); //3.8.2
		//DateTime releaseDate = new DateTime(2006, 5, 3, 0, 0, 0, 0); //4.1
		//DateTime releaseDate = new DateTime(2006, 11, 16, 0, 0, 0, 0); //4.2
		//DateTime releaseDate = new DateTime(2007, 3, 29, 0, 0, 0, 0); //4.3.1
		//DateTime releaseDate = new DateTime(2007, 11, 7, 0, 0, 0, 0); //4.4
		//DateTime releaseDate = new DateTime(2008, 8, 19, 0, 0, 0, 0); //4.5
		//DateTime releaseDate = new DateTime(2009, 4, 14, 0, 0, 0, 0); //4.6
		//DateTime releaseDate = new DateTime(2009, 8, 4, 0, 0, 0, 0); //4.7
		//DateTime releaseDate = new DateTime(2009, 12, 1, 0, 0, 0, 0); //4.8
		//DateTime releaseDate = new DateTime(2011, 8, 22, 0, 0, 0, 0); //4.9
		//DateTime releaseDate = new DateTime(2011, 9, 29, 0, 0, 0, 0); //4.10
		//extraction.getWindows(releaseDate, 60);
		//f268f458843fc84e83c244a5c271cc71386eb1cf 4b1869ebb8002e5d0b82ab55460f6126043c9ec4
		//29416b4ee89314119fb0f2d8011d63ac36531388 8817825612894d11856312a777281b6c7cf81672
		//1e752dcb0a967bcb47fae4b12fb8765e79571e69 bfc94ee739d62127c7477300b26127bd560765a1
		//String CHECKOUT_DIR = Constants.getValue("GIT_CHECKOUT_DIR");
		//UMLModel model1 = new ASTReader(new File("C:/Users/danilofs/git/mestrado/Refactoring1")).getUmlModel();
		//UMLModel model2 = new ASTReader(new File("C:/Users/danilofs/git/mestrado/Refactoring2")).getUmlModel();
		//UMLModel model1 = new ASTReader(new File("C:/Users/danilofs/git/mestrado/MyWebMarket Original")).getUmlModel();
		//UMLModel model2 = new ASTReader(new File("C:/Users/danilofs/Workspaces/mestrado/RefactoringA")).getUmlModel();
		//UMLModelDiff modelDiff = model1.diff(model2);
		//modelDiff.postProcessing();
		//System.out.println(modelDiff);
		/*List<Refactoring> refactorings = modelDiff.getRefactorings();
		for(Refactoring refactoring : refactorings) {
			System.out.println(refactoring.toString());
		}*/
		
		String prefix = "VideoStore_Class_Sequence_diagrams\\";
		//String prefix = "C:\\Users\\tsantalis\\runtime-EclipseApplication\\jfreechart-1.0.";
		//String prefix = "test\\";
		for(int i=0; i<14; i++) {
			UMLModel model1 = new ASTReader(new File(prefix + String.valueOf(i))).getUmlModel();
			UMLModel model2 = new ASTReader(new File(prefix + String.valueOf(i+1))).getUmlModel();
			System.out.println("model diff " + String.valueOf(i) + "->" + String.valueOf(i+1));
			UMLModelDiff modelDiff = model1.diff(model2);
			System.out.println(modelDiff);
			List<Refactoring> refactorings = modelDiff.getRefactorings();
			for(Refactoring refactoring : refactorings) {
				System.out.println(refactoring.toString());
			}
		}
		/*try {
			String prefix = "VideoStore_Class_Sequence_diagrams\\";
			for(int i=13; i<14; i++) {
				UMLModel model1 = new DOMReader().read(new File(prefix + String.valueOf(i) + "\\Class&SequenceDiagram.xmi"));
				UMLModel model2 = new DOMReader().read(new File(prefix + String.valueOf(i+1) + "\\Class&SequenceDiagram.xmi"));
				System.out.println("model diff " + String.valueOf(i) + "->" + String.valueOf(i+1));
				UMLModelDiff modelDiff = model1.diff(model2);
				System.out.println(modelDiff);
			}
			//XMIModelExtractor modelExtractor = new XMIModelExtractor(new File("JFreeChart_Class_diagrams\\jfreechart-1.0.0.xmi"));
			//XMIModelExtractor modelExtractor = new XMIModelExtractor(new File("jfreechart-1.0.0-2.1_VP.xmi.uml"));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}*/
	}
}
