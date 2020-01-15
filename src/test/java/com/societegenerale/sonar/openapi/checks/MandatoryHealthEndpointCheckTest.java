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

public class MandatoryHealthEndpointCheckTest {

  @Test
  public void correct_health_v2() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v2/health/correct-health.yaml", new MandatoryHealthEndpointCheck(), true);
  }

  @Test
  public void missing_health_path_v2() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v2/health/missing-health.yaml", new MandatoryHealthEndpointCheck(), true);
  }

  @Test
  public void missing_health_get_operation_v2() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v2/health/missing-operation-get-health.yaml", new MandatoryHealthEndpointCheck(), true);
  }

  @Test
  public void missing_content_type_get_health_v2() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v2/health/missing-content-type-get-health.yaml", new MandatoryHealthEndpointCheck(), true);
  }

  @Test
  public void content_type_by_default_get_health_v2() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v2/health/default-content-type.yaml", new MandatoryHealthEndpointCheck(), true);
  }

  @Test
  public void bad_content_type_get_health_v2() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v2/health/bad-content-type-get-health.yaml", new MandatoryHealthEndpointCheck(), true);
  }

  @Test
  public void bad_response_schema_health_v2() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v2/health/bad-response-schema-health.yaml", new MandatoryHealthEndpointCheck(), true);
  }

  @Test
  public void correct_health_v3() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v3/health/correct-health.yaml", new MandatoryHealthEndpointCheck(), false);
  }

  @Test
  public void missing_health_path_v3() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v3/health/correct-health.yaml", new MandatoryHealthEndpointCheck(), false);
  }

  @Test
  public void missing_health_get_operation_v3() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v3/health/missing-operation-get-health.yaml", new MandatoryHealthEndpointCheck(), false);
  }

  @Test
  public void bad_response_code_health_v3() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v3/health/bad-response-code-health.yaml", new MandatoryHealthEndpointCheck(), false);
  }

  @Test
  public void bad_content_type_get_health_v3() {
    OpenApiCheckVerifier.verify("src/test/resources/checks/v3/health/bad-content-type-get-health.yaml", new MandatoryHealthEndpointCheck(), false);
  }

}
