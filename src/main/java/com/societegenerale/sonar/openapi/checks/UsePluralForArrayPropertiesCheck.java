/*
 * OpenAPI :: Societe Generale API Guidelines Checks
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNodeType;
import java.util.Map;
import java.util.Set;
import javatools.parsers.PlingStemmer;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = UsePluralForArrayPropertiesCheck.CHECK_KEY)
public class UsePluralForArrayPropertiesCheck extends OpenApiCheck {
  public static final String CHECK_KEY = "UsePluralForArrayProperties";

  @Override
  public final Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.SCHEMA, OpenApi3Grammar.SCHEMA, OpenApi2Grammar.PARAMETER);
  }

  @Override
  protected void visitNode(JsonNode node) {
    if(node.getType() == OpenApi2Grammar.PARAMETER) {
      visitParameter(node);
    } else {
      visitSchema(node);
    }
  }

  private void visitParameter(JsonNode node) {
    String type = node.resolve().get("type").stringValue();
    if (type.equals("array") && !PlingStemmer.isPlural(node.get("name").stringValue())) {
      addIssue("Use plural nouns for array properties.", node.get("name"));
    }
  }

  private void visitSchema(JsonNode node) {
    String type = node.resolve().get("type").stringValue();
    if (!type.equals("object")) {
      return;
    }

    for (Map.Entry<String, JsonNode> entry : node.get("properties").propertyMap().entrySet()) {
      JsonNode model = entry.getValue().resolve();
      type = model.get("type").stringValue();
      if (type.equals("array") && !PlingStemmer.isPlural(entry.getKey())) {
        //entry.getValue().getKey() to be sure to come back to the original propertie's key
        addIssue("Use plural nouns for array properties.", entry.getValue().key());
      }
    }
  }
}
