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

import lombok.AllArgsConstructor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aanchev.parser.LazyList.lazyList;
import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PROTECTED;

@AllArgsConstructor(access = PROTECTED)
public class GroupRule<O> implements Rule<O> {

    private static Pattern PATTERN_ANYTHING = Pattern.compile(".*+", Pattern.DOTALL);

    private Pattern opening;
    private Pattern closing;
    private Handler<O> handler;

    @Override
    public Pattern pattern() {
        return PATTERN_ANYTHING;
    }

    @Override
    public O handle(MatchResult match, List<O> nodes, Parser parser) {
        CharSequence input = match.group();

        List<Pair<Integer, Integer>> groups = findTopLevelGroups(input, opening, closing);

        // If no groups found, this rule should not match
        if (groups.size() == 0) {
            return null;
        }

        // If only one group was found, but it was the entire input, this rule should not match
        if (groups.size() == 1
                && groups.get(0).getKey() == 0
                && groups.get(0).getValue() == input.length()
        ) {
            return null;
        }

        // Try to parse the groups, but failures should just make this rule not match so other rules can have a go
        try {
            return handler.handle(match, lazyList(groups.size(),
                    i -> parser.parse(input, groups.get(i).getKey(), groups.get(i).getValue())), parser);
        } catch (ParseException e) {
            return null;
        }
    }


    /* Functionality */

    public static List<Pair<Integer, Integer>> findTopLevelGroups(CharSequence input, Pattern opening, Pattern closing) {
        return findTopLevelGroups(input, opening, closing, true);
    }

    public static List<Pair<Integer, Integer>> findTopLevelGroups(CharSequence input, Pattern opening, Pattern closing, boolean includeBoundaries) {
        List<Integer> openings = new LinkedList<>();
        List<Integer> closings = new LinkedList<>();

        Matcher openingMatcher = opening.matcher(input);
        while (openingMatcher.find()) {
            openings.add(includeBoundaries ? openingMatcher.start() : openingMatcher.end());
        }

        Matcher closingMatcher = closing.matcher(input);
        while (closingMatcher.find()) {
            closings.add(includeBoundaries ? closingMatcher.end() : closingMatcher.start());
        }

        if (openings.size() != closings.size()) {
            throw new ParseException("Unbalanced expression!");
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

            while (itOpenings.hasNext() && (o = itOpenings.next()) < threshold) {
                c = itClosings.next();
            }

            final int groupEnd = c;
            groups.add(new Pair<>(groupStart, groupEnd));
        } while (itClosings.hasNext());

        coalesceOverlappingGroups(groups);

        return groups;
    }

    private static void coalesceOverlappingGroups(List<Pair<Integer, Integer>> groups) {
        if (groups.size() < 2) {
            return;
        }

        Iterator<Pair<Integer, Integer>> itGroups = groups.iterator();

        Pair<Integer, Integer> previousGroup = itGroups.next();
        while (itGroups.hasNext()) {
            Pair<Integer, Integer> nextGroup = itGroups.next();
            if (nextGroup.equals(previousGroup)) {
                itGroups.remove();
            } else {
                previousGroup = nextGroup;
            }
        }
    }

    @lombok.Value
    public static class Pair<L, R> {
        private final L key;
        private final R value;
    }


    /* Static constructors */

    public static <O> Rule<O> groupRule(String openingRegex, String closingRegex, Function<MatchResult, O> handler) {
        return groupRule(Pattern.compile(openingRegex), Pattern.compile(closingRegex), handler);
    }

    public static <O> Rule<O> groupRule(String openingRegex, String closingRegex, BiFunction<MatchResult, List<O>, O> handler) {
        return groupRule(Pattern.compile(openingRegex), Pattern.compile(closingRegex), handler);
    }

    public static <O> Rule<O> groupRule(String openingRegex, String closingRegex, Handler<O> handler) {
        return groupRule(Pattern.compile(openingRegex), Pattern.compile(closingRegex), handler);
    }

    public static <O> Rule<O> groupRule(Pattern openingRegex, Pattern closingRegex, Function<MatchResult, O> handler) {
        return groupRule(openingRegex, closingRegex, (match, nodes, parser) -> handler.apply(match));
    }

    public static <O> Rule<O> groupRule(Pattern openingRegex, Pattern closingRegex, BiFunction<MatchResult, List<O>, O> handler) {
        return groupRule(openingRegex, closingRegex, (match, nodes, parser) -> handler.apply(match, nodes));
    }

    public static <O> Rule<O> groupRule(Pattern openingRegex, Pattern closingRegex, Handler<O> handler) {
        return new GroupRule<>(openingRegex, closingRegex, handler);
    }

}
