package org.sonar.server.computation.formula.impl;

import org.sonar.server.computation.formula.FormulaContext;
import org.sonar.server.computation.formula.MetricValue;

public abstract class AbstractFormulaContext implements FormulaContext {

  @Override
  public MetricValue metricValue(boolean value) {
    return MetricValueImpl.create(value);
  }

  @Override
  public MetricValue metricValue(int value) {
    return MetricValueImpl.create(value);
  }

  @Override
  public MetricValue metricValue(long value) {
    return MetricValueImpl.create(value);
  }

  @Override
  public MetricValue metricValue(double value) {
    return MetricValueImpl.create(value);
  }

  @Override
  public MetricValue metricValue(String value) {
    return MetricValueImpl.create(value);
  }

  @Override
  public MetricValue noMetricValue() {
    return MetricValueImpl.noValue();
  }
}
