/*
 * Copyright 2018 Anko Anchev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.aanchev.parser.rules;

import com.aanchev.parser.GroupRule.*;
import com.aanchev.parser.ParseException;
import com.aanchev.parser.Parser;
import com.aanchev.parser.Rule;
import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aanchev.parser.GroupRule.*;
import static com.aanchev.parser.Parser.parser;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeTrue;

public class GroupRuleTest {

    // test just the grouping functionality //

    @Test
    public void findTopLevelGroups_returnsEmpty_whenNoGroupsFound() {
        String input = "ul > li";
        Pattern opening = Pattern.compile("\\[");
        Pattern closing = Pattern.compile("]");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(empty()));
    }

    @Test
    public void findTopLevelGroups_findsSingleWholeGroup() {
        String input = "<A, B>";
        Pattern opening = Pattern.compile("<");
        Pattern closing = Pattern.compile(">");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(singletonList(
                new Pair<>(0, 6)
        )));
    }

    @Test
    public void findTopLevelGroups_findsSingleSurroundedGroup() {
        String input = "private Map<String, String> aliases;";
        Pattern opening = Pattern.compile("<");
        Pattern closing = Pattern.compile(">");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(singletonList(
                new Pair<>(11, 27)
        )));
    }

    @Test
    public void findTopLevelGroups_findsMultipleTopLevelGroups() {
        String input = "a[href][target]";
        Pattern opening = Pattern.compile("\\[");
        Pattern closing = Pattern.compile("]");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(asList(
                new Pair<>(1, 7),
                new Pair<>(7, 15)
        )));
    }

    @Test
    public void findTopLevelGroups_worksWithLenghtyBoundaries() {
        String input = "Lorem <b>ipsum</b> <em>dolor</em> <span>sit amet</span>";
        Pattern opening = Pattern.compile("<\\w++>");
        Pattern closing = Pattern.compile("</\\w++>");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(asList(
                new Pair<>(6, 18),
                new Pair<>(19, 33),
                new Pair<>(34, 55)
        )));
    }

    @Test
    public void findTopLevelGroups_skipsNestedGroups() {
        String input = "a + (b - (c + d) - e) - (f + (g - h) + i) + j";
        Pattern opening = Pattern.compile("\\(");
        Pattern closing = Pattern.compile("\\)");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(asList(
                new Pair<>(4, 21),
                new Pair<>(24, 41)
        )));
    }

    @Test(expected = ParseException.class)
    public void findTopLevelGroups_throws_withUnbalancedExpression() {
        String input = "if (a > ((Map<String, Integer>) b.get(\"b\")))";
        Pattern opening = Pattern.compile("<");
        Pattern closing = Pattern.compile(">");

        findTopLevelGroups(input, opening, closing);
    }

    @Test
    public void getTopLevelGroups_worksWithSemiDetachedGroups() {
        List<Pair<Integer, Integer>> topLevelGroups = getTopLevelGroups(asList(0, 2), asList(2, 4));
        assertThat(topLevelGroups, is(asList(new Pair<>(0, 2), new Pair<>(2, 4))));
    }

    @Test
    public void getTopLevelGroups_worksWithEmptyNestedGroups() {
        List<Pair<Integer, Integer>> topLevelGroups = getTopLevelGroups(asList(2, 2), asList(2, 2));
        assertThat(topLevelGroups, is(singletonList(new Pair<>(2, 2))));
    }


    // test the rule contract //

    @Test
    public void groupRule_matches_onlyTopLevelGroups() {
        String input = "a + (b - (c + d) - e) - (f + (g - h) + i) + j";

        boolean[] called = {false};
        Rule<?> rule = groupRule("\\(", "\\)", (match, children) -> {
            assertThat(children.size(), is(2));
            assertThat(children.get(0), is("(b - (c + d) - e)"));
            assertThat(children.get(1), is("(f + (g - h) + i)"));

            called[0] = true;
            return "";
        });

        Matcher match = rule.pattern().matcher(input);
        assumeTrue(match.matches());

        Parser parser = parser(s -> s);
        rule.handle(match, null, parser);

        assertThat(called[0], is(true));
    }


    @Test
    public void groupRule_doesNotMatch_whenNoGroupsFound() {
        String input = "int a = 1";

        Rule<String> rule = groupRule("<", ">", (m, c) -> "should not happen");

        Matcher match = Pattern.compile(".*+").matcher(input);
        assumeTrue(match.matches());

        assertThat(rule.handle(match, null, null), nullValue());
    }

    @Test
    public void groupRule_doesNotMatch_unbalancedGroups() {
        String input = "if (a > ((Map<String, Integer>) b.get(\"b\")))";

        Rule<String> rule = groupRule("<", ">", (m, c) -> "");

        Matcher match = Pattern.compile(".*+").matcher(input);
        assumeTrue(match.matches());
        assertThat(rule.handle(match, null, null), nullValue());
    }

    @Test
    public void groupRule_doesNotMatch_whenUnnecessary() {
        String input = "{1, 2}";

        Rule<String> rule = groupRule("\\{", "\\}", (m, c) -> "should happen once");

        Matcher match = Pattern.compile(".*+").matcher(input);
        assumeTrue(match.matches());

        assertThat(rule.handle(match, null, null), nullValue());
    }

}