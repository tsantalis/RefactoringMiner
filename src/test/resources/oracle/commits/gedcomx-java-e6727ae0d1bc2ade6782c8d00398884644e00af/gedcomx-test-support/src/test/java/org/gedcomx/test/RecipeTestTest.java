package org.gedcomx.test;

import org.junit.After;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;


/**
 * @author Mike Gardiner
 */
public class RecipeTestTest extends RecipeTest {

  /**
   * Create a recipe and verify it exists
   *
   * @throws Exception - An error occurred
   */
  @Test
  public void testCreateRecipe() throws Exception {
    final String title = "title";

    createRecipe(title)
      .withDescription("description")
      .applicableTo(getClass());

    Snippet snippet = new Snippet("snippet description");
    CustomElement el = new CustomElement();
    el.setProp1("property one");
    el.setProp2("property two");
    CustomElement xmlResult = processThroughXml(el, snippet);
    CustomElement jsonResult = processThroughJson(el, snippet);
    assertEquals("property one", xmlResult.getProp1());
    assertEquals("property one", jsonResult.getProp1());
    assertEquals("property two", xmlResult.getProp2());
    assertEquals("property two", jsonResult.getProp2());
    addSnippet(snippet);

    endRecipe();

    assertEquals(1, RECIPES.get().size());
    Recipe r = RECIPES.get().get(0);
    assertEquals(title, r.getTitle());
    assertEquals("description", r.getDescription());
    assertEquals(1, r.getSnippets().size());
    assertTrue(r.getSnippets().get(0).getXml().contains("property one"));
    assertTrue(r.getSnippets().get(0).getJson().contains("property one"));

    super.tearDown();
    // Make sure file exists
    assertTrue(fileExists(title));
  }

  /**
   * Make sure an exception is thrown if a recipe is created without
   * a title
   */
  @Test
  public void testRecipeNoTitle() throws Exception {
    // Null Title
    try {
      createRecipe(null);
      fail();  // Shouldn't get here
    }
    catch (Exception e) {
      //fall through...
    }

    // Empty Title
    try {
      createRecipe("");
      fail();  // Shouldn't get here
    }
    catch (Exception e) {
    }

    super.tearDown();
  }

  /**
   * Test to ensure that all the recipe titles are unique
   *
   * @throws Exception - Shouldn't throw anything since we trap the exception
   */
  @Test
  public void testTestNonUniqueTitle() throws Exception {

    try {
      createRecipe("title2")
        .withDescription("description");

      createRecipe("title2")
        .withDescription("description");

      super.tearDown();
      fail();  // We shouldn't get here
    }
    catch (Exception e) {
      assertTrue(true);  // We should get here
    }
  }

  /**
   * Helper method for seeing if a file exists. The title is used to build
   * the filename.
   *
   * EXAMPLE: A title of "Unit Test" would create a file called
   * "Unit-Test.Recipe.xml" in the target/generated-doc directory.
   *
   * @param title - Title of the Recipe
   * @return true if the file indicated by the title exists
   */
  private boolean fileExists(String title) {
    StringBuilder sb = new StringBuilder(DEFAULT_OUTPUT_DIR);
    sb.append(File.separator);
    sb.append(title);
    sb.append(".recipe.xml");

    File file = new File(sb.toString());

    return file.exists();
  }

  /**
   * Override tearDown since we are testing the logic in each test
   *
   * @throws Exception - An exception occurred
   */
  @Override
  public void tearDown() throws Exception {
    // This is kind of weird to me.  The super class (RecipeTest.java) has a tearDown() method also annotated with @After, so the method is to be called
    // after running a test, but this is the actual method (not the super method) that is called
  }

  @After
  public void cleanUp() {
    File dir = new File( DEFAULT_OUTPUT_DIR );
    if (dir.exists()) {
      File[] contents = dir.listFiles();
      for (File file : contents) {
        if (file.isFile())
          file.delete();
      }
    }
  }
}
