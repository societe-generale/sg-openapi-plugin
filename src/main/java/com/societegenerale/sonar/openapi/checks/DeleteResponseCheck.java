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
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = DeleteResponseCheck.CHECK_KEY)
public class DeleteResponseCheck extends OpenApiCheck {
  public static final String CHECK_KEY = "DeleteResponse";

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.OPERATION, OpenApi3Grammar.OPERATION);
  }

  @Override
  protected void visitNode(JsonNode node) {
    String key = node.key().stringValue();
    if (!"delete".equals(key)) {
      return;
    }
    validateStatusCodes(node);
  }

  private void validateStatusCodes(JsonNode node) {
    JsonNode responsesNode = node.at("/responses");
    Map<String, JsonNode> responses = responsesNode.propertyMap();

    Map<String, JsonNode> successResponses = responses.entrySet().stream()
      .filter(e -> e.getKey().startsWith("2"))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (!successResponses.containsKey("204")) {
      addIssue("DELETE operations MUST return 204 No Content for successful response.", responsesNode.key());
    } else if (successResponses.size() > 1) {
      addIssue("DELETE operations MUST return only 204 No Content for successful response.", responsesNode.key());
    }

    responses.entrySet().stream()
        .filter(e -> e.getKey().equals("404"))
        .map(Map.Entry::getValue)
        .forEach(e -> addIssue("DELETE operations MUST NOT return 404 Missing.", e.key()));
  }
}
