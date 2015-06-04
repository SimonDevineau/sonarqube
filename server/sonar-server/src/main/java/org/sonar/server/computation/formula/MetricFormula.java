package org.sonar.server.computation.formula;

import org.sonar.api.measures.Metric;
import org.sonar.server.computation.formula.Formula;

public interface MetricFormula {
  Formula getFormula();

  Metric<?> getMetric();
}
