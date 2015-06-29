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

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.PathAwareVisitor;
import org.sonar.server.computation.component.PathAwareVisitor.SimpleStackElementFactory;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.Counter;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaContext;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

public class ComputeFormulaMeasuresStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;
  private final Formula[] formulas;

  public ComputeFormulaMeasuresStep(TreeRootHolder treeRootHolder, MeasureRepository measureRepository, MetricRepository metricRepository, Formula... formulas) {
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
    this.formulas = formulas;
  }

  @Override
  public void execute() {
    new PathAwareVisitor<Counters>(Component.Type.FILE, ComponentVisitor.Order.POST_ORDER, new SimpleStackElementFactory<Counters>() {
      @Override
      public Counters createForAny(Component component) {
        return new Counters();
      }
    }) {

      @Override
      protected void visitProject(Component project, Path<Counters> path) {
        processNotFile(project, path);
      }

      @Override
      protected void visitModule(Component module, Path<Counters> path) {
        processNotFile(module, path);
      }

      @Override
      protected void visitDirectory(Component directory, Path<Counters> path) {
        processNotFile(directory, path);
      }

      @Override
      protected void visitFile(Component file, Path<Counters> path) {
        processFile(file, path);
      }

    }.visit(treeRootHolder.getRoot());
  }

  private void processNotFile(Component component, PathAwareVisitor.Path<Counters> path) {
    for (Formula formula : formulas) {
      addNewMeasure(component, path, formula);
      aggregate(path, formula);
    }
  }

  private void addNewMeasure(Component component, PathAwareVisitor.Path<Counters> path, Formula formula) {
    Counter counter = path.current().getCounter(formula.getOutputMetricKey());
    Metric metric = metricRepository.getByKey(formula.getOutputMetricKey());
    Optional<Measure> measure = formula.createMeasure(counter);
    if (measure.isPresent()) {
      measureRepository.add(component, metric, measure.get());
    }
    // TODO add test if no measure
  }

  private void aggregate(PathAwareVisitor.Path<Counters> path, Formula formula) {
    if (!path.isRoot()) {
      Counter counter = path.current().getCounter(formula.getOutputMetricKey());
      path.parent().addCounter(formula.getOutputMetricKey(), counter);
    }
  }

  private void processFile(Component file, PathAwareVisitor.Path<Counters> path) {
    FormulaContext formulaContext = new FormulaContextImpl(file);
    for (Formula formula : formulas) {
      Counter counter = newCounter(formula);
      counter.aggregate(file, formulaContext);
      path.parent().addCounter(formula.getOutputMetricKey(), counter);
    }
  }

  private Counter newCounter(Formula formula) {
    Class counterType = formula.getCounterType();
    try {
      return (Counter) counterType.newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Cannot instantiate counter %s", counterType), e);
    }
  }

  @Override
  public String getDescription() {
    return "Compute formula measures";
  }

  private static class Counters {
    Map<String, Counter> countersByFormula = new HashMap<>();

    public void addCounter(String metricKey, Counter counter) {
      Counter existingCounter = countersByFormula.get(metricKey);
      if (existingCounter == null) {
        countersByFormula.put(metricKey, counter);
      } else {
        aggregate(existingCounter, counter);
      }
    }

    private void aggregate(Counter existingCounter, Counter counter){
      existingCounter.aggregate(counter);
    }

    public Counter getCounter(String metricKey) {
      return countersByFormula.get(metricKey);
    }
  }

  private class FormulaContextImpl implements FormulaContext {

    private final Component file;

    public FormulaContextImpl(Component file) {
      this.file = file;
    }

    @Override
    public Optional<Measure> getMeasure(String metricKey) {
      return measureRepository.getRawMeasure(file, metricRepository.getByKey(metricKey));
    }
  }
}
