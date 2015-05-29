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
package org.sonar.server.computation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportDto.Status;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.db.DbClient;
import org.sonar.server.properties.ProjectSettingsFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComputationServiceTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Captor
  ArgumentCaptor<Activity> activityArgumentCaptor;

  ComputationStep projectStep1 = mockStep();
  ComputationStep projectStep2 = mockStep();
  ComputationSteps steps = mock(ComputationSteps.class);
  ActivityService activityService = mock(ActivityService.class);
  System2 system = mock(System2.class);
  ComputationService sut;
  ProjectSettingsFactory settingsFactory = mock(ProjectSettingsFactory.class, Mockito.RETURNS_DEEP_STUBS);

  @Before
  public void setUp() {
    dbTester.truncateTables();
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao(), new SnapshotDao(system));
    sut = new ComputationService(dbClient, steps, activityService, tempFolder, system, mock(LanguageRepository.class));
  }

  @Test
  public void process_new_project() throws Exception {
    logTester.setLevel(LoggerLevel.INFO);

    when(steps.orderedSteps()).thenReturn(Arrays.asList(projectStep1, projectStep2));
    AnalysisReportDto dto = newDefaultReport();
    File zip = generateZip();

    sut.process(new ReportQueue.Item(dto, zip));

    // report is integrated -> status is set to SUCCESS
    assertThat(dto.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(dto.getFinishedAt()).isNotNull();

    // one info log at the end
    assertThat(logTester.logs(LoggerLevel.INFO)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.INFO).get(0)).startsWith("Analysis of project P1 (report 1) (done) | time=");

    // execute only the steps supporting the project qualifier
    verify(projectStep1).execute(any(ComputationContext.class));
    verify(projectStep2).execute(any(ComputationContext.class));
    verify(activityService).save(activityArgumentCaptor.capture());

    assertThat(activityArgumentCaptor.getValue().getType()).isEqualTo(Activity.Type.ANALYSIS_REPORT);
    assertThat(activityArgumentCaptor.getValue().getAction()).isEqualTo("LOG_ANALYSIS_REPORT");
    assertThat(activityArgumentCaptor.getValue().getData()).containsEntry("projectKey", "P1");
  }

  @Test
  public void process_existing_project() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    logTester.setLevel(LoggerLevel.INFO);

    when(steps.orderedSteps()).thenReturn(Arrays.asList(projectStep1, projectStep2));
    AnalysisReportDto dto = newDefaultReport();
    File zip = generateZip();

    sut.process(new ReportQueue.Item(dto, zip));

    // report is integrated -> status is set to SUCCESS
    assertThat(dto.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(dto.getFinishedAt()).isNotNull();

    // one info log at the end
    assertThat(logTester.logs(LoggerLevel.INFO)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.INFO).get(0)).startsWith("Analysis of project P1 (report 1) (done) | time=");

    // execute only the steps supporting the project qualifier
    verify(projectStep1).execute(any(ComputationContext.class));
    verify(projectStep2).execute(any(ComputationContext.class));
    verify(activityService).save(activityArgumentCaptor.capture());

    assertThat(activityArgumentCaptor.getValue().getType()).isEqualTo(Activity.Type.ANALYSIS_REPORT);
    assertThat(activityArgumentCaptor.getValue().getAction()).isEqualTo("LOG_ANALYSIS_REPORT");
    assertThat(activityArgumentCaptor.getValue().getData()).containsEntry("projectKey", "P1");
    assertThat(activityArgumentCaptor.getValue().getData()).containsEntry("projectName", "Project 1");
    assertThat(activityArgumentCaptor.getValue().getData().get("projectUuid")).isEqualTo("ABCD");
  }

  private AnalysisReportDto newDefaultReport() {
    return AnalysisReportDto.newForTests(1L).setProjectKey("P1").setUuid("U1").setStatus(Status.PENDING);
  }

  @Test
  public void debug_logs() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);

    AnalysisReportDto dto = newDefaultReport();
    File zip = generateZip();
    sut.process(new ReportQueue.Item(dto, zip));

    assertThat(logTester.logs(LoggerLevel.DEBUG)).isNotEmpty();
  }

  @Test
  public void fail_if_corrupted_zip() throws Exception {
    AnalysisReportDto dto = newDefaultReport();
    File zip = tempFolder.newFile();
    FileUtils.write(zip, "not a file");

    try {
      sut.process(new ReportQueue.Item(dto, zip));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Fail to unzip " + zip.getAbsolutePath() + " into ");
      assertThat(dto.getStatus()).isEqualTo(Status.FAILED);
      assertThat(dto.getFinishedAt()).isNotNull();
    }
  }

  @Test
  public void step_error() throws Exception {
    when(steps.orderedSteps()).thenReturn(Arrays.asList(projectStep1));
    doThrow(new IllegalStateException("pb")).when(projectStep1).execute(any(ComputationContext.class));

    AnalysisReportDto dto = newDefaultReport();
    File zip = generateZip();

    try {
      sut.process(new ReportQueue.Item(dto, zip));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("pb");
      assertThat(dto.getStatus()).isEqualTo(Status.FAILED);
      assertThat(dto.getFinishedAt()).isNotNull();
    }
  }

  private ComputationStep mockStep() {
    ComputationStep step = mock(ComputationStep.class);
    when(step.getDescription()).thenReturn(RandomStringUtils.randomAscii(5));
    return step;
  }

  private File generateZip() throws IOException {
    return generateZip(110L);
  }

  private File generateZip(long snapshotId) throws IOException {
    File dir = tempFolder.newDir();
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .setSnapshotId(snapshotId)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey("PROJECT_KEY")
      .setSnapshotId(snapshotId)
      .build());
    File zip = tempFolder.newFile();
    ZipUtils.zipDir(dir, zip);
    return zip;
  }
}
