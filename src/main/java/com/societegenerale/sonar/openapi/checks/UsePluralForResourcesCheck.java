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

import javatools.parsers.PlingStemmer;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.ResourceCheck;
import org.sonar.sslr.yaml.grammar.JsonNode;

import static org.sonar.plugins.openapi.api.PathUtils.terminalSegment;

@Rule(key = UsePluralForResourcesCheck.CHECK_KEY)
public class UsePluralForResourcesCheck extends ResourceCheck {
  public static final String CHECK_KEY = "UsePluralForResources";

  @Override
  protected void visitResource(JsonNode node) {
    String path = node.key().stringValue();
    path = terminalSegment(path);
    if (path.isEmpty()) {
      return;
    }
    String[] fragments = path.split("-");
    if (!PlingStemmer.isPlural(fragments[fragments.length - 1])) {
      addIssue("Use plural nouns for resource paths.", node.key());
    }
  }
}
