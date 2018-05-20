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

import static com.aanchev.parser.rules.RegexRule.rule;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RegexDownstrippingParserTest {

    @Test
    public void parse_canMap_simple() {
        Parser parser = new RegexDownstrippingParser<String>(asList(
                rule("\\s*[A]\\s*", children -> "type a"),
                rule("\\s*[B]\\s*", children -> "type b"),
                rule("\\s*[C]\\s*", children -> "type c")
        ));

        assertThat(parser.parse("A"), is("type a"));
        assertThat(parser.parse(" B"), is("type b"));
        assertThat(parser.parse(" C "), is("type c"));
    }

    @Test
    public void parse_canMap_dynamic() {
        Parser parser = new RegexDownstrippingParser<String>(asList(
                rule("\\d+", (matcher, children) -> "int " + matcher.group()),
                rule("\\+\\d+", (matcher, children) -> "positive " + matcher.group().substring(1)),
                rule("\\-\\d+", (matcher, children) -> "negative " + matcher.group().substring(1))
        ));

        assertThat(parser.parse("123"), is("int 123"));
        assertThat(parser.parse("+456"), is("positive 456"));
        assertThat(parser.parse("-789"), is("negative 789"));
    }

    @Test
    public void parse_canMap_nested() {
        Parser parser = new RegexDownstrippingParser<String>(asList(
                rule("\\d+", (matcher, children) -> "int " + matcher.group()),
                rule("\\+(.*)", (matcher, children) -> "positive " + children.get(0)),
                rule("\\-(.*)", (matcher, children) -> "negative " + children.get(0))
        ));

        assertThat(parser.parse("123"), is("int 123"));
        assertThat(parser.parse("+456"), is("positive int 456"));
        assertThat(parser.parse("-789"), is("negative int 789"));
    }
}