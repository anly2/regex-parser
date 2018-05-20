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

import java.util.regex.Pattern;

import static com.aanchev.parser.rules.RegexRule.rule;
import static com.aanchev.parser.rules.ShallowRule.shallowRule;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;

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

    @Test(expected = ParseException.class)
    public void parse_throwsOnUnknown() {
        Parser parser = new RegexDownstrippingParser<String>(emptyList());
        parser.parse("UNDEFINED");
    }

    @Test(expected = ParseException.class)
    public void parse_throwsOnUnknown_nested() {
        Parser parser = new RegexDownstrippingParser<String>(singletonList(
                rule("\\s++(.*)", c -> c.get(0))
        ));

        try {
            parser.parse("    UNDEFINED");
        } catch (ParseException e) {
            assertThat(e.getMessage(), stringContainsInOrder(asList("4", "13", "UNDEFINED")));
            throw e;
        }
    }

    @Test
    public void parse_allowsNesting() {
        Parser parser = new RegexDownstrippingParser<>(asList(
                rule("\\s*[A]\\s*", children -> "type a"),
                rule("\\s*[B]\\s*", children -> "type b"),
                rule("\\s*[C]\\s*", children -> "type c"),
                rule("(\\S++)\\s++(\\S++)\\s++(\\S++)", children -> children)
        ));

        assertThat(parser.parse("A B C"), is(asList("type a", "type b", "type c")));
    }

    @Test
    public void parse_honorsPrecedence_encodedInRuleOrder() {
        //test precedence with downspiralling example: "A>B<C<D>E"
        String expression = "A>B<C<D>E";

        // if '>' has higher precedence than '<'
        // then expect `(A > ((B < (C < D)) > E))`
        {
            Parser parser = new RegexDownstrippingParser<>(asList(
                    rule("\\s*(.*?)>(.*)", children -> String.format("(%s > %s)", children.get(0), children.get(1))),
                    rule("\\s*(.*?)<(.*)", children -> String.format("(%s < %s)", children.get(0), children.get(1))),
                    rule("\\s*[A-Z]\\s*", (matcher, children) -> matcher.group().trim())
            ));

            assertThat(parser.parse(expression), is("(A > ((B < (C < D)) > E))"));
        }

        // if '<' has higher precedence than '>'
        // then expect `((A > B) < (C < (D > E)))`
        {
            Parser parser = new RegexDownstrippingParser<>(asList(
                    rule("\\s*(.*?)<(.*)", children -> String.format("(%s < %s)", children.get(0), children.get(1))),
                    rule("\\s*(.*?)>(.*)", children -> String.format("(%s > %s)", children.get(0), children.get(1))),
                    rule("\\s*[A-Z]\\s*", (matcher, children) -> matcher.group().trim())
            ));

            assertThat(parser.parse(expression), is("((A > B) < (C < (D > E)))"));
        }
    }

    @Test
    public void parse_honorsAssociativeDirection_encodedInGreediness() {
        //test associative direction through greedy/reluctant in-pattern groups
        String expression = "A>B>C";

        // if '>' associates to the left
        // then expect `((A > B) > C)`
        {
            Parser parser = new RegexDownstrippingParser<>(asList(
                    rule("\\s*(.*)>(.*?)", children -> String.format("(%s > %s)", children.get(0), children.get(1))),
                    rule("\\s*[A-Z]\\s*", (matcher, children) -> matcher.group().trim())
            ));

            assertThat(parser.parse(expression), is("((A > B) > C)"));
        }

        // if '>' associates to the right
        // then expect `(A > (B > C))`
        {
            Parser parser = new RegexDownstrippingParser<>(asList(
                    rule("\\s*(.*?)>(.*)", children -> String.format("(%s > %s)", children.get(0), children.get(1))),
                    rule("\\s*[A-Z]\\s*", (matcher, children) -> matcher.group().trim())
            ));

            assertThat(parser.parse(expression), is("(A > (B > C))"));
        }
    }

    @Test
    public void parse_honorsEarlyHandlerVeto_expressedAsReturnedNull() {
        Parser parser = new RegexDownstrippingParser<>(asList(
                rule(Pattern.compile(".*"), matcher -> null),
                rule("[A]", (matcher, children) -> "letter a")
        ));

        assertThat(parser.parse("A"), is("letter a"));
    }

    @Test
    public void parse_ignoresGroups() {
        Parser parser = new RegexDownstrippingParser<String>(asList(
                rule("\\d+", (matcher, children) -> "int " + matcher.group()),
                shallowRule("\\+(\\d+)", (matcher, children) -> {
                    assertTrue(children.isEmpty());
                    return "positive " + matcher.group(1);
                })
        ));

        assertThat(parser.parse("+1"), is("positive 1"));
    }
}