package com.aanchev.parser;

import java.util.List;
import java.util.regex.MatchResult;

@FunctionalInterface
public interface Handler<O> {
    O handle(MatchResult match, List<O> nodes, Parser parser);
}
