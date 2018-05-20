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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShallowRule<O> extends RegexRule<O> {

    protected ShallowRule(Pattern pattern, Function<Matcher, Function<List<O>, O>> earlyHandler) {
        super(pattern, earlyHandler);
    }

    @Override
    public boolean shouldIgnoreGroup(int groupIndex) {
        return true;
    }


    public static <O> Rule<O> shallowRule(String regex, Function<List<O>, O> lateHandler) {
        return shallowRule(Pattern.compile(regex), matcher -> lateHandler);
    }

    public static <O> Rule<O> shallowRule(String regex, BiFunction<Matcher, List<O>, O> handler) {
        return shallowRule(Pattern.compile(regex), matcher -> children -> handler.apply(matcher, children));
    }

    public static <O> Rule<O> shallowRule(Pattern pattern, Function<Matcher, Function<List<O>, O>> earlyHandler) {
        return new ShallowRule<>(pattern, earlyHandler);
    }
}
