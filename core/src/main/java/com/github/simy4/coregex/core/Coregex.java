package com.github.simy4.coregex.core;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public abstract class Coregex implements Function<RNG, Map.Entry<RNG, String>>, Predicate<String>, Serializable {
  public static Coregex concat(Coregex first, Coregex... rest) {
    return new Concat(requireNonNull(first, "first"), requireNonNull(rest, "rest"));
  }

  public static Coregex empty() {
    return Empty.INSTANCE;
  }

  public static Coregex set(SetItem set) {
    return new Set(requireNonNull(set, "set"));
  }

  public static Coregex union(Coregex first, Coregex... rest) {
    return new Union(requireNonNull(first, "first"), requireNonNull(rest, "rest"));
  }

  public final String generate(RNG rng) {
    return apply(requireNonNull(rng, "rng")).getValue();
  }

  public Coregex quantify(int min, int max) {
    if (min < 0 || max < 0 || min > max) {
      throw new IllegalArgumentException("min: " + min + " and max: " + max + " has to be positive with min being <= max");
    }
    return 1 == min && 1 == max ? this : new Quantified(this, min, max);
  }

  protected abstract int min();

  protected abstract int max();

  abstract int weight();

  private static final class Concat extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    private Concat(Coregex first, Coregex[] rest) {
      this.first = first;
      this.rest = rest;
    }

    @Override
    public Map.Entry<RNG, String> apply(RNG rng) {
      StringBuilder sb = new StringBuilder();
      Map.Entry<RNG, String> rngAndCoregex = first.apply(rng);
      rng = rngAndCoregex.getKey();
      sb.append(rngAndCoregex.getValue());
      for (Coregex coregex : rest) {
        rngAndCoregex = coregex.apply(rng);
        rng = rngAndCoregex.getKey();
        sb.append(rngAndCoregex.getValue());
      }
      return new AbstractMap.SimpleEntry<>(rng, sb.toString());
    }

    @Override
    protected int min() {
      int min = first.min();
      for (Coregex coregex : rest) {
        min += coregex.min();
      }
      return min;
    }

    @Override
    protected int max() {
      int max = first.max();
      for (Coregex coregex : rest) {
        max += coregex.max();
      }
      return max;
    }

    @Override
    public boolean test(String s) {
      if (s.length() < min() || max() < s.length()) {
        return false;
      }
      Queue<Coregex> queue = new ArrayDeque<>(rest.length + 1);
      queue.add(first);
      Collections.addAll(queue, rest);
      return test0(s, 0, queue);
    }

    private boolean test0(String s, int offset, Queue<Coregex> rest) {
      if (rest.isEmpty()) {
        return offset == s.length();
      }
      Coregex head = rest.remove();
      boolean result = false;
      for (int i = head.min(); i <= head.max() && !result; i++) {
        int newOffset = offset + i;
        if (s.length() < newOffset) {
          break;
        }
        if (!head.test(s.substring(offset, newOffset))) {
          continue;
        }
        result = test0(s, newOffset, new ArrayDeque<>(rest));
      }
      return result;
    }

    @Override
    int weight() {
      int weight = first.weight();
      for (Coregex coregex : rest) {
        weight += coregex.weight();
      }
      return weight / (rest.length + 1);
    }

    @Override
    public String toString() {
      StringJoiner joiner = new StringJoiner(" <*> ", " <*> ", ")").setEmptyValue(")");
      for (Coregex coregex : rest) {
        joiner.add(coregex.toString());
      }
      return "(" + first + joiner;
    }
  }

  private static final class Empty extends Coregex {
    private static final long serialVersionUID = 1L;
    private static final Empty INSTANCE = new Empty();

    private Empty() {
    }

    @Override
    public Map.Entry<RNG, String> apply(RNG rng) {
      return new AbstractMap.SimpleEntry<>(rng, "");
    }

    @Override
    protected int min() {
      return 0;
    }

    @Override
    protected int max() {
      return 0;
    }

    @Override
    public Coregex quantify(int min, int max) {
      return this;
    }

    @Override
    public boolean test(String s) {
      return s.isEmpty();
    }

    @Override
    int weight() {
      return 1;
    }

    private Object readResolve() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "∅";
    }
  }

  private static final class Set extends Coregex {
    private static final long serialVersionUID = 1L;

    private final SetItem set;

    private Set(SetItem set) {
      this.set = set;
    }

    @Override
    public Map.Entry<RNG, String> apply(RNG rng) {
      Map.Entry<RNG, Long> rngAndSeed = rng.genLong();
      return new AbstractMap.SimpleEntry<>(rng, String.valueOf(set.generate(rngAndSeed.getValue())));
    }

    @Override
    protected int min() {
      return 1;
    }

    @Override
    protected int max() {
      return 1;
    }

    @Override
    public boolean test(String s) {
      return 1 == s.length() && set.test(s.charAt(0));
    }

    @Override
    int weight() {
      return set.weight();
    }

    @Override
    public String toString() {
      return "[" + set + ']';
    }
  }

  private static final class Quantified extends Coregex {
    private final Coregex quantified;
    private final int min;
    private final int max;

    private Quantified(Coregex quantified, int min, int max) {
      this.quantified = quantified;
      this.min = min;
      this.max = max;
    }

    @Override
    public Map.Entry<RNG, String> apply(RNG rng) {
      Map.Entry<RNG, Integer> rngAndQuantifier = rng.genInteger(min, max);
      rng = rngAndQuantifier.getKey();
      int quantifier = rngAndQuantifier.getValue();
      String[] repeats = new String[quantifier];
      for (int i = 0; i < quantifier; i++) {
        Map.Entry<RNG, String> rngAndCoregex = quantified.apply(rng);
        rng = rngAndCoregex.getKey();
        repeats[i] = rngAndCoregex.getValue();
      }
      return new AbstractMap.SimpleEntry<>(rng, String.join("", repeats));
    }

    @Override
    protected int min() {
      return quantified.min() * min;
    }

    @Override
    protected int max() {
      return quantified.max() * max;
    }

    @Override
    public boolean test(String s) {
      if (s.length() < min() || max() < s.length()) {
        return false;
      }
      boolean result = false;
      for (int i = min; i <= max && !result; i++) {
        if (i == 0) {
          result = s.isEmpty();
          continue;
        }
        if (s.length() % i != 0) {
          continue;
        }
        int chars = s.length() / i;
        boolean segmentMatches = true;
        for(int j = 0, idx = 0; j < s.length() && segmentMatches; j += chars, idx++) {
          segmentMatches = quantified.test(s.substring(j, j + chars));
        }
        result = segmentMatches;
      }
      return result;
    }

    @Override
    int weight() {
      return quantified.weight();
    }

    @Override
    public String toString() {
      return quantified.toString() + '{' + min + ',' + max + '}';
    }
  }

  private static final class Union extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    private Union(Coregex first, Coregex[] rest) {
      this.first = first;
      this.rest = rest;
    }

    @Override
    public Map.Entry<RNG, String> apply(RNG rng) {
      Map.Entry<RNG, Integer> rngAndWeightedSeed = rng.genInteger(0, weight());
      rng = rngAndWeightedSeed.getKey();
      int weightedSeed = rngAndWeightedSeed.getValue();
      int threshold = 0;
      for (Coregex coregex : rest) {
        threshold += coregex.weight();
        if (weightedSeed < threshold) {
          return coregex.apply(rng);
        }
      }
      return first.apply(rng);
    }

    @Override
    protected int min() {
      int min = first.min();
      for (Coregex coregex : rest) {
        min = Math.min(min, coregex.min());
      }
      return min;
    }

    @Override
    protected int max() {
      int max = first.max();
      for (Coregex coregex : rest) {
        max = Math.max(max, coregex.max());
      }
      return max;
    }

    @Override
    public boolean test(String s) {
      boolean result = first.test(s);
      for (int i = 0; i < rest.length && !result; i++) {
        result = rest[i].test(s);
      }
      return result;
    }

    @Override
    int weight() {
      int result = first.weight();
      for (Coregex coregex : rest) {
        result += coregex.weight();
      }
      return result;
    }

    @Override
    public String toString() {
      StringJoiner joiner = new StringJoiner(" <|> ", " <|> ", ")").setEmptyValue(")");
      for (Coregex coregex : rest) {
        joiner.add(coregex.toString());
      }
      return "(" + first + joiner;
    }
  }
}
