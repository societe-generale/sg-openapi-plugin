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

import org.junit.Test;
import org.sonar.openapi.OpenApiCheckVerifier;

import static org.sonar.plugins.openapi.api.PathUtils.isSpinalCase;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpinalCaseUrlCheckTest {
  @Test
  public void verify_spinal_case_in_v2() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v2/spinal-case.yaml", new SpinalCaseUrlCheck(), true);
  }

  @Test
  public void verify_spinal_case_in_v3() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v3/spinal-case.yaml", new SpinalCaseUrlCheck(), false);
  }
}
