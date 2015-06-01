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
package org.sonar.server.computation;

import org.sonar.api.config.Settings;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentTreeBuilder;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.db.DbClient;

public class ComputationContext implements org.sonar.server.computation.context.ComputationContext {
  private final Component component;

  public ComputationContext(BatchReportReader reportReader, String projectKey, Settings projectSettings, DbClient dbClient,
    ComponentTreeBuilder componentTreeBuilder, LanguageRepository languageRepository) {
    this.component = componentTreeBuilder.build(this);
  }

  @Override
  public Component getRoot() {
    return component;
  }

}
