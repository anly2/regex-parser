package com.aanchev.parser;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class Rules {
    private Rules() {
    }

    public static <O> Rule<O> rule(String regex, Function<MatchResult, O> handler) {
        return new SimpleRule<>(Pattern.compile(regex), (match, nodes, parser) -> handler.apply(match));
    }

    public static <O> Rule<O> rule(String regex, BiFunction<MatchResult, List<O>, O> handler) {
        return new SimpleRule<>(Pattern.compile(regex), (match, nodes, parser) -> handler.apply(match, nodes));
    }

    public static <O> Rule<O> rule(String regex, Handler<O> handler) {
        return new SimpleRule<>(Pattern.compile(regex), handler);
    }


    public static <O> Rule<O> rule(Pattern pattern, Function<MatchResult, O> handler) {
        return new SimpleRule<>(pattern, (match, nodes, parser) -> handler.apply(match));
    }

    public static <O> Rule<O> rule(Pattern pattern, BiFunction<MatchResult, List<O>, O> handler) {
        return new SimpleRule<>(pattern, (match, nodes, parser) -> handler.apply(match, nodes));
    }

    public static <O> Rule<O> rule(Pattern pattern, Handler<O> handler) {
        return new SimpleRule<>(pattern, handler);
    }
}
