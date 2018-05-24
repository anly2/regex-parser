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

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static com.aanchev.parser.rules.RegexRule.rule;
import static com.aanchev.parser.rules.ShallowRule.shallow;
import static lombok.AccessLevel.PROTECTED;

@AllArgsConstructor(access = PROTECTED)
@ToString(exclude = "matchHandler")
public class EarlyRule<O> implements Rule<O> {

    private Rule<O> body;
    private Function<MatchResult, MatchResult> matchHandler;



    /* Decorated functionality */

    @Override
    public MatchResult handleMatch(MatchResult match) {
        return matchHandler.apply(match);
    }


    /* Delegation */

    @Override
    public Pattern pattern() {
        return body.pattern();
    }

    @Override
    public boolean shouldIgnoreGroup(int groupIndex) {
        return body.shouldIgnoreGroup(groupIndex);
    }

    @Override
    public O handle(MatchResult match, List<O> children) {
        return body.handle(match, children);
    }


    /* Static initializers */

    public static <O> Rule<O> early(Rule<O> body, Function<MatchResult, MatchResult> matchHandler) {
        return new EarlyRule<>(body, matchHandler);
    }

    public static <O> Rule<O> earlyRule(String regex, Function<MatchResult, O> handler) {
        return earlyRule(Pattern.compile(regex), handler);
    }

    public static <O> Rule<O> earlyRule(Pattern pattern, Function<MatchResult, O> handler) {
        return shallow(rule(pattern, (match, children) -> handler.apply(match)));
    }
}
