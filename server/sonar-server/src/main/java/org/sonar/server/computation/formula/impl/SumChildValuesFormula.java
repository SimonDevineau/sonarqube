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
package org.sonar.server.computation.formula.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaContext;
import org.sonar.server.computation.formula.FormulaData;
import org.sonar.server.computation.formula.MetricValue;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

/**
 * A Formula which creates a new MetricValue on the current component which is the sum of the same metric on all its
 * children.
 * <p>
 * This implementation offers the possibility to choose which value should be saved for the current component if
 * none of its children has a value for the metric (or of component has no child): either 0 or no value at all.
 * </p>
 */
public class SumChildValuesFormula implements Formula {

  private boolean saveZeroIfNoChildValues;

  public SumChildValuesFormula(boolean saveZeroIfNoChildValues) {
    this.saveZeroIfNoChildValues = saveZeroIfNoChildValues;
  }

  @Override
  public List<Metric<?>> dependsUponMetrics() {
    return Collections.emptyList();
  }

  @Override
  public Optional<MetricValue> compute(FormulaData data, FormulaContext context) {
    final Metric<?> targetMetric = context.getTargetMetric();
    Double sum = sum(
      saveZeroIfNoChildValues,
      filter(
        transform(data.getChildren(), compose(OptionalToMetricValue.INSTANCE, new FormulaDataToOptionalMetricValue(targetMetric))),
        notNull()
      ));
    if (sum == null) {
      return Optional.absent();
    }
    return Optional.of(context.metricValue(sum));
  }

  /**
   * Sums a series of measures
   *
   * @param zeroIfNone whether to return 0 or {@code null} in case measures is {@code null}
   * @param measures   the series of measures or {@code null}
   * @return the sum of the measure series
   */
  public static Double sum(boolean zeroIfNone, @Nullable Iterable<MetricValue> measures) {
    if (measures == null) {
      return zeroIfNone(zeroIfNone);
    }

    double sum = 0d;
    boolean hasValue = false;
    for (MetricValue measure : filter(measures, notNull())) {
      LenientMetricValue metricValue = new LenientMetricValue(measure);
      if (metricValue.hasDoubleValue()) {
        hasValue = true;
        sum += metricValue.getDoubleValue();
      }
    }

    if (hasValue) {
      return sum;
    }
    return zeroIfNone(zeroIfNone);
  }

  private static Double zeroIfNone(boolean zeroIfNone) {
    return zeroIfNone ? 0d : null;
  }

  private static class FormulaDataToOptionalMetricValue implements Function<FormulaData, Optional<MetricValue>> {
    private final Metric<?> targetMetric;

    public FormulaDataToOptionalMetricValue(Metric<?> targetMetric) {
      this.targetMetric = targetMetric;
    }

    @Override
    public Optional<MetricValue> apply(@Nonnull FormulaData input) {
      return input.getMeasure(targetMetric);
    }
  }

  private enum OptionalToMetricValue implements Function<Optional<MetricValue>, MetricValue> {
    INSTANCE;

    @Override
    @Nullable
    public MetricValue apply(@Nonnull Optional<MetricValue> input) {
      if (input.isPresent()) {
        return input.get();
      }
      return null;
    }
  }
}
