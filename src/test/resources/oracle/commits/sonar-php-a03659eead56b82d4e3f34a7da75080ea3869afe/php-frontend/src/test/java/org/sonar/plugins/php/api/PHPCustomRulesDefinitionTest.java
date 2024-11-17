/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.php.api;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.php.api.visitors.PHPCustomRulesDefinition;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

import static org.fest.assertions.Assertions.assertThat;

public class PHPCustomRulesDefinitionTest {

  private static final String REPOSITORY_NAME = "Custom Rule Repository";
  private static final String REPOSITORY_KEY = "CustomRuleRepository";

  private static final String RULE_NAME = "This is my custom rule";
  private static final String RULE_KEY = "MyCustomRule";

  @Test
  public void test() {
    MyCustomJavaScriptRulesDefinition rulesDefinition = new MyCustomJavaScriptRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo(REPOSITORY_NAME);
    assertThat(repository.language()).isEqualTo(PHPCustomRulesDefinition.LANGUAGE_KEY);
    assertThat(repository.rules()).hasSize(1);

    RulesDefinition.Rule customRule = repository.rule(RULE_KEY);
    assertThat(customRule).isNotNull();
    assertThat(customRule.name()).isEqualTo(RULE_NAME);
    assertThat(customRule.htmlDescription()).isEqualTo("desc");
    assertThat(customRule.tags()).contains("mybug");

    assertThat(customRule.params()).hasSize(1);

    RulesDefinition.Param param = customRule.param("customParam");
    assertThat(param.key()).isEqualTo("customParam");
    assertThat(param.description()).isEqualTo("Custom parameter");
    assertThat(param.defaultValue()).isEqualTo("value");
  }

  @Rule(
    key = RULE_KEY,
    name = RULE_NAME,
    description = "desc",
    tags = {"mybug"})
  public class MyCustomRule extends PHPVisitorCheck {
    @RuleProperty(
      key = "customParam",
      description = "Custom parameter",
      defaultValue = "value")
    public String customParam = "value";
  }

  public static class MyCustomJavaScriptRulesDefinition extends PHPCustomRulesDefinition {

    @Override
    public String repositoryName() {
      return REPOSITORY_NAME;
    }

    @Override
    public String repositoryKey() {
      return REPOSITORY_KEY;
    }

    @Override
    public ImmutableList<Class> checkClasses() {
      return ImmutableList.<Class>of(MyCustomRule.class);
    }
  }

}

