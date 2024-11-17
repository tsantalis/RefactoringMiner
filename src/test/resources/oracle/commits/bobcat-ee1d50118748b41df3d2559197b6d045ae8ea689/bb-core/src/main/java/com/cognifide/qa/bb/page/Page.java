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

import static com.cognifide.qa.bb.page.BobcatPageFactory.BOBCAT_PAGE_PATH;

import com.cognifide.qa.bb.mapper.field.PageObjectProviderHelper;
import com.cognifide.qa.bb.qualifier.PageObjectInterface;
import com.cognifide.qa.bb.utils.PageObjectInjector;
import com.google.inject.Binding;
import com.google.inject.internal.LinkedBindingImpl;
import java.util.List;
import javax.inject.Named;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.google.inject.Inject;

import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Abstract class that represents a generic page
 */
public class Page<T extends Page> {

  @Inject
  protected WebDriver webDriver;

  @Inject
  @Named(BOBCAT_PAGE_PATH)
  protected String fullUrl;

  @Inject
  protected PageObjectInjector pageObjectInjector;

  /**
   * Return first page object of given class on page
   * @param pageObject - page object class
   * @param <X> Class that should be return
   * @return Instance of class
   */
  public <X> X getPageObject(Class<X> pageObject) {
    return getPageObject(pageObject, 0);
  }

  public <X> X getPageObject(Class<X> pageObject, int order) {
    By selector = getSelectorFromComponent(pageObject);
    List<WebElement> scope = webDriver.findElements(selector);
    return scope == null
        ? pageObjectInjector.inject(pageObject)
        : pageObjectInjector.inject(pageObject, scope.get(order));
  }

  /**
   * open the page in browser
   */
  @Step("Open page")
  public T open() {
    webDriver.get(getFullUrl());
    return (T) this;
  }

  /**
   * return full url
   */
  public String getFullUrl() {
    return fullUrl;
  }

  protected <X> By getSelectorFromComponent(Class<X> component) {
    By selector = null;
    if (component.isAnnotationPresent(
        PageObjectInterface.class)) {
      Binding<?> binding = pageObjectInjector.getOriginalInjector().getBinding(component);
      if (binding instanceof LinkedBindingImpl) {
        selector = PageObjectProviderHelper
            .retrieveSelectorFromPageObjectInterface(
                ((LinkedBindingImpl) binding).getLinkedKey().getTypeLiteral().getRawType());
      }
    } else {
      selector = PageObjectProviderHelper.retrieveSelectorFromPageObjectInterface(component);
    }
    return selector;
  }

}
