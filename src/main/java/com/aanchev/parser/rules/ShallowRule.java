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

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static lombok.AccessLevel.PROTECTED;

@AllArgsConstructor(access = PROTECTED)
public class ShallowRule<O> implements Rule<O> {

    private Rule<O> body;

    /* Decorated functionality */

    @Override
    public boolean shouldIgnoreGroup(int groupIndex) {
        return true;
    }

    /* Delegation */

    @Override
    public Pattern pattern() {
        return body.pattern();
    }

    @Override
    public MatchResult handleMatch(MatchResult match) {
        return body.handleMatch(match);
    }

    @Override
    public O handle(MatchResult match, List<O> children) {
        return body.handle(match, children);
    }


    /* Static initializers */

    public static <O> Rule<O> shallow(Rule<O> body) {
        return new ShallowRule<>(body);
    }
}
