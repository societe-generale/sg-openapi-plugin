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
import com.sonar.sslr.api.AstNodeType;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = NoTrailingSlashCheck.CHECK_KEY)
public class NoTrailingSlashCheck extends OpenApiCheck {
  protected static final String CHECK_KEY = "NoTrailingSlash";
  protected static final String MESSAGE = "Don't use terminal '/' for paths.";

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return ImmutableSet.of(OpenApi2Grammar.PATH, OpenApi3Grammar.PATH);
  }

  @Override
  public void visitNode(JsonNode node) {
    JsonNode keyNode = node.key();
    String path = keyNode.getTokenValue();
    if (path.startsWith("/") && path.endsWith("/")) {
      addIssue(MESSAGE, keyNode);
    }
  }
}
