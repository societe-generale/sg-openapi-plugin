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

import java.util.Arrays;
import java.util.List;

public final class SgCheckList {
  public static final String REPOSITORY_KEY = "sg-openapi";

  private SgCheckList() {
  }

  public static List<Class> getChecks() {
    return Arrays.asList(
      SpinalCaseUrlCheck.class,
      NoTrailingSlashCheck.class,
      CamelCaseParametersCheck.class,
      ApiVersionCheck.class,
      MandatoryHealthEndpointCheck.class,
      PostResponseCheck.class,
      UseOAuth2Check.class,
      UsePluralForResourcesCheck.class,
      DeleteResponseCheck.class,
      UseObjectForBodyCheck.class,
      UseIso8601Check.class,
      UsePluralForArrayPropertiesCheck.class
    );
  }
}
