/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.source;

import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.core.source.HtmlTextDecorator.CR_END_OF_LINE;
import static org.sonar.core.source.HtmlTextDecorator.LF_END_OF_LINE;

public class HtmlTextDecoratorTest {

  @Test
  public void should_decorate_simple_character_range() throws Exception {

    String packageDeclaration = "package org.sonar.core.source;";

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,7,k;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(packageDeclaration, decorationData);

    assertThat(htmlOutput).containsOnly("<span class=\"k\">package</span> org.sonar.core.source;");
  }

  @Test
  public void should_decorate_multiple_lines_characters_range() throws Exception {

    String firstCommentLine = "/*";
    String secondCommentLine = " * Test";
    String thirdCommentLine = " */";

    String blockComment = firstCommentLine + LF_END_OF_LINE
      + secondCommentLine + LF_END_OF_LINE
      + thirdCommentLine + LF_END_OF_LINE;

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,14,cppd;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(blockComment, decorationData);

    assertThat(htmlOutput).containsExactly(
        "<span class=\"cppd\">" + firstCommentLine + "</span>",
        "<span class=\"cppd\">" + secondCommentLine + "</span>",
        "<span class=\"cppd\">" + thirdCommentLine + "</span>",
        ""
        );
  }

  @Test
  public void should_highlight_multiple_words_in_one_line() throws Exception {

    String classDeclaration = "public class MyClass implements MyInterface {";

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,6,k;7,12,k;21,31,k;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(classDeclaration, decorationData);

    assertThat(htmlOutput).containsOnly(
        "<span class=\"k\">public</span> " +
          "<span class=\"k\">class</span> MyClass " +
          "<span class=\"k\">implements</span> MyInterface {");
  }

  @Test
  public void should_allow_multiple_levels_highlighting() throws Exception {

    String javaDocSample =
        "/**" + LF_END_OF_LINE +
          " * Creates a FormulaDecorator" + LF_END_OF_LINE +
          " *" + LF_END_OF_LINE +
          " * @param metric the metric should have an associated formula" + LF_END_OF_LINE +
          " * " + LF_END_OF_LINE +
          " * @throws IllegalArgumentException if no formula is associated to the metric" + LF_END_OF_LINE +
          " */" + LF_END_OF_LINE;

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,184,cppd;47,53,k;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(javaDocSample, decorationData);

    assertThat(htmlOutput).containsExactly(
        "<span class=\"cppd\">/**</span>",
        "<span class=\"cppd\"> * Creates a FormulaDecorator</span>",
        "<span class=\"cppd\"> *</span>",
        "<span class=\"cppd\"> * @param <span class=\"k\">metric</span> the metric should have an associated formula</span>",
        "<span class=\"cppd\"> * </span>",
        "<span class=\"cppd\"> * @throws IllegalArgumentException if no formula is associated to the metric</span>",
        "<span class=\"cppd\"> */</span>",
        ""
        );
  }

  @Test
  public void should_support_crlf_line_breaks() throws Exception {

    String crlfCodeSample =
        "/**" + CR_END_OF_LINE + LF_END_OF_LINE +
          "* @return metric generated by the decorator" + CR_END_OF_LINE + LF_END_OF_LINE +
          "*/" + CR_END_OF_LINE + LF_END_OF_LINE +
          "@DependedUpon" + CR_END_OF_LINE + LF_END_OF_LINE +
          "public Metric generatesMetric() {" + CR_END_OF_LINE + LF_END_OF_LINE +
          "  return metric;" + CR_END_OF_LINE + LF_END_OF_LINE +
          "}" + CR_END_OF_LINE + LF_END_OF_LINE;

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,52,cppd;54,67,a;69,75,k;106,112,k;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(crlfCodeSample, decorationData);

    assertThat(htmlOutput).containsExactly(
        "<span class=\"cppd\">/**</span>",
        "<span class=\"cppd\">* @return metric generated by the decorator</span>",
        "<span class=\"cppd\">*/</span>",
        "<span class=\"a\">@DependedUpon</span>",
        "<span class=\"k\">public</span> Metric generatesMetric() {",
        "  <span class=\"k\">return</span> metric;",
        "}",
        ""
        );
  }

  @Test
  public void should_close_tags_at_end_of_file() throws Exception {

    String classDeclarationSample =
        "/*" + LF_END_OF_LINE +
          " * Header" + LF_END_OF_LINE +
          " */" + LF_END_OF_LINE +
          LF_END_OF_LINE +
          "public class HelloWorld {" + LF_END_OF_LINE +
          "}";

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,16,cppd;18,25,k;25,31,k;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(classDeclarationSample, decorationData);

    assertThat(htmlOutput).containsExactly(
        "<span class=\"cppd\">/*</span>",
        "<span class=\"cppd\"> * Header</span>",
        "<span class=\"cppd\"> */</span>",
        "",
        "<span class=\"k\">public </span><span class=\"k\">class </span>HelloWorld {",
        "}"
        );
  }

  @Test
  public void should_escape_markup_chars() throws Exception {

    String javadocWithHtml =
        "/**\n" +
          " * Provides a basic framework to sequentially read any kind of character stream in order to feed a generic OUTPUT.\n" +
          " * \n" +
          " * This framework can used for instance in order to :\n" +
          " * <ul>\n" +
          " *   <li>Create a lexer in charge to generate a list of tokens from a character stream</li>\n" +
          " *   <li>Create a source code syntax highligther in charge to decorate a source code with HTML tags</li>\n" +
          " *   <li>Create a javadoc generator</li>\n" +
          " *   <li>...</li>\n" +
          " * </ul>\n" +
          " */\n";

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,453,cppd;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(javadocWithHtml, decorationData);

    assertThat(htmlOutput).containsExactly(
        "<span class=\"cppd\">/**</span>",
        "<span class=\"cppd\"> * Provides a basic framework to sequentially read any kind of character stream in order to feed a generic OUTPUT.</span>",
        "<span class=\"cppd\"> * </span>",
        "<span class=\"cppd\"> * This framework can used for instance in order to :</span>",
        "<span class=\"cppd\"> * &lt;ul&gt;</span>",
        "<span class=\"cppd\"> *   &lt;li&gt;Create a lexer in charge to generate a list of tokens from a character stream&lt;/li&gt;</span>",
        "<span class=\"cppd\"> *   &lt;li&gt;Create a source code syntax highligther in charge to decorate a source code with HTML tags&lt;/li&gt;</span>",
        "<span class=\"cppd\"> *   &lt;li&gt;Create a javadoc generator&lt;/li&gt;</span>",
        "<span class=\"cppd\"> *   &lt;li&gt;...&lt;/li&gt;</span>",
        "<span class=\"cppd\"> * &lt;/ul&gt;</span>",
        "<span class=\"cppd\"> */</span>",
        "");
  }

  @Test
  public void should_escape_ampersand_char() throws Exception {

    String javadocWithAmpersandChar =
        "/**\n" +
          " * Definition of a dashboard.\n" +
          " * <p/>\n" +
          " * Its name and description can be retrieved using the i18n mechanism, using the keys \"dashboard.&lt;id&gt;.name\" and\n" +
          " * \"dashboard.&lt;id&gt;.description\".\n" +
          " *\n" +
          " * @since 2.13\n" +
          " */\n";

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,220,cppd;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(javadocWithAmpersandChar, decorationData);

    assertThat(htmlOutput).containsExactly(
        "<span class=\"cppd\">/**</span>",
        "<span class=\"cppd\"> * Definition of a dashboard.</span>",
        "<span class=\"cppd\"> * &lt;p/&gt;</span>",
        "<span class=\"cppd\"> * Its name and description can be retrieved using the i18n mechanism, using the keys \"dashboard.&amp;lt;id&amp;gt;.name\" and</span>",
        "<span class=\"cppd\"> * \"dashboard.&amp;lt;id&amp;gt;.description\".</span>",
        "<span class=\"cppd\"> *</span>",
        "<span class=\"cppd\"> * @since 2.13</span>",
        "<span class=\"cppd\"> */</span>",
        "");
  }

  @Test
  public void should_support_cr_line_breaks() throws Exception {

    String crCodeSample =
        "/**" + CR_END_OF_LINE +
          "* @return metric generated by the decorator" + CR_END_OF_LINE +
          "*/" + CR_END_OF_LINE +
          "@DependedUpon" + CR_END_OF_LINE +
          "public Metric generatesMetric() {" + CR_END_OF_LINE +
          "  return metric;" + CR_END_OF_LINE +
          "}" + CR_END_OF_LINE;

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,50,cppd;51,64,a;65,71,k;101,107,k;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(crCodeSample, decorationData);

    assertThat(htmlOutput).containsExactly(
        "<span class=\"cppd\">/**</span>",
        "<span class=\"cppd\">* @return metric generated by the decorator</span>",
        "<span class=\"cppd\">*/</span>",
        "<span class=\"a\">@DependedUpon</span>",
        "<span class=\"k\">public</span> Metric generatesMetric() {",
        "  <span class=\"k\">return</span> metric;",
        "}",
        ""
        );

  }

  @Test
  public void should_support_multiple_empty_lines_at_end_of_file() throws Exception {

    String classDeclarationSample =
        "/*" + LF_END_OF_LINE +
          " * Header" + LF_END_OF_LINE +
          " */" + LF_END_OF_LINE +
          LF_END_OF_LINE +
          "public class HelloWorld {" + LF_END_OF_LINE +
          "}" + LF_END_OF_LINE + LF_END_OF_LINE + LF_END_OF_LINE;

    DecorationDataHolder decorationData = new DecorationDataHolder();
    decorationData.loadSyntaxHighlightingData("0,16,cppd;18,25,k;25,31,k;");

    HtmlTextDecorator htmlTextDecorator = new HtmlTextDecorator();
    List<String> htmlOutput = htmlTextDecorator.decorateTextWithHtml(classDeclarationSample, decorationData);

    assertThat(htmlOutput).containsExactly(
        "<span class=\"cppd\">/*</span>",
        "<span class=\"cppd\"> * Header</span>",
        "<span class=\"cppd\"> */</span>",
        "",
        "<span class=\"k\">public </span><span class=\"k\">class </span>HelloWorld {",
        "}",
        "",
        "",
        ""
        );
  }
}