package com.github.simy4.coregex.core;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

public final class CoregexParser {

  public Coregex parse(Pattern pattern) {
    String regex = pattern.pattern();
    if (regex.isEmpty()) {
      return Coregex.empty();
    }
    Context ctx = new Context(regex);
    Coregex regexGen = RE(ctx);
    if (ctx.hasMoreElements()) {
      regexGen = ctx.error("EOL");
    }
    return regexGen;
  }

  private Coregex RE(Context ctx) {
    Coregex re = simpleRE(ctx);
    if (ctx.hasMoreElements() && '|' == ctx.peek()) {
      List<Coregex> union = new ArrayList<>();
      while (ctx.hasMoreElements() && '|' == ctx.peek()) {
        ctx.match('|');
        union.add(simpleRE(ctx));
      }
      re = Coregex.union(re, union.toArray(new Coregex[0]));
    }
    return re;
  }

  private Coregex simpleRE(Context ctx) {
    Coregex simpleRE = basicRE(ctx);
    if (ctx.hasMoreElements() && '|' != ctx.peek() && ')' != ctx.peek()) {
      List<Coregex> concatenation = new ArrayList<>();
      while (ctx.hasMoreElements() && '|' != ctx.peek() && ')' != ctx.peek()) {
        concatenation.add(basicRE(ctx));
      }
      simpleRE = Coregex.concat(simpleRE, concatenation.toArray(new Coregex[0]));
    }
    return simpleRE;
  }

  private Coregex basicRE(Context ctx) {
    Coregex basicRE = elementaryRE(ctx);
    if (!ctx.hasMoreElements()) {
      return basicRE;
    }
    int quantifierMin;
    int quantifierMax;
    switch (ctx.peek()) {
      case '+':
        ctx.match('+');
        quantifierMin = 1;
        quantifierMax = 32;
        break;
      case '*':
        ctx.match('*');
        quantifierMin = 0;
        quantifierMax = 32;
        break;
      case '?':
        ctx.match('?');
        quantifierMin = 0;
        quantifierMax = 1;
        break;
      case '{':
        ctx.match('{');
        String times = ctx.takeWhile(this::isDigit);
        quantifierMin = times.isEmpty() ? 0 : Integer.parseInt(times);
        if (',' == ctx.peek()) {
          ctx.match(',');
          String end = ctx.takeWhile(this::isDigit);
          quantifierMax = end.isEmpty() ? 32 : Integer.parseInt(end);
        } else {
          quantifierMax = quantifierMin;
        }
        ctx.match('}');
        break;
      default:
        quantifierMin = 1;
        quantifierMax = 1;
        break;
    }
    return basicRE.quantify(quantifierMin, quantifierMax);
  }

  private Coregex elementaryRE(Context ctx) {
    Coregex elementaryRE;
    switch (ctx.peek()) {
      case '.':
        ctx.match('.');
        elementaryRE = Coregex.set(SetItem.any());
        break;
      case '[':
        elementaryRE = Coregex.set(set(ctx));
        break;
      case '(':
        elementaryRE = group(ctx);
        break;
      case '^':
        ctx.match('^');
        elementaryRE = Coregex.empty();
        break;
      case '$':
        ctx.match('$');
        elementaryRE = Coregex.empty();
        break;
      default:
        elementaryRE = Coregex.set(aChar(ctx, this::isREMetachar));
        break;
    }
    return elementaryRE;
  }

  private SetItem set(Context ctx) {
    ctx.match('[');
    boolean negated = false;
    if ('^' == ctx.peek()) {
      ctx.match('^');
      negated = true;
    }
    SetItem setItem = setItem(ctx);
    if (']' != ctx.peek()) {
      List<SetItem> setItems = new ArrayList<>();
      while (']' != ctx.peek()) {
        setItems.add(setItem(ctx));
      }
      setItem = SetItem.union(setItem, setItems.toArray(new SetItem[0]));
    }
    ctx.match(']');
    return negated ? setItem.negate() : setItem;
  }

  private SetItem setItem(Context ctx) {
    SetItem setItem;
    if ('[' == ctx.peek()) {
      setItem = set(ctx);
    } else {
      setItem = aChar(ctx, this::isSetMetachar);
      if ('-' == ctx.peek()) {
        ctx.match('-');
        char start = setItem.generate(0L);
        char end = aChar(ctx, this::isSetMetachar).generate(0L);
        setItem = SetItem.range(start, end);
      }
    }
    return setItem;
  }

  private Coregex group(Context ctx) {
    ctx.match('(');
    Coregex group;
    if ('?' == ctx.peek()) {
      ctx.match('?');
      switch (ctx.peek()) {
        case ':':
          ctx.match(':');
          group = RE(ctx);
          break;
        case '>':
          ctx.match('>');
          group = RE(ctx);
          break;
        case '=':
        case '!':
          throw new IllegalArgumentException("Regex not supported: look-aheads are not supported");
        case '<':
          ctx.match('<');
          switch (ctx.peek()) {
            case '=':
            case '!':
              throw new IllegalArgumentException(
                  "Regex not supported: look-behinds are not supported");
            default:
              ctx.takeWhile(ch -> '>' != ch);
              ctx.match('>');
              group = RE(ctx);
              break;
          }
          break;
        default:
          throw new IllegalArgumentException("Regex not supported: inline option is not supported");
      }
    } else {
      group = RE(ctx);
    }
    ctx.match(')');
    return group;
  }

  private SetItem aChar(Context ctx, IntPredicate isMetachar) {
    char ch = ctx.peek();
    SetItem aChar;
    if ('\\' == ch) {
      ctx.match('\\');
      ch = ctx.peek();
      if (isMetachar.test(ch)) {
        ctx.match(ch);
        aChar = SetItem.set(ch);
      } else {
        aChar = metachar(ctx);
      }
    } else {
      ctx.match(ch);
      aChar = SetItem.set(ch);
    }
    return aChar;
  }

  private SetItem metachar(Context ctx) {
    char ch = ctx.peek();
    SetItem charSet;
    switch (ch) {
      case 't':
        ctx.match('t');
        charSet = SetItem.set('\t');
        break;
      case 'r':
        ctx.match('r');
        charSet = SetItem.set('\r');
        break;
      case 'n':
        ctx.match('n');
        charSet = SetItem.set('\n');
        break;
      case 'd':
        ctx.match('d');
        charSet = SetItem.range('0', '9');
        break;
      case 'w':
        ctx.match('w');
        charSet =
            SetItem.union(
                SetItem.range('0', '9'),
                SetItem.range('a', 'z'),
                SetItem.range('A', 'Z'),
                SetItem.set('_'));
        break;
      case 's':
        ctx.match('s');
        charSet = SetItem.set(' ', '\t', '\r', '\n');
        break;
      default:
        throw new UnsupportedOperationException(
            "Regex not supported: metacharacter \\" + ch + " is not supported");
    }
    return charSet;
  }

  private boolean isREMetachar(int ch) {
    switch (ch) {
      case '\\':
      case '|':
      case '*':
      case '+':
      case '?':
      case '.':
      case '{':
      case '[':
      case '^':
      case '$':
      case '(':
      case ')':
        return true;
      default:
        return false;
    }
  }

  private boolean isSetMetachar(int ch) {
    switch (ch) {
      case '\\':
      case ']':
      case '-':
        return true;
      default:
        return false;
    }
  }

  private boolean isDigit(int ch) {
    switch (ch) {
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        return true;
      default:
        return false;
    }
  }

  private static final class Context {
    private final String regex;
    private int cursor;

    private Context(String regex) {
      this.regex = regex;
    }

    private boolean hasMoreElements() {
      return cursor < regex.length();
    }

    private char peek() {
      if (!hasMoreElements()) {
        throw new NoSuchElementException("No more elements");
      }
      return regex.charAt(cursor);
    }

    private void match(char ch) {
      if (ch != peek()) {
        throw new UnsupportedOperationException("Regex not supported: " + regex);
      }
      cursor++;
    }

    private String takeWhile(IntPredicate charPredicate) {
      if (hasMoreElements() && charPredicate.test(peek())) {
        int start = cursor++;
        while (hasMoreElements() && charPredicate.test(peek())) {
          cursor++;
        }
        return regex.substring(start, cursor);
      } else {
        return "";
      }
    }

    private <T> T error(String error) {
      throw new IllegalArgumentException(error);
    }
  }

}
