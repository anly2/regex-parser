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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aanchev.parser.rules.MaskedRule.maskedRule;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;

public class MaskedRuleTest {

    @Test
    public void maskedRule_canBeCreated_withLateHandler() {
        boolean[] called = {false};
        Rule<Boolean> sut = maskedRule("test pattern", singleton(1), children -> called[0] = true);

        Matcher m = sut.pattern().matcher("");
        sut.earlyHandler().apply(m).apply(emptyList());

        assertThat(called[0], is(true));
        assertThat(sut.shouldIgnoreGroup(0), is(false));
        assertThat(sut.shouldIgnoreGroup(1), is(true));
        assertThat(sut.shouldIgnoreGroup(2), is(false));
    }

    @Test
    public void maskedRule_canBeCreated_withFullHandler() {
        Matcher matcher = Pattern.compile("").matcher("");
        List<Boolean> children = new LinkedList<>();

        boolean[] called = {false};

        Rule<Boolean> sut = maskedRule("test pattern", singleton(1), (m, c) -> {
            assertSame(m, matcher);
            assertSame(c, children);
            called[0] = true;
            return true;
        });

        Boolean result = sut.earlyHandler().apply(matcher).apply(children);

        assertThat(called[0], is(true));
        assertThat(result, is(true));
        assertThat(sut.shouldIgnoreGroup(0), is(false));
        assertThat(sut.shouldIgnoreGroup(1), is(true));
        assertThat(sut.shouldIgnoreGroup(2), is(false));
    }
}