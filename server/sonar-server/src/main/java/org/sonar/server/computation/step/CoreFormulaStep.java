package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.FormulaContext;
import org.sonar.server.computation.formula.FormulaData;
import org.sonar.server.computation.formula.MetricFormula;
import org.sonar.server.computation.formula.MetricValue;
import org.sonar.server.computation.formula.impl.AbstractFormulaContext;
import org.sonar.server.computation.measure.MeasureRepository;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor.Order.POST_ORDER;

public class CoreFormulaStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final Collection<MetricFormula> metricFormulas;

  public CoreFormulaStep(TreeRootHolder treeRootHolder, MeasureRepository measureRepository, @Nullable MetricFormula... metricFormulas) {
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.metricFormulas = metricFormulas == null ? Collections.<MetricFormula>emptyList() : copyOf(metricFormulas);
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareVisitor(FILE, POST_ORDER) {
      @Override
      public void visitProject(org.sonar.server.computation.component.Component project) {
        executeFormulas(project);
      }

      @Override
      public void visitModule(org.sonar.server.computation.component.Component module) {
        executeFormulas(module);
      }

      @Override
      public void visitDirectory(org.sonar.server.computation.component.Component directory) {
        executeFormulas(directory);
      }

      @Override
      public void visitFile(org.sonar.server.computation.component.Component file) {
        executeFormulas(file);
      }
    }.visit(treeRootHolder.getRoot());
  }

  private void executeFormulas(final Component component) {
    for (final MetricFormula metricFormula : metricFormulas) {
      FormulaData formulaData = new FormulaData() {
        @Override
        public Optional<MetricValue> getMeasure(final Metric<?> metric) {
          return measureRepository.findCurrent(component, metric);
        }

        @Override
        public Collection<FormulaData> getChildren() {
          return newArrayList(
          transform(
            component.getChildren(),
            new Function<Component, FormulaData>() {
              @Override
              public FormulaData apply(@Nonnull final Component child) {
                return new FormulaData() {
                  @Override
                  public Optional<MetricValue> getMeasure(final Metric<?> metric) {
                    return measureRepository.findCurrent(child, metric);
                  }

                  @Override
                  public Collection<FormulaData> getChildren() {
                    throw new UnsupportedOperationException("Accessing FormulaData of children of children is illegal");
                  }
                };
              }
            }
          )
          );
        }
      };
      FormulaContext formulaContext = new AbstractFormulaContext() {
        @Override
        public Metric<?> getTargetMetric() {
          return metricFormula.getMetric();
        }

        @Override
        public Component getComponent() {
          return component;
        }
      };
      Optional<MetricValue> metricValue = metricFormula.getFormula().compute(formulaData, formulaContext);
      if (metricValue.isPresent()) {
        measureRepository.add(component, metricFormula.getMetric(), metricValue.get());
      }

    }
  }

  @Override
  public String getDescription() {
    return "Measure formulas";
  }
}
