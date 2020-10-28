# Regex Parser

A simple but powerful Parser implementation written in Java.

Uses Java Regexs, combined with Lambdas and a pinch of Generics,
to allow for easy definitions of grammars.

## Example usage

A very simple example:
```java
Parser parser = new RegexDownstrippingParser<String>(asList(
        rule("\\d+", (match, children) -> "int " + match.group()),
        rule("\\+(.*)", (match, children) -> "positive " + children.get(0)),
        rule("\\-(.*)", (match, children) -> "negative " + children.get(0))
));

assertThat(parser.parse("123"), is("int 123"));
assertThat(parser.parse("+456"), is("positive int 456"));
assertThat(parser.parse("-789"), is("negative int 789"));
```

An example with groups:
```java
Parser parser = new RegexDownstrippingParser<String>(asList(
        rule("(\\w+)(\\[.*\\])",
                (match, children) -> "" + children.get(0) + children.get(1)),
        rule("\\w+",
                (match, children) -> String.format("tag '%s'", match.group())),
        groupMatchingRule("\\[", "\\]",
                (match, children) -> String.join(",", children)),
        rule("\\[(\\w+)\\]",
                (match, children) -> String.format(" with attribute '%s' present", match.group(1))),
        rule("\\[(\\w+)\\*=(['\"])?(.*)\\2\\]",
                (match, children) -> String.format(" with attribute '%s' containing '%s'", match.group(1), match.group(3)))
));

assertThat(parser.parse("a"),
        is("tag 'a'"));
assertThat(parser.parse("a[target]"),
        is("tag 'a' with attribute 'target' present"));
assertThat(parser.parse("a[target][href]"),
        is("tag 'a' with attribute 'target' present, with attribute 'href' present"));
assertThat(parser.parse("a[target][href*='[127.0.0.1]']"),
        is("tag 'a' with attribute 'target' present, with attribute 'href' containing '[127.0.0.1]'"));
```
