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

package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueQueryServiceTest {

  @Mock
  DbClient dbClient;

  @Mock
  DbSession session;

  @Mock
  ComponentDao componentDao;

  IssueQueryService issueQueryService;

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);
    issueQueryService = new IssueQueryService(dbClient);
  }

  @Test
  public void create_query_from_parameters() {
    Map<String, Object> map = newHashMap();
    map.put("issues", newArrayList("ABCDE1234"));
    map.put("severities", newArrayList("MAJOR", "MINOR"));
    map.put("statuses", newArrayList("CLOSED"));
    map.put("resolutions", newArrayList("FALSE-POSITIVE"));
    map.put("resolved", true);
    map.put("components", newArrayList("org.apache"));
    map.put("componentRoots", newArrayList("org.sonar"));
    map.put("reporters", newArrayList("marilyn"));
    map.put("assignees", newArrayList("joanna"));
    map.put("languages", newArrayList("xoo"));
    map.put("assigned", true);
    map.put("planned", true);
    map.put("hideRules", true);
    map.put("createdAfter", "2013-04-16T09:08:24+0200");
    map.put("createdBefore", "2013-04-17T09:08:24+0200");
    map.put("rules", "squid:AvoidCycle,findbugs:NullReference");
    map.put("sort", "CREATION_DATE");
    map.put("asc", true);
    
    when(componentDao.getByKeys(session, newArrayList("org.apache"))).thenReturn(newArrayList(new ComponentDto().setUuid("ABCD")));
    when(componentDao.getByKeys(session, newArrayList("org.sonar"))).thenReturn(newArrayList(new ComponentDto().setUuid("BCDE")));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.issueKeys()).containsOnly("ABCDE1234");
    assertThat(query.severities()).containsOnly("MAJOR", "MINOR");
    assertThat(query.statuses()).containsOnly("CLOSED");
    assertThat(query.resolutions()).containsOnly("FALSE-POSITIVE");
    assertThat(query.resolved()).isTrue();
    assertThat(query.components()).containsOnly("ABCD");
    assertThat(query.componentRoots()).containsOnly("BCDE");
    assertThat(query.reporters()).containsOnly("marilyn");
    assertThat(query.assignees()).containsOnly("joanna");
    assertThat(query.languages()).containsOnly("xoo");
    assertThat(query.assigned()).isTrue();
    assertThat(query.planned()).isTrue();
    assertThat(query.hideRules()).isTrue();
    assertThat(query.rules()).hasSize(2);
    assertThat(query.createdAfter()).isEqualTo(DateUtils.parseDateTime("2013-04-16T09:08:24+0200"));
    assertThat(query.createdBefore()).isEqualTo(DateUtils.parseDateTime("2013-04-17T09:08:24+0200"));
    assertThat(query.sort()).isEqualTo(IssueQuery.SORT_BY_CREATION_DATE);
    assertThat(query.asc()).isTrue();
  }

  @Test
  public void add_unknown_when_no_component_found() {
    Map<String, Object> map = newHashMap();
    map.put("components", newArrayList("unknown"));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.components()).containsOnly("<UNKNOWN>");
  }

  @Test
  public void parse_list_of_rules() {
    assertThat(IssueQueryService.toRules(null)).isNull();
    assertThat(IssueQueryService.toRules("")).isEmpty();
    assertThat(IssueQueryService.toRules("squid:AvoidCycle")).containsOnly(RuleKey.of("squid", "AvoidCycle"));
    assertThat(IssueQueryService.toRules("squid:AvoidCycle,findbugs:NullRef")).containsOnly(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("findbugs", "NullRef"));
    assertThat(IssueQueryService.toRules(asList("squid:AvoidCycle", "findbugs:NullRef"))).containsOnly(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("findbugs", "NullRef"));
  }

}