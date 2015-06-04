package org.sonar.server.computation.formula.impl;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.core.component.Module;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.MetricFormula;

public class MetricsFormulasModule extends Module {
  public static final SumChildValuesFormula SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE = new SumChildValuesFormula(false);

  @Override
  protected void configureModule() {
    add(
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.LINES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.GENERATED_LINES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NCLOC),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.GENERATED_NCLOC),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.CLASSES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.PACKAGES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.FUNCTIONS),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.ACCESSORS),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.STATEMENTS),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.PUBLIC_API),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.COMMENT_LINES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.PUBLIC_UNDOCUMENTED_API),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.COMPLEXITY),
      metricFormula(AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FILES), CoreMetrics.FILE_COMPLEXITY),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.COMPLEXITY_IN_CLASSES),
      metricFormula(
        AverageFormula.create(CoreMetrics.COMPLEXITY_IN_CLASSES, CoreMetrics.CLASSES).withFallback(CoreMetrics.COMPLEXITY),
        CoreMetrics.CLASS_COMPLEXITY),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.COMPLEXITY_IN_FUNCTIONS),
      metricFormula(
        AverageFormula.create(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, CoreMetrics.FUNCTIONS).withFallback(CoreMetrics.COMPLEXITY),
        CoreMetrics.FUNCTION_COMPLEXITY),
      // metricFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY), CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION),
      // metricFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY), CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION),
      // metricFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY), CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.LINES_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_LINES_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.UNCOVERED_LINES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_UNCOVERED_LINES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.CONDITIONS_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_CONDITIONS_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.UNCOVERED_CONDITIONS),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_UNCOVERED_CONDITIONS),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.IT_LINES_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_IT_LINES_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.IT_UNCOVERED_LINES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_IT_UNCOVERED_LINES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_IT_CONDITIONS_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.IT_UNCOVERED_CONDITIONS),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.OVERALL_LINES_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_OVERALL_LINES_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.OVERALL_UNCOVERED_LINES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_OVERALL_UNCOVERED_LINES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.OVERALL_CONDITIONS_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS),
      // metricFormula(new WeightedMeanAggregationFormula(CoreMetrics.FILES, false), CoreMetrics.RFC),
      // metricFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY), CoreMetrics.RFC_DISTRIBUTION),
      // metricFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY), CoreMetrics.LCOM4_DISTRIBUTION),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.DIRECTORY_CYCLES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.DIRECTORY_TANGLES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.DIRECTORY_FEEDBACK_EDGES),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.DIRECTORY_EDGES_WEIGHT),
      metricFormula(SUM_CHILD_VALUES_FORMULA_NO_ZERO_VALUE, CoreMetrics.DIRECTORY_EDGES_WEIGHT));
  }

  private static MetricFormulaImpl metricFormula(Formula formula, Metric<?> metric) {
    return new MetricFormulaImpl(formula, metric);
  }

  private static class MetricFormulaImpl implements MetricFormula {
    private final Formula formula;
    private final Metric<?> metric;

    public MetricFormulaImpl(Formula formula, Metric<?> metric) {
      this.formula = formula;
      this.metric = metric;
    }

    @Override
    public Formula getFormula() {
      return formula;
    }

    @Override
    public Metric<?> getMetric() {
      return metric;
    }
  }
}
