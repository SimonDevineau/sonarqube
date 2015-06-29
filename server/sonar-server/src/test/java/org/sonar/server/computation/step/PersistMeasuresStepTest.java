/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.batch.protocol.Constants.MeasureValueType;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.issue.RuleCacheLoader;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureRepositoryImpl;
import org.sonar.server.computation.metric.MetricRepositoryImpl;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.metric.persistence.MetricDao;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.test.DbTests;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class PersistMeasuresStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String STRING_METRIC_KEY = "string-metric-key";
  private static final String DOUBLE_METRIC_KEY = "double-metric-key";
  private static final String OPTIMIZED_METRIC_KEY = "optimized-metric-key";
  private static final RuleKey RULE_KEY = RuleKey.of("repo", "rule-key");
  private static final int PROJECT_REF = 1;
  private static final int FILE_REF = 2;

  @ClassRule
  public static DbTester dbTester = new DbTester();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  DbClient dbClient;
  DbSession session;
  DbIdsRepository dbIdsRepository = new DbIdsRepository();
  MetricDto stringMetric;
  MetricDto doubleMetric;
  MetricDto optimizedMetric;
  RuleDto rule;
  ComponentDto projectDto;
  ComponentDto fileDto;

  PersistMeasuresStep sut;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new MeasureDao(), new ComponentDao(), new MetricDao(), new RuleDao(System2.INSTANCE));
    session = dbClient.openSession(false);

    stringMetric = new MetricDto().setValueType("STRING").setShortName("String metric").setKey(STRING_METRIC_KEY).setEnabled(true);
    dbClient.metricDao().insert(session, stringMetric);
    doubleMetric = new MetricDto().setValueType("FLOAT").setShortName("Double metric").setKey(DOUBLE_METRIC_KEY).setEnabled(true);
    dbClient.metricDao().insert(session, doubleMetric);
    optimizedMetric = new MetricDto().setValueType("BOOL").setShortName("Optimized metric").setKey(OPTIMIZED_METRIC_KEY).setEnabled(true).setOptimizedBestValue(true)
      .setBestValue(1d);
    dbClient.metricDao().insert(session, optimizedMetric);
    rule = RuleTesting.newDto(RULE_KEY);
    dbClient.ruleDao().insert(session, rule);
    session.commit();

    MetricRepositoryImpl metricRepository = new MetricRepositoryImpl(dbClient);
    metricRepository.start();
    MeasureRepository measureRepository = new MeasureRepositoryImpl(dbClient, reportReader, metricRepository);
    session.commit();

    sut = new PersistMeasuresStep(dbClient, metricRepository, dbIdsRepository, treeRootHolder, measureRepository);

    projectDto = addComponent("project-key");
    fileDto = addComponent("file-key");

    Component file = DumbComponent.builder(Component.Type.FILE, FILE_REF).setUuid("CDEF").setKey("MODULE_KEY:file").build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, PROJECT_REF).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(file).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setSnapshotId(project, 3L);
    dbIdsRepository.setComponentId(file, fileDto.getId());
    dbIdsRepository.setSnapshotId(file, 4L);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void insert_measures_from_report() throws Exception {
    reportReader.putMeasures(PROJECT_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setStringValue("measure-data")
        .setVariationValue1(1.1d)
        .setVariationValue2(2.2d)
        .setVariationValue3(3.3d)
        .setVariationValue4(4.4d)
        .setVariationValue5(5.5d)
        .setDescription("measure-description")
        .setMetricKey(STRING_METRIC_KEY)
        .build()));

    reportReader.putMeasures(FILE_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.DOUBLE)
        .setDoubleValue(123.123d)
        .setVariationValue1(1.1d)
        .setVariationValue2(2.2d)
        .setVariationValue3(3.3d)
        .setVariationValue4(4.4d)
        .setVariationValue5(5.5d)
        .setDescription("measure-description")
        .setMetricKey(DOUBLE_METRIC_KEY)
        .build()));

    sut.execute();
    session.commit();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(FILE_REF);

    List<Map<String, Object>> dtos = retrieveDtos();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(3L);
    assertThat(dto.get("componentId")).isEqualTo(projectDto.getId());
    assertThat(dto.get("metricId")).isEqualTo(stringMetric.getId().longValue());
    assertThat(dto.get("ruleId")).isNull();
    assertThat(dto.get("textValue")).isEqualTo("measure-data");
    assertThat(dto.get("severity")).isNull();

    dto = dtos.get(PROJECT_REF);
    assertThat(dto.get("snapshotId")).isEqualTo(4L);
    assertThat(dto.get("componentId")).isEqualTo(fileDto.getId());
    assertThat(dto.get("metricId")).isEqualTo(doubleMetric.getId().longValue());
    assertThat(dto.get("value")).isEqualTo(123.123d);
    assertThat(dto.get("severity")).isNull();
  }

  private List<Map<String, Object>> retrieveDtos() {
    return dbTester.select(
      "select snapshot_id as \"snapshotId\", project_id as \"componentId\", metric_id as \"metricId\", rule_id as \"ruleId\", value as \"value\", text_value as \"textValue\", " +
        "rule_priority as \"severity\" from project_measures");
  }

  @Test
  public void bestValue_measure_of_bestValueOptimized_metrics_are_not_persisted() {
    reportReader.putMeasures(FILE_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.BOOLEAN)
        .setBooleanValue(true)
        .setMetricKey(OPTIMIZED_METRIC_KEY)
        .build()));

    sut.execute();
    session.commit();

    assertThat(retrieveDtos()).isEmpty();
  }

  @Test
  public void empty_values_are_not_persisted() {
    reportReader.putMeasures(FILE_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setMetricKey(STRING_METRIC_KEY)
        .build(),
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.DOUBLE)
        .setMetricKey(DOUBLE_METRIC_KEY)
        .build()
      ));

    sut.execute();
    session.commit();

    assertThat(retrieveDtos()).isEmpty();
  }

  private ComponentDto addComponent(String key) {
    ComponentDto componentDto = new ComponentDto().setKey(key).setUuid(Uuids.create());
    dbClient.componentDao().insert(session, componentDto);
    session.commit();
    return componentDto;
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}
