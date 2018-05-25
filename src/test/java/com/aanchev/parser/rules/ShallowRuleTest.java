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

import java.util.regex.Matcher;

import static com.aanchev.parser.rules.RegexRule.rule;
import static com.aanchev.parser.rules.ShallowRule.shallow;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ShallowRuleTest {

    @Test
    public void shallowRule_canBeCreated_withLateHandler() {
        boolean[] called = {false};
        Rule<Boolean> sut = shallow(rule("test pattern", (match, children) -> called[0] = true));

        Matcher m = sut.pattern().matcher("");
        sut.handle(m.toMatchResult(), emptyList());

        assertThat(called[0], is(true));
        assertThat(sut.shouldIgnoreGroup(0), is(true));
    }
}