package org.sonar.server.computation.formula.impl;

import com.google.common.base.Optional;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaContext;
import org.sonar.server.computation.formula.FormulaData;
import org.sonar.server.computation.formula.MetricValue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

public class AverageFormula implements Formula {
  private final Metric<?> mainMetric;
  private final Metric<?> byMetric;
  @Nullable
  private final Metric<?> fallbackMetric;

  private AverageFormula(Metric<?> mainMetric, Metric<?> byMetric, @Nullable Metric<?> fallbackMetric) {
    this.mainMetric = checkNotNull(mainMetric);
    this.byMetric = checkNotNull(byMetric);
    this.fallbackMetric = fallbackMetric;
  }
  
  public static AverageFormula create(Metric<?> mainMetric, Metric<?> byMetric) {
    return new AverageFormula(mainMetric, byMetric, null);
  }
  
  public AverageFormula withFallback(Metric fallbackMetric) {
    return new AverageFormula(mainMetric, byMetric, checkNotNull(fallbackMetric));
  }

  @Override
  public List<Metric<?>> dependsUponMetrics() {
    return fallbackMetric == null ? newArrayList(mainMetric, byMetric) : newArrayList(mainMetric, fallbackMetric, byMetric);
  }

  @Override
  public Optional<MetricValue> compute(FormulaData data, FormulaContext context) {
    Optional<MetricValue> existingValue = data.getMeasure(context.getTargetMetric());
    if (existingValue.isPresent()) {
      return existingValue;
    }

    if (context.getComponent().getType() == Component.Type.FILE) {
      return calculateForFile(data, context);
    }
    return calculateOnChildren(data, context);
  }

  private Optional<MetricValue> calculateForFile(FormulaData data, FormulaContext context) {
    Double fallbackMeasure = getFallbackMetric(data);
    Double byMeasure = getLenientDoubleValue(data.getMeasure(byMetric), null);
    Double mainMeasure = getLenientDoubleValue(data.getMeasure(mainMetric), fallbackMeasure);
    if (mainMeasure != null && byMeasure != null && byMeasure > 0.0) {
      return Optional.of(context.metricValue(mainMeasure / byMeasure));
    }

    return Optional.absent();
  }

  private Optional<MetricValue> calculateOnChildren(FormulaData data, FormulaContext context) {
    double totalByMeasure = 0;
    double totalMainMeasure = 0;
    boolean hasApplicableChildren = false;

    for (FormulaData childData : data.getChildren()) {
      Double fallbackMeasure = getFallbackMetric(childData);
      Double childrenByMeasure = getLenientDoubleValue(childData.getMeasure(byMetric), null);
      Double childrenMainMeasure = getLenientDoubleValue(childData.getMeasure(mainMetric), fallbackMeasure);
      if (childrenMainMeasure != null && childrenByMeasure != null && childrenByMeasure > 0.0) {
        totalByMeasure += childrenByMeasure;
        totalMainMeasure += childrenMainMeasure;
        hasApplicableChildren = true;
      }
    }
    if (hasApplicableChildren) {
      return Optional.of(context.metricValue(totalMainMeasure / totalByMeasure));
    }

    return Optional.absent();
  }

  private Double getFallbackMetric(FormulaData data) {
    return fallbackMetric == null ? null : getLenientDoubleValue(data.getMeasure(fallbackMetric), null);
  }

  @CheckForNull
  private static Double getLenientDoubleValue(Optional<MetricValue> metricValue, @Nullable Double defaultValue) {
    if (!metricValue.isPresent()) {
      return defaultValue;
    }
    LenientMetricValue lenientMetricValue = new LenientMetricValue(metricValue.get());
    if (lenientMetricValue.hasDoubleValue()) {
      return lenientMetricValue.getDoubleValue();
    }
    return defaultValue;
  }
}
