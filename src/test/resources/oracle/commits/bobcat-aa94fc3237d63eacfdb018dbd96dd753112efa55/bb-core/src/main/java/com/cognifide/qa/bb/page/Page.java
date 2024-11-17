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

import javax.inject.Named;

import org.openqa.selenium.WebDriver;

import com.google.inject.Inject;

import io.qameta.allure.Step;

/**
 * Abstract class that represents a generic page
 */
public class Page<T extends Page> {

  @Inject
  protected WebDriver webDriver;

  @Inject
  @Named(BOBCAT_PAGE_PATH)
  protected String fullUrl;

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

}
