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

import javafx.util.Pair;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

public class GroupRule<O> implements Rule<O> {

    private static Pattern PATTERN_ANYTHING = Pattern.compile(".*+", Pattern.DOTALL);

    @Getter
    @Accessors(fluent = true)
    private Function<Matcher, Function<List<O>, O>> earlyHandler;
    private Pattern stride;

    protected GroupRule(String opening, String closing, Function<Matcher, Function<List<O>, O>> earlyHandler) {
        this.earlyHandler = prepare(earlyHandler);
        this.stride = toStride(opening, closing);
    }

    private Function<Matcher, Function<List<O>, O>> prepare(Function<Matcher, Function<List<O>, O>> earlyHandler) {
        return matcher -> {
            matcher = reshape(matcher);
            if (matcher == null) {
                return null;
            }
            return earlyHandler.apply(matcher);
        };
    }

    protected Matcher reshape(Matcher matcher) {
        Matcher reshaper = stride.matcher(matcher.group());
        //TODO: rename stride
        //TODO: ignore nested groups
        StringBuilder templateSB = new StringBuilder(matcher.group().length());
        while (reshaper.find()) {
            if (reshaper.start() > 0) {
                templateSB.append(".{").append(reshaper.start()).append("}");
            }

            if (reshaper.group("opening") != null) {
                templateSB.append(".{").append(reshaper.group("opening").length()).append("}");
                templateSB.append("(");
            }
            if (reshaper.group("closing") != null) {
                templateSB.append(")");
                templateSB.append(".{").append(reshaper.group("closing").length()).append("}");
            }
        }
        Pattern template = Pattern.compile(templateSB.toString());

        //TODO: reset with new input (
        matcher.region(matcher.regionStart(), matcher.regionEnd()); //reset
        if (!matcher.usePattern(template).matches()) {
            return null;
        }

        return matcher;
    }

    protected static Pattern toStride(String opening, String closing) {
        return Pattern.compile(String.format("(?<opening>%s)|(?<closing>%s)", opening, closing));
    }


    /* Functionality */

    public static List<Pair<Integer, Integer>> findTopLevelGroups(CharSequence input, Pattern opening, Pattern closing) {
        List<Integer> openings = new LinkedList<>();
        List<Integer> closings = new LinkedList<>();

        Matcher openingMatcher = opening.matcher(input);
        while (openingMatcher.find()) {
            openings.add(openingMatcher.end());
        }

        Matcher closingMatcher = closing.matcher(input);
        while (closingMatcher.find()) {
            closings.add(closingMatcher.start());
        }

        return getTopLevelGroups(openings, closings);
    }

    public static List<Pair<Integer, Integer>> getTopLevelGroups(Iterable<Integer> openings, Iterable<Integer> closings) {
        Iterator<Integer> itOpenings = openings.iterator();
        Iterator<Integer> itClosings = closings.iterator();

        if (!itOpenings.hasNext()) {
            return emptyList();
        }

        List<Pair<Integer, Integer>> groups = new LinkedList<>();

        int o = itOpenings.next();

        do {
            int c = itClosings.next();

            final int groupStart = o;
            final int threshold = c;

            while (itOpenings.hasNext() && (o = itOpenings.next()) <= threshold) {
                c = itClosings.next();
            }

            final int groupEnd = c;
            groups.add(new Pair<>(groupStart, groupEnd));
        } while (itOpenings.hasNext());

        return groups;
    }


    /* Rule contract */

    @Override
    public Pattern pattern() {
        return PATTERN_ANYTHING;
    }

    @Override
    public boolean shouldIgnoreGroup(int groupIndex) {
        return false;
    }


    /* Static constructors */

    public static <O> Rule<O> groupRule(String opening, String closing, Function<Matcher, Function<List<O>, O>> earlyHandler) {
        return new GroupRule<>(Pattern.quote(opening), Pattern.quote(closing), earlyHandler);
    }
}
