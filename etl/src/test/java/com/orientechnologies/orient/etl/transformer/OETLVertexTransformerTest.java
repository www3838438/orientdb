/*
 *
 *  * Copyright 2010-2016 OrientDB LTD (info(-at-)orientdb.com)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.orientechnologies.orient.etl.transformer;

import com.orientechnologies.orient.etl.OETLBaseTest;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.Vertex;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Tests ETL Vertex Transformer.
 *
 * @author Gregor Frey
 */
public class OETLVertexTransformerTest extends OETLBaseTest {

  @Before
  public void setUp() {
    graph.createVertexType("Person");
    graph.createKeyIndex("name", Vertex.class, new Parameter<String, String>("type", "UNIQUE"),
        new Parameter<String, String>("class", "Person"));
    graph.commit();
  }

  @Test
  public void testCreateVertex() {
    process("{source: { content: { value: 'name,\nGregor' } }, extractor : { csv: {} },"
        + " transformers: [{vertex: {class:'Person', skipDuplicates:false}},"
        + "], loader: { orientdb: { dbAutoCreateProperties:true, cluster: 'custom', dbURL: 'memory:" + name.getMethodName()
        + "', dbType:'graph', useLightweightEdges:false } } }");

    assertEquals("V", 1, graph.countVertices());
    assertEquals("person", 1, graph.countVertices("Person"));

    assertThat(graph.getRawGraph().countClusterElements("custom")).isEqualTo(1);

  }

  @Test
  public void testCreateTargetVertexIfNotExists() {
    process("{source: { content: { value: 'name,idf,parent\nParent,1,\nChild,2,1' } }, extractor : { csv: {} },"
        + " transformers: [{merge: { joinFieldName:'idf', lookup:'V.idf'}}, {vertex: {class:'V'}},"
        + "{edge:{ class: 'E', joinFieldName: 'parent', lookup: 'V.idf', unresolvedLinkAction: 'CREATE' }, if: '$input.parent IS NOT NULL'}"
        + "], loader: { orientdb: { dbURL: 'memory:" + name.getMethodName() + "', dbType:'graph', useLightweightEdges:false } } }");

    graph.makeActive();
    ;
    assertThat(graph.countVertices("V")).isEqualTo(2);
  }

  //  @Test(expected = ORecordDuplicatedException.class)
  @Test
  public void testErrorOnDuplicateVertex() {
    process("{ config: { 'log': 'DEBUG' },  source: { content: { value: 'name,\nGregor\nGregor\nHans' } }, extractor : { csv: {} },"
        + " transformers: [ {vertex: {class:'Person', skipDuplicates:false}}," + "], loader: { orientdb: { dbURL: 'memory:" + name
        .getMethodName() + "', dbType:'graph', useLightweightEdges:false } } }");

    assertThat(graph.countVertices("V")).isEqualTo(1);
  }

  @Test
  public void testSkipDuplicateVertex() {
    process("{source: { content: { value: 'name,\nGregor\nGregor\nHans' } }, extractor : { csv: {} },"
        + " transformers: [{vertex: {class:'Person', skipDuplicates:true}},]," + " loader: { orientdb: {  dbURL: 'memory:" + name
        .getMethodName() + "', dbType:'graph', useLightweightEdges:false } } }");

    assertThat(graph.countVertices("Person")).isEqualTo(2);
  }
}