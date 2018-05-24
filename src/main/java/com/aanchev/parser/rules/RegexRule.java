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
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static lombok.AccessLevel.PROTECTED;

@AllArgsConstructor(access = PROTECTED)
@ToString(exclude = "handler")
public class RegexRule<O> implements Rule<O> {

    @Getter
    @Accessors(fluent = true)
    private Pattern pattern;
    private BiFunction<MatchResult, List<O>, O> handler;


    @Override
    public O handle(MatchResult match, List<O> children) {
        return handler.apply(match, children);
    }


    /* Static initializers */

    public static <O> Rule<O> rule(String regex, BiFunction<MatchResult, List<O>, O> handler) {
        return rule(Pattern.compile(regex), handler);
    }

    public static <O> Rule<O> rule(Pattern pattern, BiFunction<MatchResult, List<O>, O> handler) {
        return new RegexRule<>(pattern, handler);
    }

    public static <O> Rule<O> rule(String regex, Function<List<O>, O> handler) {
        return rule(Pattern.compile(regex), handler);
    }

    public static <O> Rule<O> rule(Pattern pattern, Function<List<O>, O> handler) {
        return new RegexRule<>(pattern, (match, childer) -> handler.apply(childer));
    }
}
