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

import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNodeType;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

import static com.societegenerale.sonar.openapi.checks.UseObjectForBodyCheck.CHECK_KEY;

@Rule(key = CHECK_KEY)
public class UseObjectForBodyCheck extends OpenApiCheck {
  protected static final String CHECK_KEY = "UseObjectForBody";

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.RESPONSE, OpenApi3Grammar.RESPONSE,
                           OpenApi2Grammar.PARAMETER, OpenApi3Grammar.REQUEST_BODY);
  }

  @Override
  protected void visitNode(JsonNode node) {
    if (node.getType() == OpenApi2Grammar.RESPONSE) {
      validateSchema(node.get("schema"));
    } else if (node.getType() == OpenApi3Grammar.RESPONSE){
      node.get("content").propertyMap().values()
          .forEach(n -> validateSchema(n.get("schema")));
    } else if (node.getType() == OpenApi2Grammar.PARAMETER){
      visitParameter(node);
    } else {
      visitRequestBody(node);
    }
  }

  private void visitParameter(JsonNode node) {
    String in = node.get("in").stringValue();
    if (!in.equals("body")) {
      return;
    }
    validateSchema(node.get("schema"));
  }

  private void visitRequestBody(JsonNode node) {
    for (JsonNode mediaType : node.get("content").properties()) {
      validateSchema(mediaType.get("schema"));
    }
  }

  private void validateSchema(JsonNode schema) {
    JsonNode resolved = schema.resolve();
    if (resolved.isMissing()) {
      return;
    }
    JsonNode type = resolved.get("type");
    if (type.isMissing()) {
      return;
    }
    if (!type.stringValue().equals("object")) {
      addIssue("Use only objects for body parameters and operation return types.", type);
    }
  }
}
