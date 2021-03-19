package com.github.simy4.coregex.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.IntPredicate;

abstract class SetItem implements IntPredicate, Serializable {
  static SetItem range(char start, char end) {
    return new Range(start, end);
  }

  static SetItem set(char first, char... rest) {
    return new Set(first, rest);
  }

  static SetItem union(SetItem first, SetItem... rest) {
    return new Union(first, rest);
  }

  private SetItem() {
  }

  abstract char generate(long seed);

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
    char generate(long seed) {
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
  }

  private static final class Range extends SetItem {
    private static final long serialVersionUID = 1L;

    private final char start;
    private final char end;

    Range(char start, char end) {
      this.start = start;
      this.end = end;
    }

    @Override
    char generate(long seed) {
      return (char) (start + Math.abs((int) (seed % (end - start))));
    }

    @Override
    public boolean test(int value) {
      return start <= value && value <= end;
    }
  }

  private static final class Set extends SetItem {
    private static final long serialVersionUID = 1L;

    private final char first;
    private final char[] rest;

    Set(char first, char[] rest) {
      this.first = first;
      this.rest = rest;
    }

    @Override
    char generate(long seed) {
      int idx = (int) Math.abs(seed % (rest.length + 1));
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
  }

  private static final class Union extends SetItem {
    private static final long serialVersionUID = 1L;

    private final SetItem first;
    private final SetItem[] rest;

    Union(SetItem first, SetItem[] rest) {
      this.first = first;
      this.rest = rest;
    }

    @Override
    char generate(long seed) {
      int idx = (int) Math.abs(seed % (rest.length + 1));
      return (idx < rest.length ? rest[idx] : first).generate(seed);
    }

    @Override
    public boolean test(int value) {
      return first.test(value) || Arrays.stream(rest).anyMatch(setItem -> setItem.test(value));
    }
  }
}
