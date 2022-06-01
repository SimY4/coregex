/*
 * Copyright 2021 Alex Simkin
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

public final class CoregexParser {
  public static CoregexParser getInstance() {
    return new CoregexParser();
  }

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
    char ch = ctx.peek();
    switch (ch) {
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
      case '\\':
        ctx.match('\\');
        ch = ctx.peek();
        elementaryRE = Coregex.set(isREMetachar(ch) ? SetItem.single(ch) : metachar(ctx));
        break;
      default:
        ctx.match(ch);
        elementaryRE = Coregex.set(SetItem.single(ch));
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
    char ch = ctx.peek();
    switch (ch) {
      case '[':
        setItem = set(ctx);
        break;
      case '-':
        ctx.match('-');
        setItem = SetItem.single('-');
        break;
      case '\\':
        ctx.match('\\');
        ch = ctx.peek();
        if (!isSetMetachar(ch)) {
          setItem = metachar(ctx);
          break;
        }
        // fall through
      default:
        ctx.match(ch);
        if ('-' == ctx.peek()) {
          ctx.match('-');
          char end = ctx.peek();
          switch (end) {
            case ']':
              setItem = SetItem.set(ch, '-');
              break;
            case '\\':
              ctx.match('\\');
              end = ctx.peek();
              // fall through
            default:
              ctx.match(end);
              setItem = SetItem.range(ch, end);
              break;
          }
        } else {
          setItem = SetItem.single(ch);
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
        default:
          group = ctx.unsupported("inline option is not supported");
          break;
      }
    } else {
      group = RE(ctx);
    }
    ctx.match(')');
    return group;
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
        charSet = ctx.error("metacharacter \\" + ch + " is not supported");
        break;
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
        error("No more elements");
      }
      return regex.charAt(cursor);
    }

    private void match(char ch) {
      if (ch != peek()) {
        error(String.valueOf(ch));
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

    private <T> T error(String expected) {
      String messagePrefix = "Unable to parse regex: '";
      char[] cursor = new char[messagePrefix.length() + this.cursor];
      Arrays.fill(cursor, ' ');
      cursor[cursor.length - 1] = '^';
      String message =
          String.join(
              System.lineSeparator(),
              "Unable to parse regex: '" + regex + "'",
              new String(cursor),
              "Expected: '" + expected + "' Actual: '" + regex.charAt(this.cursor) + "'");
      throw new UnsupportedOperationException(message);
    }

    private <T> T unsupported(String reason) {
      String messagePrefix = "Unable to parse regex: '";
      char[] cursor = new char[messagePrefix.length() + this.cursor];
      Arrays.fill(cursor, ' ');
      cursor[cursor.length - 1] = '^';
      String message =
          String.join(
              System.lineSeparator(),
              "Unable to parse regex: '" + regex + "'",
              new String(cursor),
              "Reason: " + reason);
      throw new UnsupportedOperationException(message);
    }
  }
}
