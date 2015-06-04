package org.sonar.server.computation.formula.impl;

import org.sonar.server.computation.formula.MetricValue;

public class LenientMetricValue implements MetricValue {
  private final MetricValue delegate;

  public LenientMetricValue(MetricValue delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean hasNoValue() {
    return delegate.hasNoValue();
  }

  @Override
  public boolean hasBooleanValue() {
    return delegate.hasBooleanValue();
  }

  @Override
  public boolean hasIntValue() {
    return delegate.hasIntValue();
  }

  @Override
  public boolean hasLongValue() {
    return delegate.hasIntValue() || delegate.hasLongValue();
  }

  @Override
  public boolean hasDoubleValue() {
    return hasLongValue() || delegate.hasDoubleValue();
  }

  @Override
  public boolean getBooleanValue() {
    return delegate.getBooleanValue();
  }

  @Override
  public int getIntValue() {
    return delegate.getIntValue();
  }

  @Override
  public long getLongValue() {
    if (delegate.hasIntValue()) {
      return delegate.getIntValue();
    }
    return delegate.getLongValue();
  }

  @Override
  public double getDoubleValue() {
    if (delegate.hasIntValue()) {
      return delegate.getIntValue();
    }
    if (delegate.hasLongValue()) {
      return delegate.getLongValue();
    }
    return delegate.getDoubleValue();
  }

  @Override
  public String getStringValue() {
    return delegate.getStringValue();
  }
}
