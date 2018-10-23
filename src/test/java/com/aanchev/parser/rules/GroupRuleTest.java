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

import com.aanchev.parser.ParseException;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aanchev.parser.rules.EarlyRule.early;
import static com.aanchev.parser.rules.GroupRule.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

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
    public void groupRule_delegatesToDecorated() {
        @SuppressWarnings("unchecked")
        Rule<Object> body = mock(Rule.class);
        Rule<Object> groupRule = groupMatching(body, "\\(", "\\)");

        when(body.shouldIgnoreGroup(0)).thenReturn(false);
        when(body.shouldIgnoreGroup(1)).thenReturn(true);
        assertThat(groupRule.shouldIgnoreGroup(0), is(false));
        assertThat(groupRule.shouldIgnoreGroup(1), is(true));

        Matcher matcher = Pattern.compile(".*+").matcher("if(true)");
        assumeTrue(matcher.matches());
        List<Object> children = emptyList();

        groupRule.pattern();
        groupRule.handleMatch(matcher);
        groupRule.handle(matcher, children);

        verify(body).pattern();
        verify(body).handleMatch(any());
        verify(body).handle(any(), any());
    }

    @Test
    public void groupRule_matches_onlyTopLevelGroups() {
        String input = "a + (b - (c + d) - e) - (f + (g - h) + i) + j";

        List<Pair<Integer, Integer>> expectedGroups = asList(
                new Pair<>(4, 21),
                new Pair<>(24, 41)
        );

        boolean[] called = {false};
        Rule<?> rule = groupMatchingRule("\\(", "\\)", (match, children) -> {
            List<Pair<Integer, Integer>> matchedGroups = new LinkedList<>();
            for (int g = 1; g <= match.groupCount(); g++) {
                matchedGroups.add(new Pair<>(match.start(g), match.end(g)));
            }
            assertThat(matchedGroups, is(expectedGroups));

            called[0] = true;
            return null;
        });

        Matcher matcher = rule.pattern().matcher(input);
        assumeTrue(matcher.matches());

        MatchResult match = rule.handleMatch(matcher.toMatchResult());
        rule.handle(match, emptyList());

        assertThat(called[0], is(true));
    }

    @Test
    public void groupRule_preservesMatchStartOffset() {
        String input = "a[target][href]";

        Rule<String> rule = groupMatchingRule("\\[", "\\]",
                (match, children) -> "a");

        Matcher matcher = rule.pattern().matcher(input).region(1, 9);
        assumeTrue(matcher.matches());

        MatchResult groupMatch = ((GroupRule) rule).matchGroups(matcher);

        assertThat(groupMatch.groupCount(), is(1));
        assertThat(groupMatch.start(), is(1));
        assertThat(groupMatch.end(), is(9));
        assertThat(groupMatch.start(1), is(1));
        assertThat(groupMatch.end(1), is(9));
        assertThat(groupMatch.group(), is("[target]"));
        assertThat(groupMatch.group(1), is("[target]"));
    }

    @Test
    public void groupRule_passesMatchedGroups_toEarlyMatchHandler() {
        String input = "a + (b - (c + d) - e) - (f + (g - h) + i) + j";

        List<Pair<Integer, Integer>> expectedGroups = asList(
                new Pair<>(4, 21),
                new Pair<>(24, 41)
        );

        boolean[] called = {false};
        Rule<String> rule = groupMatching("\\(", "\\)",
                early(
                        RegexRule.rule(".*+", (m, c) -> "EARLY"),
                        match -> {
                            List<Pair<Integer, Integer>> matchedGroups = new LinkedList<>();
                            for (int g = 1; g <= match.groupCount(); g++) {
                                matchedGroups.add(new Pair<>(match.start(g), match.end(g)));
                            }
                            assertThat(matchedGroups, is(expectedGroups));

                            called[0] = true;
                            return match;
                        }
                )
        );

        Matcher matcher = rule.pattern().matcher(input);
        assumeTrue(matcher.matches());

        MatchResult match = rule.handleMatch(matcher.toMatchResult());
        String result = rule.handle(match, emptyList());

        assertThat(result, is("EARLY"));
        assertThat(called[0], is(true));
    }

    @Test
    public void groupRule_doesNotMatch_whenNoGroupsFound() {
        String input = "int a = 1";

        Rule<String> rule = groupMatchingRule("<", ">", (m, c) -> "should not happen");

        Matcher matcher = Pattern.compile(".*+").matcher(input);
        assumeTrue(matcher.matches());
        assertThat(rule.handleMatch(matcher), nullValue());
    }

    @Test
    public void groupRule_doesNotMatch_unbalancedGroups() {
        String input = "if (a > ((Map<String, Integer>) b.get(\"b\")))";

        Rule<String> rule = groupMatching("<", ">",
                RegexRule.rule(".*+", (m, c) -> ""));

        Matcher matcher = Pattern.compile(".*+").matcher(input);
        assumeTrue(matcher.matches());
        assertThat(rule.handleMatch(matcher), nullValue());
    }

    @Test
    public void groupRule_doesNotMatch_whenUnnecessary() {
        String input = "{1, 2}";

        Rule<String> rule = groupMatchingRule("\\{", "\\}", (m, c) -> "should happen once");

        Matcher matcher = Pattern.compile(".*+").matcher(input);
        assumeTrue(matcher.matches());
        MatchResult match = matcher.toMatchResult();

        match = rule.handleMatch(match);
        assertThat(match, nullValue());
    }


    // test custom match implementation

    @Test
    public void matchImplementation_sameAsMatcherAsMatchResult() {
        String input = "abc";

        Matcher matcher = Pattern.compile(".(.).").matcher(input);
        assumeTrue(matcher.matches());

        MatchResult match = asMatchResult(input, asList(new Pair<>(0, 3), new Pair<>(1, 2)));

        assertThat(match.groupCount(), is(matcher.groupCount()));

        assertThat(match.start(), is(matcher.start()));
        assertThat(match.start(0), is(matcher.start(0)));
        assertThat(match.start(1), is(matcher.start(1)));
        assertThat(match.start(2), is(-1));

        assertThat(match.end(), is(matcher.end()));
        assertThat(match.end(0), is(matcher.end(0)));
        assertThat(match.end(1), is(matcher.end(1)));
        assertThat(match.end(2), is(-1));

        assertThat(match.group(), is(matcher.group()));
        assertThat(match.group(0), is(matcher.group(0)));
        assertThat(match.group(1), is(matcher.group(1)));

        try {
            match.group(2);
            fail("An exception should be throw for non-existent groups.");
        } catch (IndexOutOfBoundsException e0) {
            try {
                assumeTrue(matcher.group(2) == null);
            } catch (IndexOutOfBoundsException e1) {
                assertThat(e0.getMessage(), is(e1.getMessage()));
            }
        }
    }

    @Test
    public void originalMatch_accessible() {
        String input = "a(b)c";
        Matcher matcher = Pattern.compile("(.)...(.)").matcher(input);
        Rule<?> rule = groupMatchingRule("\\(", "\\)", (m, c) -> null);

        assumeTrue(matcher.matches());

        MatchResult match = rule.handleMatch(matcher);
        MatchResult original = originalMatch(match);

        assertThat(original.group(1), is("a"));
        assertThat(original.group(2), is("c"));
        assertThat(match.group(1), is("(b)"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void originalMatch_throwsForOtherRules() {
        String input = "a(b)c";
        Matcher matcher = Pattern.compile("(.)...(.)").matcher(input);
        Rule<?> rule = RegexRule.rule("..(.)..", (m, c) -> null);

        assumeTrue(matcher.matches());

        MatchResult match = rule.handleMatch(matcher);
        originalMatch(match);
    }
}