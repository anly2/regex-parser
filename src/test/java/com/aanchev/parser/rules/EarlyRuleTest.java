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

import org.junit.Test;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aanchev.parser.rules.EarlyRule.early;
import static com.aanchev.parser.rules.EarlyRule.earlyRule;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;

public class EarlyRuleTest {

    @Test
    @SuppressWarnings("unchecked")
    public void early_delegatesToDecorated() {
        Rule<Object> body = mock(Rule.class);
        Rule<Object> early = early(body, m -> m);

        early.pattern();

        when(body.shouldIgnoreGroup(0)).thenReturn(false);
        when(body.shouldIgnoreGroup(1)).thenReturn(true);
        assertThat(early.shouldIgnoreGroup(0), is(false));
        assertThat(early.shouldIgnoreGroup(1), is(true));

        MatchResult match = Pattern.compile(".*+").matcher("").toMatchResult();
        List<Object> children = emptyList();
        early.handle(match, children);

        verify(body).shouldIgnoreGroup(0);
        verify(body).shouldIgnoreGroup(1);
        verify(body).pattern();
        verify(body).handle(match, children);
    }

    @Test
    public void earlyRule_createsRule() {
        String regex = "<(.)>";
        String input = "<A>";

        Matcher matcher = Pattern.compile(regex).matcher(input);
        assumeTrue(matcher.find());

        Rule<String> rule = earlyRule(regex, match -> match.group(1));
        String result = rule.handle(matcher.toMatchResult(), emptyList());

        assertThat(rule.pattern().pattern(), is(Pattern.compile(regex).pattern()));
        assertThat(result, is("A"));
    }
}