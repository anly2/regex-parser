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

import com.aanchev.parser.ParseException;
import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aanchev.parser.rules.RegexRule.rule;
import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PROTECTED;

@AllArgsConstructor(access = PROTECTED)
public class GroupRule<O> implements Rule<O> {

    private static Pattern PATTERN_ANYTHING = Pattern.compile(".*+", Pattern.DOTALL);

    private Rule<O> body;
    private Pattern opening;
    private Pattern closing;


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


    /* Decorated Functionality */

    @Override
    public MatchResult handleMatch(MatchResult match) {
        if (match instanceof GroupRule.Match) {
            // A group rule handled the match already, so break an infinite recursion.
            //indicate this rule should not match
            return null;
        }
        try {
            MatchResult groupMatch = matchTopLevelGroups(match.group(), opening, closing);
            if (groupMatch.groupCount() == 0) {
                return null;
            }
            if (groupMatch instanceof GroupRule.Match) {
                ((GroupRule.Match) groupMatch).original = match;
            }
            return body.handleMatch(groupMatch);
        } catch (ParseException e) {
            return null;
        }
    }


    /* Functionality */

    public static MatchResult matchTopLevelGroups(CharSequence input, Pattern opening, Pattern closing) {
        return asMatchResult(input, findTopLevelGroups(input, opening, closing));
    }

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


    /* Custom Match Result implementation */

    public static MatchResult originalMatch(MatchResult groupMatch) {
        if (groupMatch instanceof GroupRule.Match) {
            return ((GroupRule.Match) groupMatch).original;
        }
        throw new IllegalArgumentException("Given match result is not a custom group-matching result.");
    }

    protected static MatchResult asMatchResult(CharSequence input, List<Pair<Integer, Integer>> groups) {
        int[] starts = new int[groups.size() + 1];
        int[] ends = new int[groups.size() + 1];

        starts[0] = 0;
        ends[0] = input.length();

        int i = 1;
        for (Pair<Integer, Integer> group : groups) {
            starts[i] = group.getKey();
            ends[i] = group.getValue();
            i++;
        }

        return new Match(input, starts, ends);
    }

    @RequiredArgsConstructor
    protected static class Match implements MatchResult {
        protected MatchResult original = null;

        @NonNull
        private CharSequence input;
        @NonNull
        private int[] starts;
        @NonNull
        private int[] ends;


        @Override
        public int start() {
            return 0;
        }

        @Override
        public int start(int group) {
            if (group >= starts.length) {
                return -1;
            }
            return starts[group];
        }

        @Override
        public int end() {
            return input.length();
        }

        @Override
        public int end(int group) {
            if (group >= ends.length) {
                return -1;
            }
            return ends[group];
        }

        @Override
        public String group() {
            return input.toString();
        }

        @Override
        public String group(int group) {
            if (group >= starts.length) {
                throw new IndexOutOfBoundsException("No group " + group);
            }
            return input.subSequence(starts[group], ends[group]).toString();
        }

        @Override
        public int groupCount() {
            return starts.length - 1;
        }
    }


    /* Static constructors */

    public static <O> Rule<O> groupMatching(Rule<O> body, String openingRegex, String closingRegex) {
        return groupMatching(openingRegex, closingRegex, body);
    }

    public static <O> Rule<O> groupMatching(Rule<O> body, Pattern opening, Pattern closing) {
        return groupMatching(opening, closing, body);
    }


    public static <O> Rule<O> groupMatching(String openingRegex, String closingRegex, Rule<O> body) {
        return groupMatching(body, Pattern.compile(openingRegex), Pattern.compile(closingRegex));
    }

    public static <O> Rule<O> groupMatching(Pattern opening, Pattern closing, Rule<O> body) {
        return new GroupRule<>(body, opening, closing);
    }


    public static <O> Rule<O> groupMatchingRule(String openingRegex, String closingRegex, BiFunction<MatchResult, List<O>, O> handler) {
        return groupMatchingRule(Pattern.compile(openingRegex), Pattern.compile(closingRegex), handler);
    }

    public static <O> Rule<O> groupMatchingRule(Pattern opening, Pattern closing, BiFunction<MatchResult, List<O>, O> handler) {
        return new GroupRule<>(rule(PATTERN_ANYTHING, handler), opening, closing);
    }
}
