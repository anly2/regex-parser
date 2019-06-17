package com.aanchev.parser;

import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Value
public class SimpleRule<O> implements Rule<O> {
    @Accessors(fluent = true)
    private final Pattern pattern;
    private final Handler<O> handler;

    @Override
    public O handle(MatchResult match, List<O> nodes, Parser parser) {
        return handler.handle(match, nodes, parser);
    }
}
