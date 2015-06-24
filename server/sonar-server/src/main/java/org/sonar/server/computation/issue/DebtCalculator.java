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
package org.sonar.server.computation.issue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.debt.Characteristic;
import org.sonar.server.computation.debt.DebtModelHolder;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

import static com.google.common.collect.Maps.newHashMap;

public class DebtCalculator extends IssueListener {

  private final IssueUpdater updater;
  private final RuleCache ruleCache;
  private final DebtModelHolder debtModelHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  private final Map<Integer, Debt> debtsByComponentRef = new HashMap<>();
  private Debt currentDebt;

  public DebtCalculator(IssueUpdater updater, RuleCache ruleCache, DebtModelHolder debtModelHolder,
    MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.updater = updater;
    this.ruleCache = ruleCache;
    this.debtModelHolder = debtModelHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void beforeComponent(Component component, Tracking tracking) {
    this.currentDebt = new Debt();

    // aggregate children counters
    for (Component child : component.getChildren()) {
      // no need to keep the children in memory. They can be garbage-collected.
      Debt childDebt = debtsByComponentRef.remove(child.getRef());
      currentDebt.add(childDebt);
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.resolution() == null) {
      // TODO calculate debt according to rule remediation function. Currently done by batch.
      currentDebt.add(issue);
    }
  }

  @Override
  public void afterComponent(Component component) {
    if (this.currentDebt.minutes > 0L) {
      Metric metric = metricRepository.getByKey(CoreMetrics.TECHNICAL_DEBT_KEY);

      // total value
      measureRepository.add(component, metric, Measure.newMeasureBuilder().create(this.currentDebt.minutes));

      // distribution by rule
      for (Map.Entry<Integer, Long> entry : currentDebt.minutesByRuleId.entrySet()) {
        int ruleId = entry.getKey();
        long ruleDebt = entry.getValue();
        measureRepository.add(component, metric, Measure.newMeasureBuilder().forRule(ruleId).create(ruleDebt));
      }

      // distribution by characteristic
      for (Map.Entry<Integer, Long> entry : currentDebt.minutesByCharacteristicId.entrySet()) {
        int characteristicId = entry.getKey();
        long characteristicDebt = entry.getValue();
        measureRepository.add(component, metric, Measure.newMeasureBuilder().forCharacteristic(characteristicId).create(characteristicDebt));
      }
    }
    this.currentDebt = null;
  }

  private class Debt {
    private long minutes = 0L;
    private final SumMap<Integer> minutesByRuleId = new SumMap<>();
    private final SumMap<Integer> minutesByCharacteristicId = new SumMap<>();

    void add(DefaultIssue issue) {
      Long issueMinutes = issue.debtInMinutes();
      if (issueMinutes != null && issueMinutes != 0L) {
        this.minutes += issueMinutes;

        RuleDto rule = ruleCache.get(issue.ruleKey());
        this.minutesByRuleId.add(rule.getId(), issueMinutes);

        Characteristic characteristic = debtModelHolder.getCharacteristicById(rule.getSubCharacteristicId());
        this.minutesByCharacteristicId.add(characteristic.getId(), issueMinutes);
        Integer characteristicParentId = characteristic.getParentId();
        if (characteristicParentId != null) {
          this.minutesByCharacteristicId.add(characteristicParentId, issueMinutes);
        }
      }
    }

    public void add(Debt debt) {
      this.minutes += debt.minutes;
      this.minutesByRuleId.add(debt.minutesByRuleId);
      this.minutesByCharacteristicId.add(debt.minutesByCharacteristicId);
    }
  }

  private static class SumMap<E> {
    private final Map<E, Long> sumByKeys = newHashMap();

    void add(SumMap<E> other) {
      for (Map.Entry<E, Long> entry : other.entrySet()) {
        add(entry.getKey(), entry.getValue());
      }
    }

    void add(@Nullable E key, Long value) {
      if (key != null) {
        Long currentValue = sumByKeys.get(key);
        sumByKeys.put(key, currentValue != null ? (currentValue + value) : value);
      }
    }

    @CheckForNull
    Long get(E key) {
      return sumByKeys.get(key);
    }

    Set<Map.Entry<E, Long>> entrySet() {
      return sumByKeys.entrySet();
    }
  }
}
