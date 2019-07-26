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
package com.societegenerale.sonar.openapi;

import com.societegenerale.sonar.openapi.checks.SgCheckList;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCustomRuleRepository;

import javax.annotation.Nullable;
import java.util.List;

public class SgWayProfileDefinition implements BuiltInQualityProfilesDefinition {
  public static final String SG_WAY_PROFILE = "SG way";
  private final OpenApiCustomRuleRepository[] repositories;

  public SgWayProfileDefinition() {
    this(null);
  }

  public SgWayProfileDefinition(@Nullable OpenApiCustomRuleRepository[] repositories) {
    this.repositories = repositories;
  }

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(SG_WAY_PROFILE, "openapi");
    addRepositoryRules(profile, SgCheckList.REPOSITORY_KEY, SgCheckList.getChecks());
    if (repositories != null) {
      for (OpenApiCustomRuleRepository repository : repositories) {
        if (repository.repositoryKey().equals("openapi")) {
          addRepositoryRules(profile, repository.repositoryKey(), repository.checkClasses());
        }
      }
    }
    profile.done();
  }

  public void addRepositoryRules(NewBuiltInQualityProfile profile, String key, List<Class> checks) {
    for (Class check : checks) {
      Rule annotation = AnnotationUtils.getAnnotation(check, Rule.class);
      profile.activateRule(key, annotation.key());
    }
  }
}
