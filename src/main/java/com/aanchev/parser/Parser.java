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

import java.util.function.Function;

public interface Parser {
    <E> E parse(CharSequence input);

    default <E> E parse(CharSequence input, int start, int end) {
        return this.parse(input.subSequence(start, end));
    }


    static <R> Parser parser(Function<CharSequence, R> impl) {
        return new Parser() {
            @Override
            @SuppressWarnings("unchecked")
            public <E> E parse(CharSequence input) {
                return (E) impl.apply(input);
            }
        };
    }
}

