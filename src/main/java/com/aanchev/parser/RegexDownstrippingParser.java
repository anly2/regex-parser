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

import com.aanchev.parser.rules.Rule;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

import static java.util.Collections.unmodifiableList;

@Slf4j
public class RegexDownstrippingParser<O> implements Parser {

    private List<Rule<O>> rules;

    public RegexDownstrippingParser(List<Rule<O>> rules) {
        this.rules = unmodifiableList(rules);
    }


    @Override
    @SuppressWarnings("unchecked")
    public <E> E parse(CharSequence input) {
        return (E) parse(input, 0, input.length());
    }

    public O parse(CharSequence input, int start, int end) {
        for (Rule<O> rule : rules) {
            Matcher matcher = rule.pattern().matcher(input).region(start, end);

            if (!matcher.matches()) {
                //this rule did not match
                continue;
            }

            MatchResult match = rule.handleMatch(matcher.toMatchResult());

            if (match == null) {
                //the early match handler indicated this rule should be skipped
                continue;
            }

            if (log.isTraceEnabled()) {
                log.trace("Rule {} matched against '{}'", rule, input.subSequence(start, end));
            }

            LinkedList<O> children = new LinkedList<>();
            for (int g = 1; g <= match.groupCount(); g++) {
                if (rule.shouldIgnoreGroup(g)) {
                    continue;
                }

                int s = match.start(g);
                int e = match.end(g);

                children.add((s == -1 || e == -1) ? null : parse(input, s, e));
            }

            return rule.handle(match, children);
        }

        throw new ParseException("Unable to parse a section. " +
                String.format("No rule matched the region %d to %d ('%s')",
                        start, end, input.subSequence(start, end)));
    }
}

