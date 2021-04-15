package com.github.simy4.coregex.core;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public abstract class Coregex implements Function<RNG, Map.Entry<RNG, String>>, Serializable {
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

  public final Coregex quantify(int min, int max) {
    if (min < 0 || max < 0 || min > max) {
      throw new IllegalArgumentException("min: " + min + " and max: " + max + " has to be positive with min being <= max");
    }
    return 1 == min && 1 == max ? this : new Quantified(this, min, max);
  }

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
    public String toString() {
      return "TBD";
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
      Map.Entry<RNG, Integer> rngAndIdx = rng.genInteger(0, rest.length);
      int idx = rngAndIdx.getValue();
      return (idx < rest.length ? rest[idx] : first).apply(rngAndIdx.getKey());
    }

    @Override
    public String toString() {
      StringJoiner joiner = new StringJoiner("|", "|", ")");
      for (Coregex coregex : rest) {
        joiner.add(coregex.toString());
      }
      return "(" + first + joiner;
    }
  }
}
