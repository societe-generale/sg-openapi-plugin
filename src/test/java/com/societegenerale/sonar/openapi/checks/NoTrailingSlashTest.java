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

import org.junit.Test;
import org.sonar.openapi.OpenApiCheckVerifier;
import org.sonar.plugins.openapi.api.TestOpenApiVisitorRunner;
import org.sonar.plugins.openapi.api.OpenApiVisitorContext;
import org.sonar.plugins.openapi.api.PreciseIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class NoTrailingSlashTest {
  @Test
  public void verify_spinal_case_in_v2() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v2/trailing-slash.yaml", new NoTrailingSlashCheck(), true);
  }

  @Test
  public void verify_spinal_case_in_v3() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v3/trailing-slash.yaml", new NoTrailingSlashCheck(), false);
  }
}
