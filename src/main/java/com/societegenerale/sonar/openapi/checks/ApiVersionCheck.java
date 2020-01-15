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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.impl.MissingNode;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = ApiVersionCheck.CHECK_KEY)
public class ApiVersionCheck extends OpenApiCheck {
  protected static final String CHECK_KEY = "ApiVersion";
  protected static final String UNDEFINED_VERSION_MESSAGE = "The API MUST define a base path that starts with the API's major version.";
  protected static final String MISSING_VERSION_MESSAGE = "The API base path MUST start with the API's major version.";
  protected static final String UNALIGNED_VERSION_MESSAGE = "API version and API base path differ.";
  protected static final String API_VERSION_MESSAGE = "The API version is not in line with the base path.";
  private static final Pattern BASE_PATH_PATTERN = Pattern.compile("^/(v[0-9]+)(/[^/]+)*/?");
  private static final Pattern URL_PATTERN = Pattern.compile("^(http[s]?://[^/]+)?(/[^/]+)?(/[^/]+)*/?");
  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^{}]+)}");

  private JsonNode apiVersionNode;

  private static String extractVersion(JsonNode urlNode, Map<String, JsonNode> variables) {
    String url = urlNode.getTokenValue().trim();
    if (url.isEmpty()) {
      return url;
    }
    url = resolveVariables(url, variables);
    Matcher matcher = URL_PATTERN.matcher(url);
    if (matcher.matches()) {
      String version = matcher.group(2);
      return version == null ? "" : version;
    } else {
      return "";
    }
  }

  private static Map<String, JsonNode> extractVariables(JsonNode server) {
    Map<String, JsonNode> nodes = new HashMap<>();
    for (JsonNode node : server.at("/variables").propertyMap().values()) {
      nodes.put(node.key().getTokenValue(), node);
    }
    return nodes;
  }

  private static String resolveVariables(String version, Map<String, JsonNode> variables) {
    Matcher matcher = VARIABLE_PATTERN.matcher(version);
    Set<String> names = new HashSet<>();
    while (matcher.find()) {
      names.add(matcher.group());
    }
    for (String name : names) {
      JsonNode node = variables.get(name.substring(1, name.length() - 1));
      String paramValue;
      if (node == null) {
        paramValue = ""; // Unresolved parameter is a bug, detected in a separate rule
      } else {
        paramValue = node.at("/default").value().getTokenValue();
      }
      version = version.replace(name, paramValue);
    }
    return version;
  }

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return ImmutableSet.of(OpenApi2Grammar.ROOT, OpenApi3Grammar.SERVER, OpenApi2Grammar.PATH, OpenApi3Grammar.PATH);
  }

  @Override
  protected void visitFile(JsonNode node) {
    // The node is mandatory, so we can safely assume its presence
    apiVersionNode = node.at("/info/version").value();
    if (node.getType() == OpenApi3Grammar.ROOT) {
      JsonNode servers = node.get("servers");
      if (servers.elements().isEmpty()) {
        addIssue(UNDEFINED_VERSION_MESSAGE, node);
      }
    }
  }

  @Override
  public void visitNode(JsonNode node) {
    if (node.getType() == OpenApi2Grammar.ROOT) {
      checkV2BasePath(node);
    } else if (node.getType() == OpenApi3Grammar.SERVER) {
      checkV3BasePath(node);
    } else {
      checkOperationPath(node.key());
    }
  }

  private void checkOperationPath(JsonNode node) {
    String path = node.getTokenValue();
    Matcher matcher = BASE_PATH_PATTERN.matcher(path);
    if (matcher.matches()) {
      addIssue("Resource paths MUST NOT contain the API version. Use the basePath field instead.", node);
    }
  }

  private void checkV3BasePath(JsonNode server) {
    JsonNode urlNode = server.at("/url").value();
    Map<String, JsonNode> variables = extractVariables(server);
    String version = extractVersion(urlNode, variables);
    if (version.isEmpty()) {
      addIssue(UNDEFINED_VERSION_MESSAGE, urlNode);
    } else if (!BASE_PATH_PATTERN.matcher(version).matches()) {
      addIssue(MISSING_VERSION_MESSAGE, urlNode);
    } else {
      String apiVersion = apiVersionNode.getTokenValue();
      if (!apiVersion.startsWith(version.substring(2))) { // strip leading "/v"
        addIssue(UNALIGNED_VERSION_MESSAGE, urlNode)
          .secondary(apiVersionNode, API_VERSION_MESSAGE);
      }
    }
  }

  private void checkV2BasePath(JsonNode root) {
    JsonNode basePathNode = root.at("/basePath");
    if (basePathNode.isMissing()) {
      addLineIssue(UNDEFINED_VERSION_MESSAGE, root.getTokenLine());
    } else {
      String basePath = basePathNode.value().getTokenValue();
      checkVersionInBasePath(basePathNode.value(), basePath);
    }
  }

  private void checkVersionInBasePath(JsonNode basePathNode, String basePath) {
    Matcher matcher = BASE_PATH_PATTERN.matcher(basePath);
    if (matcher.matches()) {
      String version = matcher.group(1);
      String apiVersion = apiVersionNode.getTokenValue();
      if (!apiVersion.startsWith(version.substring(1))) { // strip leading "v"
        addIssue(UNALIGNED_VERSION_MESSAGE, basePathNode)
          .secondary(apiVersionNode, "The API version is not in line with the base path.");
      }
    } else {
      addIssue(MISSING_VERSION_MESSAGE, basePathNode);
    }
  }

}
