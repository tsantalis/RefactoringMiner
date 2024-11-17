/*-
 * #%L
 * Bobcat
 * %%
 * Copyright (C) 2018 Cognifide Ltd.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.cognifide.qa.bb.page;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Names;

/**
 * Factory to create pages.
 * <p>
 * Creates dynamically instances of {@link Page}.
 */
public class BobcatPageFactory {

  public static final String BOBCAT_PAGE_PATH = "bobcat.page.path";

  @Inject
  private Injector injector;

  /**
   * Create pages from selected path and selected type
   *
   * @param path path to the page
   * @param tClass class of the page
   * @param <T> type of the page
   * @return a Page instance specified by path
   */
  public <T extends Page> T create(String path, Class<T> tClass) {
    AbstractModule module = new AbstractModule() {
      @Override
      protected void configure() {
        bind(String.class).annotatedWith(Names.named(BOBCAT_PAGE_PATH)).toInstance(path);
      }
    };
    Injector childInjector = injector.createChildInjector(module);
    return childInjector.getInstance(tClass);
  }

  /**
   * Create pages from selected path and selected type
   *
   * @param path path to the page
   * @return a Page instance specified by path
   */
  public ActivePage create(String path) {
    AbstractModule module = new AbstractModule() {
      @Override
      protected void configure() {
        bind(String.class).annotatedWith(Names.named(BOBCAT_PAGE_PATH)).toInstance(path);
      }
    };
    Injector childInjector = injector.createChildInjector(module);
    return childInjector.getInstance(ActivePage.class);
  }
}
