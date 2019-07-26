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

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.PreciseIssue;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = UseOAuth2Check.CHECK_KEY)
public class UseOAuth2Check extends OpenApiCheck {
  protected static final String CHECK_KEY = "UseOAuth2";

  private Map<String, Set<String>> scopesByScheme;
  private JsonNode globalSecurity;

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return ImmutableSet.of(OpenApi2Grammar.OPERATION, OpenApi3Grammar.OPERATION);
  }

  @Override
  protected void visitFile(JsonNode root) {
    scopesByScheme = new HashMap<>();
    if (root.getType() instanceof OpenApi2Grammar) {
      collectV2SecuritySchemes(root);
    } else {
      collectV3SecuritySchemes(root);
    }
    globalSecurity = root.at("/security");
  }

  private void collectV3SecuritySchemes(JsonNode root) {
    root.at("/components/securitySchemes").propertyMap()
        .forEach((k, v) -> {
          if (v.at("/type").getTokenValue().equals("oauth2")) {
            List<AstNode> flows = v.getDescendants(OpenApi3Grammar.IMPLICIT_FLOW, OpenApi3Grammar.CREDENTIALS_FLOW, OpenApi3Grammar.AUTH_FLOW, OpenApi3Grammar.PASSWORD_FLOW);
            Set<String> scopes = flows.stream()
                .flatMap(n->((JsonNode)n).at("/scopes").propertyNames().stream())
                .collect(Collectors.toSet());
            scopesByScheme.putIfAbsent(k, new HashSet<>());
            Set<String> existing = scopesByScheme.get(k);
            existing.addAll(scopes);
          }
        });
  }

  private void collectV2SecuritySchemes(JsonNode root) {
    root.at("/securityDefinitions").propertyMap()
        .forEach((k, v) -> {
          if (v.at("/type").getTokenValue().equals("oauth2")) {
            Collection<String> scopes = v.at("/scopes").propertyNames();
            scopesByScheme.putIfAbsent(k, new HashSet<>());
            Set<String> existing = scopesByScheme.get(k);
            existing.addAll(scopes);
          }
        });
  }

  @Override
  public void visitNode(JsonNode node) {
    List<Map.Entry<String, JsonNode>> requestedScopes = getRequestedOauth2Scopes(node);
    if (requestedScopes.isEmpty()) {
      addIssue("Endpoint is not secured by OAuth2 scope(s).", node.key());
    } else {
      Set<JsonNode> undefined = filterUndefined(requestedScopes);
      if (!undefined.isEmpty()) {
        PreciseIssue issue = addIssue("Endpoint is secured by undefined OAuth2 scope(s).", node.key());
        undefined.forEach(n -> issue.secondary(n, "Define this scope"));
      }
    }
  }

  private Set<JsonNode> filterUndefined(List<Map.Entry<String, JsonNode>> requested) {
    return requested.stream()
        .filter(e -> {
          Set<String> scopesInScheme = scopesByScheme.getOrDefault(e.getKey(), Collections.emptySet());
          return !scopesInScheme.contains(e.getValue().stringValue());
        })
        .map(Map.Entry::getValue)
        .collect(Collectors.toSet());
  }

  private List<Map.Entry<String, JsonNode>> getRequestedOauth2Scopes(JsonNode node) {
    JsonNode security = node.at("/security");
    if (security.isMissing()) {
      security = globalSecurity;
    }
    return security.elements().stream()
        .flatMap(n ->
            n.propertyMap().entrySet().stream()
                .filter(e -> scopesByScheme.containsKey(e.getKey())))
        .flatMap(e -> e.getValue().elements().stream().map(n -> new HashMap.SimpleEntry<>(e.getKey(), n)))
        .collect(Collectors.toList());
  }
}
