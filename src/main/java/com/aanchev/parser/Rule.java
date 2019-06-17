package com.aanchev.parser;

import java.util.regex.Pattern;

public interface Rule<O> extends Handler<O> {
    Pattern pattern();
}
