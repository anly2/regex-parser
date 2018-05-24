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

import javafx.util.Pair;
import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aanchev.parser.rules.GroupRule.findTopLevelGroups;
import static com.aanchev.parser.rules.GroupRule.groupMatchingRule;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class GroupRuleTest {

    @Test
    public void groupRule_matches_singleGroup() {
    }

    @Test
    public void groupRule_matches_multipleGroups() {
    }

    @Test
    public void groupRule_matches_onlyTopLevelGroups() {
        String input = "a + (b - (c + d) - e) - (f + (g - h) + i) + j";

        boolean[] called = {false};
        Rule<?> rule = groupMatchingRule("\\(", "\\)", (match, children) -> {
            for (int g = 1; g <= match.groupCount(); g++) {
                System.out.println(g + ": " + match.group(g));
            }
            called[0] = true;
            return null;
        });

        Matcher matcher = rule.pattern().matcher(input);
        if (matcher.matches()) {
            rule.handle(matcher.toMatchResult(), emptyList());
        }

        assertThat(called[0], is(true));

    }

    @Test
    public void groupRule_doesNotMatch_noGroups() {
    }

    @Test
    public void groupRule_doesNotMatch_unbalancedGroups() {
    }


    // test just the grouping functionality //

    @Test
    public void findTopLevelGroups_findsSingleWholeGroup() {
        String input = "<A, B>";
        Pattern opening = Pattern.compile("<");
        Pattern closing = Pattern.compile(">");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(singletonList(
                new Pair<>(1, 5)
        )));
    }

    @Test
    public void findTopLevelGroups_findsSingleSurroundedGroup() {
        String input = "private Map<String, String> aliases;";
        Pattern opening = Pattern.compile("<");
        Pattern closing = Pattern.compile(">");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(singletonList(
                new Pair<>(12, 26)
        )));
    }


    @Test
    public void findTopLevelGroups_findsMultipleTopLevelGroups() {
        String input = "a[href][target]";
        Pattern opening = Pattern.compile("\\[");
        Pattern closing = Pattern.compile("]");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(asList(
                new Pair<>(2, 6),
                new Pair<>(8, 14)
        )));
    }

    @Test
    public void findTopLevelGroups_worksWithLenghtyBoundaries() {
        String input = "Lorem <b>ipsum</b> <em>dolor</em> <span>sit amet</span>";
        Pattern opening = Pattern.compile("<\\w++>");
        Pattern closing = Pattern.compile("</\\w++>");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(asList(
                new Pair<>(9, 14),
                new Pair<>(23, 28),
                new Pair<>(40, 48)
        )));
    }

    @Test
    public void findTopLevelGroups_skipsNestedGroups() {
        String input = "a + (b - (c + d) - e) - (f + (g - h) + i) + j";
        Pattern opening = Pattern.compile("\\(");
        Pattern closing = Pattern.compile("\\)");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        assertThat(groups, is(asList(
                new Pair<>(5, 20),
                new Pair<>(25, 40)
        )));
    }
}