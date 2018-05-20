# Regex Parser

A simple but powerful Parser implementation using Java Regexs.

## Example usage

```java
Parser parser = new RegexDownstrippingParser<String>(asList(
        rule("\\d+", (matcher, children) -> "int " + matcher.group()),
        rule("\\+(.*)", (matcher, children) -> "positive " + children.get(0)),
        rule("\\-(.*)", (matcher, children) -> "negative " + children.get(0))
));

assertThat(parser.parse("123"), is("int 123"));
assertThat(parser.parse("+456"), is("positive int 456"));
assertThat(parser.parse("-789"), is("negative int 789"));
```