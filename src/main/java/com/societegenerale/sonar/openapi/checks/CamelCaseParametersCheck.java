/*
 * OpenAPI :: Societe Generale API Guidelines Checks
 * Copyright (C) 2019-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package com.societegenerale.sonar.openapi.checks;

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstNodeType;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = CamelCaseParametersCheck.CHECK_KEY)
public class CamelCaseParametersCheck extends OpenApiCheck {
  protected static final String CHECK_KEY = "CamelCaseParameters";
  protected static final String MESSAGE = "Use CamelCase names for query parameters and json properties.";
  private static final Pattern PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9]*");

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return ImmutableSet.of(OpenApi2Grammar.SCHEMA, OpenApi3Grammar.SCHEMA, OpenApi2Grammar.PARAMETER, OpenApi3Grammar.PARAMETER);
  }

  @Override
  public void visitNode(JsonNode node) {
    if (node.getType() == OpenApi2Grammar.SCHEMA || node.getType() == OpenApi3Grammar.SCHEMA) {
      checkSchema(node);
    } else if (node.getType() == OpenApi3Grammar.PARAMETER) {
      checkParameter(node, "query");
    } else {
      checkParameter(node, "query", "formData", "body");
    }
  }

  private void checkSchema(JsonNode node) {
    JsonNode properties = node.at("/properties");
    if (properties != null) {
      for (JsonNode property : properties.properties()) {
        checkCamelCase(property.key());
      }
    }
  }

  private void checkParameter(JsonNode node, String... locationsToCheck) {
    JsonNode in = node.at("/in");
    for (String l : locationsToCheck) {
      if (l.equals(in.getTokenValue())) {
        checkCamelCase(node.at("/name"));
        return;
      }
    }
  }

  private void checkCamelCase(JsonNode node) {
    if (node.isMissing()) {
      return;
    }
    if (!PATTERN.matcher(node.getTokenValue()).matches()) {
      addIssue(MESSAGE, node);
    }
  }

}
