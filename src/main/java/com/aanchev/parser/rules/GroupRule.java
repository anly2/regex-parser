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


    /* Decorated Functionality */

    @Override
    public MatchResult handleMatch(MatchResult match) {
        try {
            MatchResult groupMatch = matchTopLevelGroups(match.group(), opening, closing);
            return body.handleMatch(groupMatch);
        } catch (ParseException e) {
            return null;
        }
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

    /* Functionality */

    public static MatchResult matchTopLevelGroups(CharSequence input, Pattern opening, Pattern closing) {
        return asMatchResult(input, findTopLevelGroups(input, opening, closing));
    }

    private static MatchResult asMatchResult(CharSequence input, List<Pair<Integer, Integer>> topLevelGroups) {
        //TODO: implement better

        StringBuilder template = new StringBuilder();
        int i = 0;
        for (Pair<Integer, Integer> group : topLevelGroups) {
            template.append(".{")
                    .append(group.getKey() - i)
                    .append("}(")
                    .append(".{")
                    .append(group.getValue() - group.getKey())
                    .append("})");
            i = group.getValue();
        }
        template.append(".{").append(input.length() - i).append("}");

        Matcher matcher = Pattern.compile(template.toString()).matcher(input);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.toMatchResult();
    }


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

            while (itOpenings.hasNext() && (o = itOpenings.next()) <= threshold) {
                c = itClosings.next();
            }

            final int groupEnd = c;
            groups.add(new Pair<>(groupStart, groupEnd));
        } while (itClosings.hasNext());

        return groups;
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
