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

import java.io.Serializable;
import java.util.StringJoiner;
import java.util.function.IntPredicate;

import static java.util.Objects.requireNonNull;

public abstract class SetItem implements IntPredicate, Serializable {
  private static final char[] EMPTY = {};

  public static SetItem any() {
    return range(Character.MIN_VALUE, Character.MAX_VALUE);
  }

  public static SetItem range(char start, char end) {
    if (start >= end) {
      throw new IllegalArgumentException("start: " + start + " should be < end: " + end);
    }
    return new Range(start, end);
  }

  public static SetItem set(char first, char... rest) {
    return new Set(first, requireNonNull(rest, "rest"));
  }

  public static SetItem single(char ch) {
    return set(ch, EMPTY);
  }

  public static SetItem union(SetItem first, SetItem... rest) {
    return new Union(requireNonNull(first, "first"), requireNonNull(rest, "rest"));
  }

  private SetItem() {}

  public abstract char generate(long seed);

  abstract int weight();

  @Override
  public SetItem negate() {
    return new Negated(this);
  }

  private static final class Negated extends SetItem {
    private static final long serialVersionUID = 1L;

    private final SetItem negated;

    private Negated(SetItem negated) {
      this.negated = negated;
    }

    @Override
    public char generate(long seed) {
      char result;
      long offset = 0L;
      do {
        result = negated.generate(seed + offset++);
      } while (!test(result));
      return result;
    }

    @Override
    public boolean test(int value) {
      return !negated.test(value);
    }

    @Override
    public SetItem negate() {
      return negated;
    }

    @Override
    int weight() {
      return negated.weight();
    }

    @Override
    public String toString() {
      return "!" + negated;
    }
  }

  private static final class Range extends SetItem {
    private static final long serialVersionUID = 1L;

    private final char start;
    private final char end;

    private Range(char start, char end) {
      this.start = start;
      this.end = end;
    }

    @Override
    public char generate(long seed) {
      return (char) (start + (Math.abs(seed) % (end - start)));
    }

    @Override
    public boolean test(int value) {
      return start <= value && value <= end;
    }

    @Override
    int weight() {
      return end - start;
    }

    @Override
    public String toString() {
      return "" + start + '-' + end;
    }
  }

  private static final class Set extends SetItem {
    private static final long serialVersionUID = 1L;

    private final char first;
    private final char[] rest;

    private Set(char first, char[] rest) {
      this.first = first;
      this.rest = rest;
    }

    @Override
    public char generate(long seed) {
      int idx = (int) (Math.abs(seed) % weight());
      return idx < rest.length ? rest[idx] : first;
    }

    @Override
    public boolean test(int value) {
      boolean result = first == value;
      for (int i = 0; !result && i < rest.length; i++) {
        result = rest[i] == value;
      }
      return result;
    }

    @Override
    int weight() {
      return rest.length + 1;
    }

    @Override
    public String toString() {
      StringJoiner joiner = new StringJoiner("");
      for (char ch : rest) {
        joiner.add(String.valueOf(ch));
      }
      return "" + first + joiner;
    }
  }

  private static final class Union extends SetItem {
    private static final long serialVersionUID = 1L;

    private final SetItem first;
    private final SetItem[] rest;

    private Union(SetItem first, SetItem[] rest) {
      this.first = first;
      this.rest = rest;
    }

    @Override
    public char generate(long seed) {
      int weightedSeed = (int) (Math.abs(seed) % weight());
      int threshold = 0;
      for (SetItem item : rest) {
        threshold += item.weight();
        if (weightedSeed < threshold) {
          return item.generate(seed);
        }
      }
      return first.generate(seed);
    }

    @Override
    public boolean test(int value) {
      boolean result = first.test(value);
      for (int i = 0; !result && i < rest.length; i++) {
        result = rest[i].test(value);
      }
      return result;
    }

    @Override
    int weight() {
      int result = first.weight();
      for (SetItem item : rest) {
        result += item.weight();
      }
      return result;
    }

    @Override
    public String toString() {
      StringJoiner joiner = new StringJoiner("|", "|", ")").setEmptyValue(")");
      for (SetItem setItem : rest) {
        joiner.add(setItem.toString());
      }
      return "(" + first + joiner;
    }
  }
}
