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
import lombok.experimental.Accessors;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = PROTECTED)
public class RegexRule<O> implements Rule<O> {
    private Pattern pattern;
    private Function<Matcher, Function<List<O>, O>> earlyHandler;

    @Override
    public boolean shouldIgnoreGroup(int groupIndex) {
        return false;
    }

    @Override
    public String toString() {
        return "/" + pattern.pattern() + "/";
    }


    public static <O> Rule<O> rule(String regex, Function<List<O>, O> lateHandler) {
        return rule(Pattern.compile(regex), matcher -> lateHandler);
    }

    public static <O> Rule<O> rule(String regex, BiFunction<Matcher, List<O>, O> handler) {
        return rule(Pattern.compile(regex), matcher -> children -> handler.apply(matcher, children));
    }

    public static <O> Rule<O> rule(Pattern pattern, Function<Matcher, Function<List<O>, O>> earlyHandler) {
        return new RegexRule<>(pattern, earlyHandler);
    }
}
