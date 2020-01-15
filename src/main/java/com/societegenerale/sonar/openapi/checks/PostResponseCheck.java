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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = PostResponseCheck.CHECK_KEY)
public class PostResponseCheck extends OpenApiCheck {

  private static final String BAD_OR_MISSING_201_HEADER = "201 Created MUST return a Location string header.";
  private static final String BAD_OR_MISSING_202_HEADER = "202 Accepted MUST return Location and Content-Location string headers.";
  public static final String CHECK_KEY = "PostResponse";

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.OPERATION, OpenApi3Grammar.OPERATION);
  }

  @Override
  protected void visitNode(JsonNode node) {
    String key = node.key().getTokenValue();
    if (!"post".equals(key)) {
      return;
    }
    Map<String, JsonNode> statusCodes = validateStatusCodes(node);
    validateStatusCode(statusCodes.get("201"), BAD_OR_MISSING_201_HEADER, "Location");
    validateStatusCode(statusCodes.get("202"), BAD_OR_MISSING_202_HEADER, "Location", "Content-Location");
  }

  private Map<String, JsonNode> validateStatusCodes(JsonNode node) {
    JsonNode responsesNode = node.at("/responses");
    Map<String, JsonNode> responses = responsesNode.propertyMap();

    Map<String, JsonNode> successResponses = responses.entrySet().stream()
      .filter(e -> e.getKey().startsWith("2"))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (!successResponses.containsKey("201") && !successResponses.containsKey("202")) {
      addIssue("POST operations MUST return 201 Created or 202 Accepted for successful response.", responsesNode.key());
    } else if (successResponses.containsKey("201") && successResponses.containsKey("202")) {
      addIssue("POST operations MUST return only one of 201 Created and 202 Accepted for successful response.", responsesNode.key());
    }

    return successResponses;
  }

  private void validateStatusCode(@Nullable JsonNode response, String errorMessage, String... requiredHeaders) {
    if (response == null) {
      return;
    }
    JsonNode effective = response.resolve();

    for (String headerPath : requiredHeaders) {
      validateHeader(headerPath, effective, response.key(), errorMessage);
    }
  }

  private void validateHeader(String headerName, JsonNode response, JsonNode errorLocation, String errorMessage) {
    JsonNode locationHeader = response.at("/headers/" + headerName).value();
    if (locationHeader.isMissing()) {
      addIssue(errorMessage, errorLocation);
      return;
    }

    String typePath;
    if (locationHeader.getType() == OpenApi2Grammar.HEADER) {
      typePath = "/type";
    } else {
      typePath = "/schema/type";
    }
    JsonNode typeNode = locationHeader.at(typePath).value();
    if (typeNode == null || !"string".equals(typeNode.getTokenValue())) {
      addIssue(errorMessage, errorLocation);
    }
  }
}
