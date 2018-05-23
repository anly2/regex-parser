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
import static com.aanchev.parser.rules.GroupRule.groupRule;
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
        Rule<?> rule = groupRule("(", ")", matcher -> {
            System.out.println(matcher);
            called[0] = true;
            return null;
        });

        Matcher matcher = rule.pattern().matcher(input);
        if (matcher.matches()) {
            rule.earlyHandler().apply(matcher);
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
    public void findTopLevelGroups_works() {
        String input = "a + (b - (c + d) - e) - (f + (g - h) + i) + j";
        Pattern opening = Pattern.compile("\\(");
        Pattern closing = Pattern.compile("\\)");

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);
        groups.forEach(group -> {
            int start = group.getKey();
            int end = group.getValue();
            System.out.format("%d-%d: %s%n", start, end, input.substring(start, end));
        });
    }
}