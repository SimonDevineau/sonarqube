package org.sonar.server.computation.formula;

public interface MetricValue {

  boolean hasBooleanValue();

  boolean hasIntValue();

  boolean hasLongValue();

  boolean hasDoubleValue();

  boolean hasNoValue();

  boolean getBooleanValue();

  int getIntValue();

  long getLongValue();

  double getDoubleValue();

  String getStringValue();

}
