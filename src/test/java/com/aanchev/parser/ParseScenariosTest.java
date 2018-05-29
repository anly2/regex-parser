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

package com.aanchev.parser;

import org.junit.Test;

import static com.aanchev.parser.rules.GroupRule.groupMatchingRule;
import static com.aanchev.parser.rules.RegexRule.rule;
import static com.aanchev.parser.rules.ShallowRule.shallow;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ParseScenariosTest {
    @Test
    public void scenario_verySimple() {
        Parser parser = new RegexDownstrippingParser<String>(asList(
                rule("\\d+", (match, children) -> "int " + match.group()),
                rule("\\+(.*)", (match, children) -> "positive " + children.get(0)),
                rule("\\-(.*)", (match, children) -> "negative " + children.get(0))
        ));

        assertThat(parser.parse("123"), is("int 123"));
        assertThat(parser.parse("+456"), is("positive int 456"));
        assertThat(parser.parse("-789"), is("negative int 789"));
    }

    @Test
    public void scenario_withGroups() {
        Parser parser = new RegexDownstrippingParser<String>(asList(
                rule("(\\w+)(\\[.*\\])",
                        (match, children) -> "" + children.get(0) + children.get(1)),
                rule("\\w+",
                        (match, children) -> String.format("tag '%s'", match.group())),
                groupMatchingRule("\\[", "\\]",
                        (match, children) -> String.join(",", children)),
                shallow(rule("\\[(\\w+)\\]",
                        (match, children) -> String.format(" with attribute '%s' present", match.group(1)))),
                shallow(rule("\\[(\\w+)\\*=(['\"])?(.*)\\2\\]",
                        (match, children) -> String.format(" with attribute '%s' containing '%s'", match.group(1), match.group(3))))
        ));

        assertThat(parser.parse("a"),
                is("tag 'a'"));
        assertThat(parser.parse("a[target]"),
                is("tag 'a' with attribute 'target' present"));
        assertThat(parser.parse("a[target][href]"),
                is("tag 'a' with attribute 'target' present, with attribute 'href' present"));
        assertThat(parser.parse("a[target][href*='[127.0.0.1]']"),
                is("tag 'a' with attribute 'target' present, with attribute 'href' containing '[127.0.0.1]'"));
    }
}
