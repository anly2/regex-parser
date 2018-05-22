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

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
