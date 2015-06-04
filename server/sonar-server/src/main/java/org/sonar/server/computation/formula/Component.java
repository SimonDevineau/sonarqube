package org.sonar.server.computation.formula;

import com.google.common.base.Optional;
import org.sonar.api.measures.Metric;

public interface Component {
  enum ComponentType {
    PROJECT, MODULE, DIRECTORY, FILE
  }

  ComponentType getType();

  Optional<MetricValue> getValue(Metric<?> metric);
}
