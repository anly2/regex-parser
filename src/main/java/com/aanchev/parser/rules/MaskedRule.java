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

import java.util.BitSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaskedRule<O> extends RegexRule<O> {

    private BitSet ignoredGroups;

    protected MaskedRule(Pattern pattern, BitSet ignoredGroups, Function<Matcher, Function<List<O>, O>> earlyHandler) {
        super(pattern, earlyHandler);
        this.ignoredGroups = ignoredGroups;
    }

    @Override
    public boolean shouldIgnoreGroup(int groupIndex) {
        return ignoredGroups.get(groupIndex);
    }


    public static <O> Rule<O> maskedRule(String regex, Iterable<Integer> ignoredGroups, Function<List<O>, O> lateHandler) {
        BitSet bitmask = new BitSet();
        for (Integer g : ignoredGroups) {
            bitmask.set(g);
        }

        return maskedRule(regex, bitmask, lateHandler);
    }

    public static <O> Rule<O> maskedRule(String regex, BitSet ignoredGroups, Function<List<O>, O> lateHandler) {
        return new MaskedRule<>(Pattern.compile(regex), ignoredGroups, matcher -> lateHandler);
    }

    public static <O> Rule<O> maskedRule(String regex, Iterable<Integer> ignoredGroups, BiFunction<Matcher, List<O>, O> handler) {
        BitSet bitmask = new BitSet();
        for (Integer g : ignoredGroups) {
            bitmask.set(g);
        }

        return maskedRule(regex, bitmask, handler);
    }

    public static <O> Rule<O> maskedRule(String regex, BitSet ignoredGroups, BiFunction<Matcher, List<O>, O> handler) {
        return new MaskedRule<>(Pattern.compile(regex), ignoredGroups, matcher -> children -> handler.apply(matcher, children));
    }
}
