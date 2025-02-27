/**
 * Copyright Intellectual Reserve, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gedcomx.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.After;

/**
 * Base class for a test class that can be used for creating recipes.
 *
 * @author Mike Gardiner
 * @author Ryan Heaton
 */
public class RecipeTest {

  static final ThreadLocal<org.gedcomx.test.Recipe> CURRENT_RECIPE = new ThreadLocal<org.gedcomx.test.Recipe>();
  static final ThreadLocal<List<Recipe>> RECIPES = new ThreadLocal<List<Recipe>>() {
    @Override
    protected List<Recipe> initialValue() {
      return new ArrayList<org.gedcomx.test.Recipe>();
    }
  };
  static final String DEFAULT_OUTPUT_DIR = "target" + File.separator + "recipes";

  /**
   * Create a new recipe
   *
   * @param title - Title of the recipe
   * @return the recipe
   */
  protected Recipe createRecipe(String title) {
    if (title == null || title.isEmpty()) {
      throw new IllegalArgumentException("title must not be null or empty");
    }

    Recipe recipe = new Recipe();
    recipe.setTitle(title);
    setCurrentRecipe(recipe);
    return recipe;
  }

  protected void addSnippet(Snippet snippet) {
    Recipe currentRecipe = CURRENT_RECIPE.get();
    if (currentRecipe == null) {
      throw new IllegalStateException("No recipe has been initialized to accommodate the snippet.");
    }
    currentRecipe.addSnippet(snippet);
  }

  private void setCurrentRecipe(org.gedcomx.test.Recipe recipe) {
    org.gedcomx.test.Recipe old = CURRENT_RECIPE.get();
    if (old != null) {
      RECIPES.get().add(old);
    }

    CURRENT_RECIPE.set(recipe);
  }

  @After
  public void tearDown() throws Exception {
    endRecipe();

    String recipesDir = System.getProperty("recipes.dir");
    if (recipesDir == null) {
      recipesDir = DEFAULT_OUTPUT_DIR;
    }

    File directory = new File(recipesDir);
    directory.mkdirs();

    Marshaller marshaller = JAXBContext.newInstance(Recipe.class).createMarshaller();

    boolean ensureUniqueRecipes = !"false".equals(System.getProperty("ensure.unique.recipes")) && !isRunningFromIntelliJ();
    for (Recipe recipe : RECIPES.get()) {
      File file = new File(recipesDir, generateFilename(recipe.getTitle()));

      if (ensureUniqueRecipes && file.exists()) {
        RECIPES.get().clear();                // this throws but we must still clear the recipes so we don't have side affects on other tests
        throw new Exception("File is not unique, please ensure the recipe title is unique!\n" + recipe.getTitle());
      }

      OutputStream os = new FileOutputStream(file);
      marshaller.marshal(recipe, os);
      os.close();
    }
    RECIPES.get().clear();
  }

  private boolean isRunningFromIntelliJ() {
    try {
      Class.forName("com.intellij.rt.execution.application.AppMain", false, getClass().getClassLoader());
      return true;
    }
    catch (Throwable e) {
      return false;
    }
  }

  /**
   * Generate a new filename based on the title of the recipe
   *
   * @param title - The title to use
   * @return String representing the filename
   */
  private String generateFilename(String title) {
    StringBuilder filename = new StringBuilder(title.replace(' ', '-'));
    filename.append(".recipe.xml");
    return filename.toString();
  }

  /**
   * Marks the recipe as ended
   */
  protected void endRecipe() {
    setCurrentRecipe(null);
  }
}
