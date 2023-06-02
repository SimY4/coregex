/*
 * Copyright 2021-2023 Alex Simkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.simy4.coregex.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

/**
 * Coregex parser.
 *
 * @author Alex Simkin
 * @since 0.1.0
 */
public final class CoregexParser {
  private static final CoregexParser instance = new CoregexParser();

  /** @return coregex parser singleton instance. */
  public static CoregexParser getInstance() {
    return instance;
  }

  /**
   * Constructs {@link Coregex} from provided {@link Pattern} instance.
   *
   * @param pattern regular expression to parse.
   * @return parsed coregex instance.
   * @throws UnsupportedOperationException if provided pattern constructs are not yet supported.
   */
  public Coregex parse(Pattern pattern) {
    String regex = pattern.pattern();
    int flags = pattern.flags();
    if (regex.isEmpty()) {
      return Coregex.empty();
    } else if (0 != (Pattern.LITERAL & flags)) {
      return new Coregex.Literal(regex, flags);
    }
    Context ctx = new Context(regex, flags);
    Coregex coregex = RE(ctx);
    if (ctx.hasMoreElements()) {
      coregex = ctx.error("EOL");
    }
    return coregex.simplify();
  }

  /*
   * <pre>{@code
   * re ::= simpleRE, {'|', simpleRE}
   * }</pre>
   */
  private Coregex RE(Context ctx) {
    Coregex re = simpleRE(ctx);
    if ('|' == ctx.peek()) {
      List<Coregex> union = new ArrayList<>();
      while ('|' == ctx.peek()) {
        ctx.match('|');
        union.add(simpleRE(ctx));
      }
      re = new Coregex.Union(re, union.toArray(new Coregex[0]));
    }
    return re;
  }

  /*
   * <pre>{@code
   * simpleRE ::= basicRE, {basicRE}
   * }</pre>
   */
  private Coregex simpleRE(Context ctx) {
    Coregex simpleRE = basicRE(ctx);
    if (ctx.hasMoreElements() && '|' != ctx.peek() && ')' != ctx.peek()) {
      List<Coregex> concatenation = new ArrayList<>();
      while (ctx.hasMoreElements() && '|' != ctx.peek() && ')' != ctx.peek()) {
        concatenation.add(basicRE(ctx));
      }
      simpleRE = new Coregex.Concat(simpleRE, concatenation.toArray(new Coregex[0]));
    }
    return simpleRE;
  }

  /*
   * <pre>{@code
   * basicRE ::= elementaryRE, ['+' | '*' | '?' | '+', '+' | '?', '?' | '{', times, '}' | '{', times, ',', '}' | '{', times, ',', times, '}']
   * times ::= digit, {digit}
   * }</pre>
   */
  private Coregex basicRE(Context ctx) {
    Coregex basicRE = elementaryRE(ctx);
    int quantifierMin;
    int quantifierMax;
    switch (ctx.peek()) {
      case '+':
        ctx.match('+');
        quantifierMin = 1;
        quantifierMax = -1;
        break;
      case '*':
        ctx.match('*');
        quantifierMin = 0;
        quantifierMax = -1;
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
          quantifierMax = end.isEmpty() ? -1 : Integer.parseInt(end);
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
    Coregex.Quantified.Type type;
    switch (ctx.peek()) {
      case '?':
        ctx.match('?');
        type = Coregex.Quantified.Type.RELUCTANT;
        break;
      case '+':
        ctx.match('+');
        type = Coregex.Quantified.Type.POSSESSIVE;
        break;
      default:
        type = Coregex.Quantified.Type.GREEDY;
        break;
    }
    return basicRE.quantify(quantifierMin, quantifierMax, type);
  }

  /*
   * <pre>{@code
   * elementaryRE ::= '.' | set | group | '^' | '$' | quoted | '\', metachar | literal
   * }</pre>
   */
  private Coregex elementaryRE(Context ctx) {
    Coregex elementaryRE;
    char ch = ctx.peek();
    switch (ch) {
      case '.':
        ctx.match('.');
        elementaryRE = Coregex.any(ctx.flags);
        break;
      case '[':
        elementaryRE = new Coregex.Set(set(ctx));
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
      case '\\':
        ctx.match('\\');
        ch = ctx.peek();
        elementaryRE = 'Q' == ch ? quoted(ctx) : new Coregex.Set(metachar(ctx));
        break;
      default:
        elementaryRE = literal(ctx);
        break;
    }
    return elementaryRE;
  }

  /*
   * <pre>{@code
   * quoted ::= '\', 'Q', ? quoted ?, '\', 'E'
   * }</pre>
   */
  private Coregex.Literal quoted(Context ctx) {
    ctx.match('Q');
    StringBuilder literal = new StringBuilder();
    do {
      literal.append(ctx.takeWhile(ch -> '\\' != ch));
      ctx.match('\\');
    } while ('E' != ctx.peek() && (literal.append('\\') != null));
    ctx.match('E');
    return new Coregex.Literal(literal.toString());
  }

  /*
   * <pre>{@code
   * literal ::= {? not metachar not followed by quantifier ?}
   * }</pre>
   */
  private Coregex literal(Context ctx) {
    char ch;
    if (ctx.hasMoreElements() && !isREMetachar(ch = ctx.peek())) {
      StringBuilder literal = new StringBuilder();
      ctx.match(ch);
      if ('#' == ch && 0 != (Pattern.COMMENTS & ctx.flags)) {
        ctx.takeWhile(c -> '\n' != c && '\r' != c);
      } else if (!isWhitespace(ch) || 0 == (Pattern.COMMENTS & ctx.flags)) {
        literal.append(ch);
      }
      loop:
      while (ctx.hasMoreElements() && !isREMetachar(ch = ctx.peek())) {
        if ('#' == ch && 0 != (Pattern.COMMENTS & ctx.flags)) {
          ctx.takeWhile(c -> '\n' != c && '\r' != c);
          continue;
        }
        char next = ctx.peek(1);
        switch (next) {
          case '*':
          case '+':
          case '?':
          case '{':
            break loop;
          default:
            ctx.match(ch);
            if (!isWhitespace(ch) || 0 == (Pattern.COMMENTS & ctx.flags)) {
              literal.append(ch);
            }
        }
      }
      return new Coregex.Literal(literal.toString(), ctx.flags);
    } else {
      return Coregex.empty();
    }
  }

  /*
   * <pre>{@code
   * set ::= '[', [ '^' ], { set-item }, ']'
   * }</pre>
   */
  private Set set(Context ctx) {
    ctx.match('[');
    boolean negated = false;
    if ('^' == ctx.peek()) {
      ctx.match('^');
      negated = true;
    }
    Set.Builder set = Set.builder(ctx.flags);
    setItem(set, ctx);
    if (']' != ctx.peek()) {
      while (']' != ctx.peek()) {
        setItem(set, ctx);
      }
    }
    ctx.match(']');
    return (negated ? set.negate() : set).build();
  }

  /*
   * <pre>{@code
   * set-item ::= set | range | '\', metachar | single
   * range    ::= single, '-', single
   * single   ::= ? a character ?
   * }</pre>
   */
  @SuppressWarnings("fallthrough")
  private void setItem(Set.Builder set, Context ctx) {
    char ch = ctx.peek();
    switch (ch) {
      case '[':
        set.set(set(ctx));
        break;
      case '-':
        ctx.match('-');
        set.single('-');
        break;
      case '\\':
        ctx.match('\\');
        set.set(metachar(ctx));
        break;
      default:
        ctx.match(ch);
        if ('-' == ctx.peek()) {
          ctx.match('-');
          char end = ctx.peek();
          switch (end) {
            case ']':
              set.set(ch, '-');
              break;
            case '\\':
              ctx.match('\\');
              end = ctx.peek();
              // fall through
            default:
              ctx.match(end);
              set.range(ch, end);
              break;
          }
        } else {
          set.single(ch);
        }
    }
  }

  /*
   * <pre>{@code
   * group ::= '(', [ '?', ( ':' | '>' | '=' | '!' | '<', [ '=' | '!' | literal, '>' ] | flags ) ], re, ')'
   * }</pre>
   */
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
          group = ctx.unsupported("look-aheads are not supported");
          break;
        case '<':
          ctx.match('<');
          switch (ctx.peek()) {
            case '=':
            case '!':
              group = ctx.unsupported("look-behinds are not supported");
              break;
            default:
              ctx.takeWhile(ch -> '>' != ch);
              ctx.match('>');
              group = RE(ctx);
              break;
          }
          break;
        case '-':
          ctx.match('-');
          int flags = flags(ctx);
          ctx.flags &= ~flags;
          group = Coregex.empty();
          break;
        default:
          ctx.flags |= flags(ctx);
          group = Coregex.empty();
          break;
      }
    } else {
      group = RE(ctx);
    }
    ctx.match(')');
    return group;
  }

  /*
   * <pre>{@code
   * falgs ::= 'd' | 'i' | 'm' | 's' | 'u' | 'U' | 'x'
   * }</pre>
   */
  private int flags(Context ctx) {
    int flags = 0;
    char ch = ctx.peek();
    do {
      switch (ch) {
        case 'd':
          ctx.match('d');
          flags |= Pattern.UNIX_LINES;
          break;
        case 'i':
          ctx.match('i');
          flags |= Pattern.CASE_INSENSITIVE;
          break;
        case 'm':
          ctx.match('m');
          flags |= Pattern.MULTILINE;
          break;
        case 's':
          ctx.match('s');
          flags |= Pattern.DOTALL;
          break;
        case 'u':
          ctx.match('u');
          flags |= Pattern.UNICODE_CASE;
          break;
        case 'U':
          ctx.match('U');
          flags |= Pattern.UNICODE_CHARACTER_CLASS;
          break;
        case 'x':
          ctx.match('x');
          flags |= Pattern.COMMENTS;
          break;
        default:
          return ctx.unsupported("unknown flag");
      }
    } while (')' != (ch = ctx.peek()));
    return flags;
  }

  /*
   * <pre>{@code
   * metachar ::= 't' | 'r' | 'n' | 'd' | 'D' | 'w' | 'W' | 's' | 'p', '{', ? posix ?, '}' | 'S' | 'b' | 'B' | 'A' | 'G' | 'z' | 'k' | digit | single
   * }</pre>
   */
  private Set metachar(Context ctx) {
    char ch = ctx.peek();
    Set.Builder metachar = Set.builder();
    switch (ch) {
      case 't':
        ctx.match('t');
        metachar.single('\t');
        break;
      case 'r':
        ctx.match('r');
        metachar.single('\r');
        break;
      case 'n':
        ctx.match('n');
        metachar.single('\n');
        break;
      case 'd':
        ctx.match('d');
        metachar.range('0', '9');
        break;
      case 'D':
        ctx.match('D');
        metachar.range('0', '9').negate();
        break;
      case 'w':
        ctx.match('w');
        metachar.range('0', '9').range('a', 'z').range('A', 'Z').single('_');
        break;
      case 'W':
        ctx.match('W');
        metachar.range('0', '9').range('a', 'z').range('A', 'Z').single('_').negate();
        break;
      case 's':
        ctx.match('s');
        metachar.set(' ', '\t');
        break;
      case 'p':
        ctx.match('p');
        ctx.match('{');
        String posix = ctx.takeWhile(pos -> '}' != pos);
        switch (posix) {
          case "Lower":
          case "javaLowerCase":
            metachar.range('a', 'z');
            break;
          case "Upper":
          case "javaUpperCase":
            metachar.range('A', 'Z');
            break;
          case "ASCII":
            metachar.range(Character.MIN_VALUE, (char) 0x7F);
            break;
          case "Digit":
            metachar.range('0', '9');
            break;
          case "XDigit":
            metachar.range('0', '9').range('a', 'f').range('A', 'F');
            break;
          case "Alpha":
            metachar.range('a', 'z').range('A', 'Z');
            break;
          case "Alnum":
            metachar.range('a', 'z').range('A', 'Z').range('0', '9');
            break;
          case "Punct":
            metachar.set(
                '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':',
                ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~');
            break;
          case "Graph":
          case "Print":
            metachar
                .range('a', 'z')
                .range('A', 'Z')
                .range('0', '9')
                .set(
                    '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':',
                    ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}',
                    '~');
            break;
          case "Blank":
          case "Space":
          case "javaWhitespace":
            metachar.set(' ', '\t');
            break;
          default:
            ctx.unsupported("posix char class \\" + posix + " is not supported");
            break;
        }
        ctx.match('}');
        break;
      case 'Q':
        for (char quoted : quoted(ctx).literal().toCharArray()) {
          metachar.single(quoted);
        }
        break;
      case 'S':
      case 'b':
      case 'B':
      case 'A':
      case 'G':
      case 'Z':
      case 'z':
      case 'k':
        ctx.unsupported("metacharacter \\" + ch + " is not supported");
        break;
      default:
        if (isDigit(ch)) {
          ctx.unsupported("metacharacter \\" + ch + " is not supported");
        } else {
          // escaped metacharacter
          ctx.match(ch);
          metachar.single(ch);
        }
        break;
    }
    return metachar.build();
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

  /**
   *
   *
   * <pre>{@code
   * digit ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
   * }</pre>
   */
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

  private boolean isWhitespace(int ch) {
    switch (ch) {
      case ' ':
      case '\t':
      case '\f':
      case '\r':
      case '\n':
        return true;
      default:
        return false;
    }
  }

  private static final class Context {
    private final String regex;
    private int flags;
    private int cursor;

    private Context(String regex, int flags) {
      this.regex = regex;
      this.flags = flags;
    }

    private boolean hasMoreElements() {
      return cursor < regex.length();
    }

    private char peek() {
      return peek(0);
    }

    private char peek(int i) {
      if (regex.length() <= cursor + i) {
        return (char) -1;
      }
      return regex.charAt(cursor + i);
    }

    private void match(char ch) {
      if (ch != peek()) {
        error(String.valueOf(ch));
      }
      cursor++;
    }

    private String takeWhile(IntPredicate charPredicate) {
      if (charPredicate.test(peek())) {
        int start = cursor++;
        while (hasMoreElements() && charPredicate.test(peek())) {
          cursor++;
        }
        return regex.substring(start, cursor);
      } else {
        return "";
      }
    }

    private <T> T error(String expected) {
      char[] cursor = new char[this.cursor];
      Arrays.fill(cursor, ' ');
      cursor[cursor.length - 1] = '^';
      String actual = hasMoreElements() ? String.valueOf(regex.charAt(this.cursor)) : "<EOL>";
      String message =
          String.join(
              System.lineSeparator(),
              "Unable to parse regex:",
              regex,
              new String(cursor),
              "Expected: " + expected + " Actual: " + actual);
      throw new IllegalArgumentException(message);
    }

    private <T> T unsupported(String reason) {
      char[] cursor = new char[this.cursor];
      Arrays.fill(cursor, ' ');
      cursor[cursor.length - 1] = '^';
      String message =
          String.join(
              System.lineSeparator(),
              "Unable to parse regex:",
              regex,
              new String(cursor),
              "Reason: " + reason);
      throw new UnsupportedOperationException(message);
    }
  }
}
