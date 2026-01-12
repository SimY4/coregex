/*
 * Copyright 2021-2026 Alex Simkin
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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

  /**
   * @return coregex parser singleton instance.
   */
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
      return Coregex.literal(regex, flags);
    }
    Context ctx = new Context(regex, flags);
    Coregex coregex = RE(ctx);
    if (ctx.hasMoreElements()) {
      coregex = ctx.error("EOL");
    }
    return coregex;
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
   * simpleRE ::= {basicRE}
   * }</pre>
   */
  private Coregex simpleRE(Context ctx) {
    Coregex simpleRE = Coregex.empty();
    if (ctx.hasMoreElements() && '|' != ctx.peek() && ')' != ctx.peek()) {
      simpleRE = basicRE(ctx);
      if (ctx.hasMoreElements() && '|' != ctx.peek() && ')' != ctx.peek()) {
        List<Coregex> concatenation = new ArrayList<>();
        while (ctx.hasMoreElements() && '|' != ctx.peek() && ')' != ctx.peek()) {
          concatenation.add(basicRE(ctx));
        }
        simpleRE = new Coregex.Concat(simpleRE, concatenation.toArray(new Coregex[0]));
      }
    }
    return simpleRE;
  }

  /*
   * <pre>{@code
   * basicRE ::= elementaryRE, [ quantifier, [ '+' | '?' ] ]
   * quantifier ::= '+' | '*' | '?' | range
   * range ::=  '{', numeric, [ ',', [ numeric ] ], '}'
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
        quantifierMin = Math.max(0, numeric(ctx));
        if (',' == ctx.peek()) {
          ctx.match(',');
          quantifierMax = numeric(ctx);
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
   * elementaryRE ::= '.' | set | group | '^' | '$' | '\', quoted | '\', 'A' | '\', 'b' | '\', 'B' | '\', 'R'
   *                      | '\', 'z' | '\', 'Z' | '\', 'G' | '\', 'k', '<', literal ,'>' | '\', numeric
   *                      | '\', metachar | literal
   * }</pre>
   */
  private Coregex elementaryRE(Context ctx) {
    Coregex elementaryRE;
    switch (ctx.peek()) {
      case '.':
        ctx.match('.');
        elementaryRE = Coregex.any(ctx.flags);
        break;
      case '[':
        elementaryRE = set(ctx);
        break;
      case '(':
        elementaryRE = group(ctx);
        break;
      case '^':
        ctx.match('^');
        elementaryRE = new Coregex.Group(Coregex.Group.Type.LOOKBEHIND, Coregex.empty());
        break;
      case '$':
        ctx.match('$');
        elementaryRE = new Coregex.Group(Coregex.Group.Type.LOOKAHEAD, Coregex.empty());
        break;
      case '\\':
        ctx.match('\\');
        char ch = ctx.peek();
        switch (ch) {
          case 'Q':
            elementaryRE = Coregex.literal(quoted(ctx), 0);
            break;
          case 'A':
            ctx.match('A');
            elementaryRE = new Coregex.Group(Coregex.Group.Type.LOOKBEHIND, Coregex.empty());
            break;
          case 'b':
            ctx.match('b');
            elementaryRE = Coregex.wordBoundary(ctx.flags, true);
            break;
          case 'B':
            ctx.match('B');
            elementaryRE = Coregex.wordBoundary(ctx.flags, false);
            break;
          case 'R':
            ctx.match('R');
            elementaryRE =
                new Coregex.Union(
                    Coregex.literal("\r\n", 0),
                    Set.builder()
                        .set('\n', '\u000B', '\u000C', '\r', '\u0085', '\u2028', '\u2029')
                        .build());
            break;
          case 'z':
            ctx.match('z');
            elementaryRE = new Coregex.Group(Coregex.Group.Type.LOOKAHEAD, Coregex.empty());
            break;
          case 'Z':
            ctx.match('Z');
            elementaryRE =
                new Coregex.Group(
                    Coregex.Group.Type.LOOKAHEAD,
                    new Coregex.Union(Coregex.empty(), Coregex.literal("\n", 0)));
            break;
          case 'G':
            elementaryRE = ctx.unsupported("metacharacter \\" + ch + " is not supported");
            break;
          case 'k':
            ctx.match('k');
            ctx.match('<');
            String name = ctx.span(c -> '>' != c);
            ctx.match('>');
            elementaryRE = new Coregex.Ref(name);
            break;
          default:
            if (((ch - '0') | ('9' - ch)) >= 0) {
              elementaryRE = new Coregex.Ref(numeric(ctx));
            } else {
              elementaryRE = metachar(ctx);
            }
            break;
        }
        break;
      default:
        elementaryRE = literal(ctx);
        break;
    }
    return elementaryRE;
  }

  /*
   * <pre>{@code
   * quoted ::= 'Q', ? quoted ?, '\', 'E'
   * }</pre>
   */
  private String quoted(Context ctx) {
    ctx.match('Q');
    ctx.flags |= Pattern.LITERAL;
    StringBuilder literal = new StringBuilder();
    do {
      literal.append(ctx.span(ch -> '\\' != ch));
      ctx.match('\\');
    } while ('E' != ctx.peek() && (literal.append('\\') != null));
    ctx.flags &= ~Pattern.LITERAL;
    ctx.match('E');
    return literal.toString();
  }

  /*
   * <pre>{@code
   * literal ::= literal-char, {literal-char}
   * literal-char ::= ? not metachar not followed by quantifier ?
   * }</pre>
   */
  private Coregex literal(Context ctx) {
    Coregex literal;
    char ch;
    if (ctx.hasMoreElements() && !isREMetachar(ch = ctx.peek())) {
      StringBuilder sb = new StringBuilder();
      ctx.match(ch);
      sb.append(ch);
      loop:
      while (ctx.hasMoreElements() && !isREMetachar(ch = ctx.peek())) {
        char next = ctx.peek(2);
        switch (next) {
          case '*':
          case '+':
          case '?':
          case '{':
            break loop;
          default:
            ctx.match(ch);
            sb.append(ch);
        }
      }
      literal = Coregex.literal(sb.toString(), ctx.flags);
    } else {
      literal = Coregex.empty();
    }
    return literal;
  }

  /*
   * <pre>{@code
   * set ::= '[', [ '^' ], set-item, { [ '&&' ], set-item }, ']'
   * }</pre>
   */
  private Set set(Context ctx) {
    ctx.match('[');
    boolean negated = false;
    if ('^' == ctx.peek()) {
      ctx.match('^');
      negated = true;
    }
    Set.Builder set = Set.builder(ctx.flags), intersect = null;
    setItem(set, ctx);
    while (']' != ctx.peek()) {
      boolean mustIntersect = false;
      if ('&' == ctx.peek() && '&' == ctx.peek(2)) {
        ctx.match('&');
        ctx.match('&');
        if (null != intersect) {
          set = intersect.intersect(set.build());
        }
        intersect = set;
        set = Set.builder(ctx.flags);
        mustIntersect = '[' == ctx.peek();
      }
      setItem(set, ctx);
      if (mustIntersect) {
        set = intersect.intersect(set.build());
        intersect = null;
      }
    }
    if (null != intersect) {
      set = intersect.intersect(set.build());
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
        set.union(set(ctx));
        break;
      case '-':
        ctx.match('-');
        set.single('-');
        break;
      case '\\':
        ctx.match('\\');
        set.union(metachar(ctx));
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
   * group ::= group-with-body | group-without-body
   * group-with-body ::= '(', [ '?', ':' | '>' | '=' | '!' | '<', '=' | '!' | literal, '>' | [ '-' ], flags, ':' ], re, ')'
   * group-without-body ::= '(', '?', [ '-' ], flags, ')'
   * }</pre>
   */
  @SuppressWarnings("fallthrough")
  private Coregex group(Context ctx) {
    boolean negate = false;
    ctx.match('(');
    Coregex group;
    if ('?' == ctx.peek()) {
      ctx.match('?');
      switch (ctx.peek()) {
        case ':':
          ctx.match(':');
          group = new Coregex.Group(Coregex.Group.Type.NON_CAPTURING, RE(ctx));
          break;
        case '>':
          ctx.match('>');
          group = new Coregex.Group(Coregex.Group.Type.ATOMIC, RE(ctx));
          break;
        case '=':
          ctx.match('=');
          group = new Coregex.Group(Coregex.Group.Type.LOOKAHEAD, RE(ctx));
          break;
        case '!':
          ctx.match('!');
          group = new Coregex.Group(Coregex.Group.Type.NEGATIVE_LOOKAHEAD, RE(ctx));
          break;
        case '<':
          ctx.match('<');
          switch (ctx.peek()) {
            case '=':
              ctx.match('=');
              group = new Coregex.Group(Coregex.Group.Type.LOOKBEHIND, RE(ctx));
              break;
            case '!':
              ctx.match('!');
              group = new Coregex.Group(Coregex.Group.Type.NEGATIVE_LOOKBEHIND, RE(ctx));
              break;
            default:
              String name = ctx.span(ch -> '>' != ch);
              ctx.match('>');
              group = new Coregex.Group(ctx.index(), name, RE(ctx));
              break;
          }
          break;
        case '-':
          ctx.match('-');
          negate = true;
        // fall through
        default:
          int flags = ctx.flags;
          ctx.flags = negate ? flags & ~flags(ctx) : flags | flags(ctx);
          if (')' == ctx.peek()) {
            group = Coregex.empty();
            break;
          }
          ctx.match(':');
          group = new Coregex.Group(ctx.index(), RE(ctx));
          ctx.flags = flags;
          break;
      }
    } else {
      group = new Coregex.Group(ctx.index(), RE(ctx));
    }
    ctx.match(')');
    return group;
  }

  /*
   * <pre>{@code
   * flags ::= flag, {flag}
   * flag ::= 'd' | 'i' | 'm' | 's' | 'u' | 'U' | 'x'
   * }</pre>
   */
  private int flags(Context ctx) {
    int flags = 0;
    while (true) {
      char ch = ctx.peek();
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
          return flags;
      }
    }
  }

  /*
   * <pre>{@code
   * metachar ::= 't' | 'r' | 'n' | 'd' | 'D' | 'w' | 'W' | 's' | 'p', '{', ? posix ?, '}' | 'S' | quoted | single
   * }</pre>
   */
  @SuppressWarnings("fallthrough")
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
        metachar.set('\r', '\n', '\t', '\f', ' ');
        break;
      case 'S':
        ctx.match('S');
        metachar.set('\r', '\n', '\t', '\f', ' ').negate();
        break;
      case 'p':
        ctx.match('p');
        ctx.match('{');
        String posix = ctx.span(pos -> '}' != pos);
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

          case "Alnum":
            metachar.range('0', '9');
          // fall through
          case "Alpha":
            metachar.range('a', 'z').range('A', 'Z');
            break;

          case "Print":
            metachar.single(' ');
          // fall through
          case "Graph":
            metachar.range('0', '9').range('a', 'z').range('A', 'Z');
          // fall through
          case "Punct":
            metachar.set(
                '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':',
                ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~');
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
        for (char quoted : quoted(ctx).toCharArray()) {
          metachar.single(quoted);
        }
        break;
      default:
        // escaped metacharacter
        ctx.match(ch);
        metachar.single(ch);
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

  /*
   * <pre>{@code
   * numeric ::= digit, {digit}
   * digit ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
   * }</pre>
   */
  private int numeric(Context ctx) {
    char ch = ctx.peek();
    if (((ch - '0') | ('9' - ch)) < 0) {
      return -1;
    }
    int numeric = 0;
    do {
      ctx.match(ch);
      numeric = numeric * 10 + (ch - '0');
      ch = ctx.peek();
    } while (((ch - '0') | ('9' - ch)) >= 0);
    return numeric;
  }

  private static final class Context {
    private static final char SKIP = '\u0000';
    private static final char EOF = '\uFFFF';

    private static final MethodHandle CHARACTER_CODE_POINT_OF;

    static {
      MethodHandle codePointOf = null;
      try {
        codePointOf =
            MethodHandles.lookup()
                .findStatic(
                    Character.class, "codePointOf", MethodType.methodType(int.class, String.class));
      } catch (Exception ignored) {
      } finally {
        CHARACTER_CODE_POINT_OF = codePointOf;
      }
    }

    private final String regex;
    private final char[] tokens = {SKIP, SKIP, SKIP, SKIP};
    private int flags, index, cursor, tokensCursor;

    Context(String regex, int flags) {
      this.regex = regex;
      this.flags = flags;
    }

    boolean hasMoreElements() {
      return EOF != peek();
    }

    char peek() {
      return peek(1);
    }

    char peek(int i) {
      for (; tokensCursor < i; tokensCursor++) {
        tokens[tokensCursor] = token();
      }
      return tokens[i - 1];
    }

    void match(char ch) {
      if (ch != peek()) {
        error(String.valueOf(ch));
      }
      tokensCursor--;
      for (int i = 0; i < tokens.length - 1; i++) {
        tokens[i] = tokens[i + 1];
      }
      tokens[tokens.length - 1] = SKIP;
    }

    String span(IntPredicate charPredicate) {
      char ch;
      if (EOF != (ch = peek()) && charPredicate.test(ch)) {
        StringBuilder span = new StringBuilder();
        match(ch);
        span.append(ch);
        while (EOF != (ch = peek()) && charPredicate.test(ch)) {
          match(ch);
          span.append(ch);
        }
        return span.toString();
      } else {
        return "";
      }
    }

    private char token() {
      char[] chars;
      char ch;
      int start, cursor = this.cursor;
      loop:
      do {
        ch = cursor < regex.length() ? regex.charAt(cursor) : EOF;
        if (0 != (flags & Pattern.LITERAL)) {
          cursor += 1;
          break;
        }
        switch (ch) {
          case ' ':
          case '\t':
          case '\f':
          case '\r':
          case '\n':
            if (0 != (flags & Pattern.COMMENTS)) {
              ch = SKIP;
            }
            break;
          case '#':
            if (0 != (flags & Pattern.COMMENTS)) {
              while (cursor + 1 < regex.length()
                  && ('\n' != (ch = regex.charAt(cursor + 1))
                      && (0 == (flags & Pattern.UNIX_LINES) || '\r' != ch))) {
                cursor++;
              }
              ch = SKIP;
            }
            break;
          case '\\':
            switch (cursor + 1 < regex.length() ? regex.charAt(cursor + 1) : EOF) {
              case '0':
                start = cursor += 2;
                while ('0' <= (ch = regex.charAt(cursor)) && ch <= '7') {
                  cursor++;
                }
                chars = Character.toChars(Integer.parseInt(regex.substring(start, cursor), 8));
                System.arraycopy(chars, 0, tokens, tokensCursor, chars.length);
                ch = chars[0];
                break loop;
              case 'N':
                if (null == CHARACTER_CODE_POINT_OF) {
                  break;
                }
                start = cursor += 3;
                while ('}' != regex.charAt(cursor)) {
                  cursor++;
                }
                try {
                  chars =
                      Character.toChars(
                          (int) CHARACTER_CODE_POINT_OF.invoke(regex.substring(start, cursor)));
                  System.arraycopy(chars, 0, tokens, tokensCursor, chars.length);
                  ch = chars[0];
                } catch (Throwable t) {
                  //noinspection DataFlowIssue
                  throw (RuntimeException) t;
                }
                break;
              case 'u':
                String u = regex.substring(cursor + 2, cursor + 6);
                chars = Character.toChars(Integer.parseInt(u, 16));
                System.arraycopy(chars, 0, tokens, tokensCursor, chars.length);
                ch = chars[0];
                cursor += 6;
                break loop;
              case 'x':
                String x = regex.substring(cursor + 2, cursor + 4);
                ch = (char) Integer.parseInt(x, 16);
                cursor += 4;
                break loop;
            }
        }
        cursor += 1;
      } while (SKIP == ch);
      this.cursor = cursor;
      return ch;
    }

    int index() {
      return ++index;
    }

    <T> T error(String expected) {
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

    <T> T unsupported(String reason) {
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
