package com.github.simy4.coregex.core;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;

public abstract class Coregex implements Function<RNG, Map.Entry<RNG, String>>, Serializable {
  public static Coregex concat(Coregex first, Coregex... rest) {
    return new Concat(first, rest);
  }

  public static Coregex empty() {
    return Empty.INSTANCE;
  }

  public static Coregex set(SetItem set) {
    return new Set(set);
  }

  public static Coregex union(Coregex first, Coregex... rest) {
    return new Union(first, rest);
  }

  public final String generate(RNG rng) {
    return apply(rng).getValue();
  }

  public final Coregex quantify(int min, int max) {
    return 1 == min && 1 == max ? this : new Quantified(this, min, max);
  }

  private static final class Concat extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    Concat(Coregex first, Coregex[] rest) {
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
  }

  private static final class Union extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    Union(Coregex first, Coregex[] rest) {
      this.first = first;
      this.rest = rest;
    }

    @Override
    public Map.Entry<RNG, String> apply(RNG rng) {
      Map.Entry<RNG, Integer> rngAndIdx = rng.genInteger(0, rest.length);
      int idx = rngAndIdx.getValue();
      return (idx < rest.length ? rest[idx] : first).apply(rngAndIdx.getKey());
    }
  }
}
