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

import static com.aanchev.parser.rules.MaskedRule.masked;
import static com.aanchev.parser.rules.RegexRule.rule;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MaskedRuleTest {

    @Test
    public void masked_canDecorateRule_varargsIgnoredGroups() {
        boolean[] called = {false};
        Rule<Boolean> sut = masked(rule("test pattern", children -> called[0] = true), 1);

        Matcher m = sut.pattern().matcher("");
        sut.handle(m.toMatchResult(), emptyList());

        assertThat(called[0], is(true));
        assertThat(sut.shouldIgnoreGroup(0), is(false));
        assertThat(sut.shouldIgnoreGroup(1), is(true));
        assertThat(sut.shouldIgnoreGroup(2), is(false));
    }

    @Test
    public void masked_canDecorateRule_iterableIgnoredGroups() {
        boolean[] called = {false};
        Rule<Boolean> sut = masked(rule("test pattern", children -> called[0] = true), singleton(1));

        Matcher m = sut.pattern().matcher("");
        sut.handle(m.toMatchResult(), emptyList());

        assertThat(called[0], is(true));
        assertThat(sut.shouldIgnoreGroup(0), is(false));
        assertThat(sut.shouldIgnoreGroup(1), is(true));
        assertThat(sut.shouldIgnoreGroup(2), is(false));
    }

    @Test
    public void masked_delegatesToDecorated() {
        @SuppressWarnings("unchecked")
        Rule<Object> body = mock(Rule.class);
        Rule<Object> masked = masked(body, 1);

        MatchResult match = Pattern.compile(".*+").matcher("").toMatchResult();
        List<Object> children = emptyList();

        masked.pattern();
        masked.handleMatch(match);
        masked.handle(match, children);

        verify(body).pattern();
        verify(body).handleMatch(match);
        verify(body).handle(match, children);
    }
}