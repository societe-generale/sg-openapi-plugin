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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.impl.MissingNode;
import org.sonar.sslr.yaml.grammar.JsonNode;

import static org.sonar.sslr.yaml.grammar.Utils.escapeJsonPointer;

@Rule(key = MandatoryHealthEndpointCheck.CHECK_KEY)
public class MandatoryHealthEndpointCheck extends OpenApiCheck {
  protected static final String CHECK_KEY = "MandatoryHealthEndpoint";

  private static final String MISSING_HEALTH_ENDPOINT_MESSAGE = "This API must define a /health path";
  private static final String MISSING_HEALTH_GET_OPERATION_MESSAGE = "The API must have a GET operation on /health path";
  private static final String MISSING_HEALTH_OK_RESPONSE_MESSAGE = "The operation GET /health must have a 200 OK response code";
  private static final String MISSING_HEALTH_GET_CONTENT_TYPE_RESPONSE_MESSAGE = "The operation GET /health must return an 'application/json' document";
  private static final String MISSING_HEALTH_MANDATORY_ARGUMENT_RESPONSE_MESSAGE = "The operation GET /health schema must contain string attribute: ";
  private static final String BAD_HEALTH_STATUS_ENUMERATION = "The status argument on the response of operation GET /health must be either UP|DOWN|DEGRADED";
  private static final String HEALTH_PATH = "/health";
  private static final String HEALTH_METHOD = "get";
  private static final String HEALTH_RESPONSE_CONTENT_TYPE = "application/json";

  private boolean isOpenAPIv3 = false;
  private boolean hasDefaultJson;


  @Override
  protected void visitFile(JsonNode root) {
    isOpenAPIv3 = (root.getType() == OpenApi3Grammar.ROOT);
    JsonNode paths = root.at("/paths");
    this.hasDefaultJson = hasDefaultJson(root);
    JsonNode healthPath = root.at("/paths/" + escapeJsonPointer(HEALTH_PATH));
    if (!healthPath.isMissing()){
      verifyHealthEndpoint(healthPath);
    } else {
      addIssue(MISSING_HEALTH_ENDPOINT_MESSAGE, paths.isMissing() ? root : paths.key());
    }
  }

  private boolean hasDefaultJson(JsonNode root) {
    return root.at("/produces").elements().stream().anyMatch(n -> n.getTokenValue().equals("application/json"));
  }

  private void verifyHealthEndpoint(JsonNode path){
    JsonNode operation = path.value().at("/" + escapeJsonPointer(HEALTH_METHOD));
    if (!operation.isMissing()){
      verifyResponseContentType(operation);
      verifyResponseSchema(operation);
    } else {
      addIssue(MISSING_HEALTH_GET_OPERATION_MESSAGE,path.key());
    }
  }

  private void verifyResponseSchema(JsonNode operation){
    Schema schema = null;
    if (isOpenAPIv3) {
      String jsonPointer = "/responses/200/content/" +
          escapeJsonPointer(HEALTH_RESPONSE_CONTENT_TYPE) +
          "/schema";
      JsonNode s = operation.value().at(jsonPointer);
      if (!s.isMissing() && s.key().getTokenValue().equals("schema")){
        schema = SchemaBuilder.getSchemaInstance(s.value(), Schemav3.class);
      }
      // TBD
    } else {
      schema = SchemaBuilder.getSchemaInstance(operation.value().at("/responses/200/schema"), Schemav2.class);
    }
    if (schema != null) {
      new HealthSchemaVerifier().verify(schema);
    }
  }

  private void verifyResponseContentType(JsonNode operation){
    boolean isContentType = false;
    JsonNode produces;
    if (isOpenAPIv3){
      produces = operation.value().at("/responses/200/content");
      if (produces == MissingNode.MISSING){
        addIssue(MISSING_HEALTH_OK_RESPONSE_MESSAGE, operation.key());
        return;
      }else {
        isContentType = produces.propertyMap().containsKey(HEALTH_RESPONSE_CONTENT_TYPE);
      }
     // OpenAPI v2
    }else{
      produces = operation.value().at("/produces");
      if (produces == MissingNode.MISSING && !hasDefaultJson){
        addIssue(MISSING_HEALTH_GET_CONTENT_TYPE_RESPONSE_MESSAGE, operation.key());
        return;
      }else{
        for (JsonNode contentType : produces.elements()){
          isContentType = (isContentType || HEALTH_RESPONSE_CONTENT_TYPE.equals(contentType.getTokenValue()));
        }
      }
    }
    if (!isContentType && !hasDefaultJson){
      addIssue(MISSING_HEALTH_GET_CONTENT_TYPE_RESPONSE_MESSAGE, produces.key());
    }
  }

  /**
   * Definition of /Health Json response structure
   */
  private class HealthSchemaVerifier {

    private List<String> statusEnum = new ArrayList<>();

    public HealthSchemaVerifier(){
      statusEnum.add("UP");
      statusEnum.add("DOWN");
      statusEnum.add("DEGRADED");
    }

    public void verify(Schema schema){

      verifyParameter(schema, "version");
      verifyParameter(schema, "status");
      verifyStatusEnumeration(schema, statusEnum);
      verifyParameter(schema, "comment");
      if (schema.getProperties().containsKey("modules")) {
        verifyModule(schema.getProperties().get("modules"));
      }
      if (schema.getProperties().containsKey("az")) {
        verifyAZ(schema.getProperties().get("az"));
      }
    }

    private void verifyModule(Schema schema){
      if (schema.getItems() != null) {
        Schema itemSchema = schema.getItems();
        verifyParameter(itemSchema, "status");
        verifyStatusEnumeration(schema, statusEnum);
        verifyParameter(itemSchema, "comment");
        verifyParameter(itemSchema, "name");
        if (itemSchema.getProperties().containsKey("az")) {
          verifyAZ(itemSchema.getProperties().get("az"));
        }
      }
    }

    private void verifyAZ(Schema schema){
      if (schema.getItems() != null) {
        Schema itemSchema = schema.getItems();
        verifyParameter(itemSchema, "status");
        verifyStatusEnumeration(schema, statusEnum);
        verifyParameter(itemSchema, "comment");
        verifyParameter(itemSchema, "name");
      }
    }

    private void verifyParameter(Schema schema, String key){
      Schema parameterSchema = schema.getProperties().get(key);
      if (parameterSchema == null || !parameterSchema.getType().equals("string")) {
        addIssue(MISSING_HEALTH_MANDATORY_ARGUMENT_RESPONSE_MESSAGE + key, schema.getNode().key());
      }
    }

    private void verifyStatusEnumeration(Schema schema, List<String> expectedEnumeration){
      boolean valid;
      if (schema.getProperties().containsKey("status")){
        Schema enumSchema = schema.getProperties().get("status");
        valid = enumSchema.getEnumeration().containsAll(expectedEnumeration) && expectedEnumeration.containsAll(enumSchema.getEnumeration());
        if (!valid){
          addIssue(BAD_HEALTH_STATUS_ENUMERATION, enumSchema.getNode().key());
        }
      }
    }

  }

  /**
   * Partial implementation of Schema object in OpenAPIv2 and v3
   */
  private interface Schema{
    Map<String,Schema> getProperties();
    List<String> getEnumeration();
    JsonNode getNode();
    Schema getItems();

    String getType();
  }

  private static class SchemaBuilder{
    public static Schema getSchemaInstance(JsonNode schema, Class<? extends Schema> schemaClass){
      if (schema.isMissing()) {
        return null;
      }
      JsonNode target = schema;
      if (schema.isRef()){
        target = schema.resolve();
      }
      if (schemaClass.equals(Schemav2.class)){
        return new Schemav2(target);
      }else{
        return new Schemav3(target);
      }
    }
  }

  private static class Schemav2 extends Schemav3{

    public Schemav2(JsonNode schema){
      super(schema);
    }

  }

  private static class Schemav3 implements Schema{
    private List<String> enumeration;
    private Map<String,Schema> properties;
    private Schema items;
    private JsonNode node;
    private String type;

    public Schemav3(JsonNode schema){
      JsonNode schemaValue = schema.value();
      enumeration = getListFromElements(schemaValue.at("/enum"));
      node = schema;
      properties = schemaValue.at("/properties").propertyMap(
          n -> SchemaBuilder.getSchemaInstance(n, this.getClass()));
      items = SchemaBuilder.getSchemaInstance(schemaValue.at("/items"), this.getClass());
      JsonNode typeNode = schemaValue.at("/type");
      type = typeNode.isMissing() ? null : typeNode.getTokenValue();
    }

    private List<String> getListFromElements(JsonNode node){
      List<String> list = new ArrayList<>();
      for(JsonNode n : node.elements()){
        list.add(n.getTokenValue());
      }
      return list;
    }

    @Override
    public List<String> getEnumeration() {
      return enumeration;
    }

    @Override
    public Map<String, Schema> getProperties() {
      return properties;
    }

    @Override
    public Schema getItems() {
      return items;
    }

    @Override
    public JsonNode getNode() {
      return node;
    }

    @Override
    public String getType() {
      return type;
    }
  }
}
