package org.sonar.server.computation.formula.impl;

import java.util.Locale;
import javax.annotation.Nullable;
import org.sonar.server.computation.formula.MetricValue;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MetricValueImpl implements MetricValue {
  private static final MetricValue NO_VALUE = new MetricValueImpl(ValueType.NO_VALUE, null, null);

  protected enum ValueType {
    NO_VALUE, BOOLEAN, INT, LONG, DOUBLE, STRING
  }

  private final ValueType valueType;
  @Nullable
  private final Double value;
  @Nullable
  private final String data;

  protected MetricValueImpl(ValueType valueType, @Nullable Double value, @Nullable String data) {
    this.valueType = valueType;
    this.value = value;
    this.data = data;
  }

  protected static MetricValue create(boolean value) {
    return new MetricValueImpl(ValueType.BOOLEAN, value ? 1.0d : 0.0d, null);
  }

  protected static MetricValue create(int value) {
    return new MetricValueImpl(ValueType.INT, (double) value, null);
  }

  protected static MetricValue create(long value) {
    return new MetricValueImpl(ValueType.LONG, (double) value, null);
  }

  protected static MetricValue create(double value) {
    return new MetricValueImpl(ValueType.DOUBLE, value, null);
  }

  protected static MetricValue create(String value) {
    return new MetricValueImpl(ValueType.STRING, null, checkNotNull(value));
  }

  public static MetricValue noValue() {
    return NO_VALUE;
  }

  @Override
  public boolean hasBooleanValue() {
    return valueType == ValueType.BOOLEAN;
  }

  @Override
  public boolean hasIntValue() {
    return valueType == ValueType.INT;
  }

  @Override
  public boolean hasLongValue() {
    return valueType == ValueType.LONG;
  }

  @Override
  public boolean hasDoubleValue() {
    return valueType == ValueType.DOUBLE;
  }

  @Override
  public boolean hasNoValue() {
    return valueType == ValueType.NO_VALUE;
  }

  @Override
  public boolean getBooleanValue() {
    checkValueType(ValueType.BOOLEAN);
    return value == 1.0d;
  }

  @Override
  public int getIntValue() {
    checkValueType(ValueType.INT);
    return value.intValue();
  }

  @Override
  public long getLongValue() {
    checkValueType(ValueType.LONG);
    return value.longValue();
  }

  @Override
  public double getDoubleValue() {
    checkValueType(ValueType.DOUBLE);
    return value;
  }

  @Override
  public String getStringValue() {
    checkValueType(ValueType.STRING);
    return data;
  }

  private void checkValueType(ValueType expected) {
    if (valueType != expected) {
      throw new IllegalStateException(
        String.format(
          "value can not be converted to %s because current value type is a %s",
          expected.toString().toLowerCase(Locale.US),
          valueType
          ));
    }
  }

}
