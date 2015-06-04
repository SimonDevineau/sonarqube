package org.sonar.server.computation.formula.impl;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.formula.MetricValue;

public class BatchReportMetricValue {

  public static Optional<MetricValue> create(@Nullable BatchReport.Measure measure) {
    if (measure == null) {
      return Optional.absent();
    }
    if (measure.hasIntValue()) {
      return Optional.of(MetricValueImpl.create(measure.getIntValue()));
    }
    if (measure.hasLongValue()) {
      return Optional.of(MetricValueImpl.create(measure.getLongValue()));
    }
    if (measure.hasDoubleValue()) {
      return Optional.of(MetricValueImpl.create(measure.getDoubleValue()));
    }
    if (measure.hasBooleanValue()) {
      return Optional.of(MetricValueImpl.create(measure.getBooleanValue()));
    }
    if (measure.hasStringValue()) {
      return Optional.of(MetricValueImpl.create(measure.getStringValue()));
    }
    return Optional.absent();
  }

}
