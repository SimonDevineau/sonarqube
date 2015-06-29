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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.formula.LineFormula;
import org.sonar.server.computation.measure.MeasureRepoEntry;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

public class ComputeFormulaMeasuresStepTest {

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(CoreMetrics.LINES);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);


  ComputeFormulaMeasuresStep sut;

  @Before
  public void setUp() throws Exception {
    sut = new ComputeFormulaMeasuresStep(treeRootHolder, measureRepository, metricRepository, new LineFormula());
  }

  @Test
  public void add_measures() throws Exception {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 111)
              .addChildren(
                builder(Component.Type.FILE, 1111).build(),
                builder(Component.Type.FILE, 1112).build()
              ).build()
          ).build(),
        DumbComponent.builder(MODULE, 12)
          .addChildren(
            DumbComponent.builder(DIRECTORY, 121)
              .addChildren(
                builder(Component.Type.FILE, 1211).build()
              ).build()
          ).build()
      ).build();

    treeRootHolder.setRoot(project);

    measureRepository.addRawMeasure(1111, CoreMetrics.LINES_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(1112, CoreMetrics.LINES_KEY, newMeasureBuilder().create(8));
    measureRepository.addRawMeasure(1211, CoreMetrics.LINES_KEY, newMeasureBuilder().create(2));

    sut.execute();

    assertThat(toEntries(measureRepository.getNewRawMeasures(1))).containsOnly(MeasureRepoEntry.entryOf(CoreMetrics.LINES_KEY, newMeasureBuilder().create(20)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(11))).containsOnly(MeasureRepoEntry.entryOf(CoreMetrics.LINES_KEY, newMeasureBuilder().create(18)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(111))).containsOnly(MeasureRepoEntry.entryOf(CoreMetrics.LINES_KEY, newMeasureBuilder().create(18)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(12))).containsOnly(MeasureRepoEntry.entryOf(CoreMetrics.LINES_KEY, newMeasureBuilder().create(2)));
    assertThat(toEntries(measureRepository.getNewRawMeasures(121))).containsOnly(MeasureRepoEntry.entryOf(CoreMetrics.LINES_KEY, newMeasureBuilder().create(2)));
  }
}
