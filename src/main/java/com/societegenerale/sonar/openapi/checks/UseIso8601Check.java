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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNodeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.PreciseIssue;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.impl.MissingNode;

import static org.sonar.plugins.openapi.api.v3.OpenApi3Grammar.HEADER;
import static org.sonar.plugins.openapi.api.v3.OpenApi3Grammar.PARAMETER;
import static org.sonar.plugins.openapi.api.v3.OpenApi3Grammar.SCHEMA;

@Rule(key = UseIso8601Check.CHECK_KEY)
public class UseIso8601Check extends OpenApiCheck {
  // 2014-25-12T12:04:13.134Z
  public static final Pattern ISO_8601_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([.,]\\d+)?Z");
  // 2014-25-12
  public static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  protected static final String CHECK_KEY = "UseIso8601";
  private IdentityHashMap<JsonNode, PreciseIssue> issues = new IdentityHashMap<>();

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.PARAMETER, PARAMETER,
        OpenApi2Grammar.SCHEMA, SCHEMA,
        OpenApi2Grammar.HEADER, HEADER);
  }

  @Override
  protected void visitNode(JsonNode node) {
    if (node.getType() == OpenApi3Grammar.PARAMETER || node.getType() == OpenApi3Grammar.HEADER) {
      visitParameterV3(node);
    } else if (node.getType() == OpenApi2Grammar.PARAMETER || node.getType() == OpenApi2Grammar.HEADER) {
      visitParameterV2(node);
    } else if (node.getType() == OpenApi3Grammar.SCHEMA || node.getType() == OpenApi2Grammar.SCHEMA) {
      visitSchema(node);
    }
  }

  private void visitParameterV3(JsonNode node) {
    JsonNode schema = node.get("schema");
    JsonNode content = node.get("content");
    List<JsonNode> examples = new ArrayList<>();
    JsonNode exampleBase;
    JsonNode nameNode = node.getType() == OpenApi3Grammar.HEADER ? node.key() : node.get("name");
    boolean isDate;
    if (!schema.isMissing()) {
      examples.add(schema.get("example"));
      exampleBase = schema;
      isDate = verifyNameAndType(nameNode.stringValue(), schema);
    } else if (!content.isMissing()) {
      isDate = content.properties().stream().map(n -> n.get("schema")).anyMatch(s -> verifyNameAndType(nameNode.stringValue(), s));
      exampleBase = content.properties().stream().map(n -> n.get("schema")).findFirst().orElse(MissingNode.MISSING);
    } else {
      isDate = verifyNameAndType(nameNode.stringValue(), node);
      exampleBase = node;
    }
    if (isDate) {
      examples.add(node.get("example"));
      examples.addAll(node.get("examples").properties().stream().map(n -> n.resolve().get("value")).collect(Collectors.toList()));
      verifyExamples(examples, exampleBase);
    }
  }

  private void visitParameterV2(JsonNode node) {
    if (node.get("in").stringValue().equals("body")) {
      // V2 body parameters have a schema, and an example in Schema node
      visitParameterV3(node);
    } else {
      // V2 other parameters and headers mimick schema, but can't have an example
      JsonNode nameNode = node.getType() == OpenApi2Grammar.HEADER ? node.key() : node.get("name");
      verifyNameAndType(nameNode.stringValue(), node);
    }
  }

  private void visitSchema(JsonNode node) {
    // Note that some of the schema (of type string) will also be visited as part of Parameter validation
    // Here we're concerned about validating a schema's properties - hence of object type
    node = node.resolve();
    String type = node.get("type").stringValue();
    if (type.equals("object")) {
      for (Map.Entry<String, JsonNode> entry : node.get("properties").propertyMap().entrySet()) {
        JsonNode propertySchema = entry.getValue();
        boolean isDate = verifyNameAndType(entry.getKey(), propertySchema);
        if (isDate) {
          verifyExamples(Collections.singletonList(propertySchema.get("example")), propertySchema);
        }
      }
      if (!node.get("additionalProperties").isMissing()) {
        JsonNode propertySchema = node.get("additionalProperties");
        if (isDateType(propertySchema)) {
          verifyExamples(Collections.singletonList(propertySchema.get("example")), propertySchema);
        }
      }
    }
  }

  private boolean verifyNameAndType(String name, JsonNode node) {
    boolean isDateType = isDateType(node);
    boolean isDateName = isDateName(name);
    if (isDateName && !isDateType) {
      JsonNode errorNode = getErrorLocation(node);
      issues.put(errorNode, addIssue("Dates must use string/date-time or string/date format.", errorNode));
    }
    return isDateName || isDateType ;
  }

  private void verifyExamples(List<JsonNode> examples, JsonNode schemaNode) {
    boolean isPureDate = schemaNode.get("format").stringValue().equals("date");
    boolean hasExample = false;
    for (JsonNode example : examples) {
      boolean valid = isPureDate ? isIsoDate(example.stringValue()): isIso8601UTC(example.stringValue());
      hasExample |= !example.isMissing();
      if (!valid && !example.isMissing()) {
        addIssue("Define an ISO8601-compliant UTC date example.", example);
      }
    }
    if (!hasExample) {
      insertIssue(schemaNode, "Define an ISO8601-compliant UTC date example.");
    }
  }

  private void insertIssue(JsonNode schemaNode, String message) {
    JsonNode errorNode = getErrorLocation(schemaNode);
    PreciseIssue typeIssue = issues.get(errorNode);
    if (typeIssue != null) {
      typeIssue.secondary(errorNode, message);
    } else {
      issues.put(errorNode, addIssue(message, errorNode));
    }
  }

  private static JsonNode getErrorLocation(JsonNode schemaNode) {
    JsonNode errorNode = schemaNode.get("format");
    if (errorNode.isMissing()) {
      errorNode = schemaNode.get("type");
    }
    return errorNode;
  }

  @VisibleForTesting
  static boolean isIso8601UTC(String example) {
    return ISO_8601_PATTERN.matcher(example).matches();
  }

  private static boolean isIsoDate(String example) {
    return ISO_DATE_PATTERN.matcher(example).matches();
  }

  private static boolean isDateName(String name) {
    return name.toLowerCase().endsWith("date");
  }

  private static boolean isDateType(JsonNode node) {
    String type = node.get("type").stringValue();
    String format = node.get("format").stringValue();
    return type.equals("string") && (format.equals("date-time") || format.equals("date"));
  }
}
