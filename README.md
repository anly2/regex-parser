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