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
package com.cognifide.qa.bb.aem.core.pages;

import java.util.List;

import javax.inject.Named;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.cognifide.qa.bb.aem.core.component.GlobalBar;
import com.cognifide.qa.bb.frame.FrameSwitcher;
import com.cognifide.qa.bb.mapper.field.PageObjectProviderHelper;
import com.cognifide.qa.bb.page.Page;
import com.cognifide.qa.bb.qualifier.PageObjectInterface;
import com.cognifide.qa.bb.utils.PageObjectInjector;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.internal.LinkedBindingImpl;

import io.qameta.allure.Step;

/**
 * Represents a generic AEM page. Users should use the {@link com.cognifide.qa.bb.page.BobcatPageFactory} to obtain instances of such classes.
 *
 * @param <T> type of the page
 */
public class AemAuthorPage<T extends AemAuthorPage> extends Page {

  private static final String CONTENT_FRAME = "ContentFrame";

  @Inject
  private GlobalBar globalBar;

  @Inject
  private FrameSwitcher frameSwitcher;

  @Inject
  @Named("author.url")
  protected String authorUrl;

  /**
   * Returns the nth page object representing given component. Switches to the ContentFrame during the process.
   *
   * @param component class of the page object representing the component to be returned
   * @param order     which component should be returned in case there are multiple instances
   * @param <X>       type of the component
   * @return a page object representing the requested component
   */
  public <X> X getContent(Class<X> component, int order) {
    globalBar.switchToPreviewMode();
    frameSwitcher.switchTo(CONTENT_FRAME);
    By selector = getSelectorFromComponent(component);
    List<WebElement> scope = webDriver.findElements(selector);
    frameSwitcher.switchBack();
    return scope == null
        ? pageObjectInjector.inject(component, CONTENT_FRAME)
        : pageObjectInjector.inject(component, scope.get(order), CONTENT_FRAME);
  }

  /**
   * Open the page in browser
   */
  @Step("Open page")
  @Override
  public T open() {
    webDriver.get(authorUrl + getFullUrl());
    return (T) this;
  }

  /**
   * Opens the page in the edit mode
   *
   * @return self-reference
   */
  @Step("Open page in editor")
  public T openInEditor() {
    webDriver.get(authorUrl + "/editor.html" + getFullUrl());
    return (T) this;
  }

}
