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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public abstract class Coregex implements Serializable {
  private static final long serialVersionUID = 1L;

  public static Coregex concat(Coregex first, Coregex... rest) {
    return new Concat(requireNonNull(first, "first"), requireNonNull(rest, "rest"));
  }

  private static final Coregex ANY =
      set(
          com.github.simy4.coregex.core.Set.builder()
              .range(Character.MIN_VALUE, (char) (Character.MIN_SURROGATE - 1))
              .build());

  public static Coregex any() {
    return ANY;
  }

  public static Coregex empty() {
    return new Literal("");
  }

  public static Coregex literal(String literal) {
    return new Literal(requireNonNull(literal, "literal"));
  }

  public static Coregex set(com.github.simy4.coregex.core.Set set) {
    return new Set(requireNonNull(set, "set"));
  }

  public static Coregex union(Coregex first, Coregex... rest) {
    return new Union(requireNonNull(first, "first"), requireNonNull(rest, "rest"));
  }

  private Coregex() {}

  protected abstract Map.Entry<RNG, String> apply(RNG rng, int remainder);

  public final String generate(RNG rng, int remainder) {
    if (remainder < 0) {
      throw new IllegalArgumentException("remainder: " + remainder + " has to be non negative");
    }
    return apply(requireNonNull(rng, "rng"), remainder).getValue();
  }

  public final Coregex quantify(int min, int max) {
    if (min < 0 || max < 0 || min > max) {
      throw new IllegalArgumentException(
          "min: " + min + " and max: " + max + " has to be positive with min being <= max");
    }
    return 1 == min && 1 == max ? this : new Quantified(this, min, max);
  }

  public abstract int minLength();

  public abstract int maxLength();

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
    protected Map.Entry<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      StringBuilder sb = new StringBuilder(Math.min(remainder, maxLength()));
      Coregex chunk = first;
      int i = 0;
      do {
        Map.Entry<RNG, String> rngAndCoregex =
            chunk.apply(rng, remainder - minLength() + chunk.minLength());
        rng = rngAndCoregex.getKey();
        String value = rngAndCoregex.getValue();
        sb.append(value);
        remainder -= (value.length() - chunk.minLength());
      } while (i < rest.length && (chunk = rest[i++]) != null);
      return new AbstractMap.SimpleEntry<>(rng, sb.toString());
    }

    @Override
    public int minLength() {
      int min = first.minLength();
      for (Coregex coregex : rest) {
        min += coregex.minLength();
      }
      return min;
    }

    @Override
    public int maxLength() {
      int max = first.maxLength();
      for (Coregex coregex : rest) {
        max += coregex.maxLength();
      }
      return max;
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
      StringBuilder sb = new StringBuilder();
      sb.append(first.toString());
      for (Coregex coregex : rest) {
        sb.append(coregex.toString());
      }
      return sb.toString();
    }
  }

  private static final class Literal extends Coregex {
    private static final long serialVersionUID = 1L;

    private final String literal;

    private Literal(String literal) {
      this.literal = literal;
    }

    @Override
    protected Map.Entry<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      return new AbstractMap.SimpleEntry<>(rng, literal);
    }

    @Override
    public int minLength() {
      return literal.length();
    }

    @Override
    public int maxLength() {
      return literal.length();
    }

    @Override
    int weight() {
      return 1;
    }

    @Override
    public String toString() {
      return literal;
    }
  }

  private static final class Set extends Coregex {
    private static final long serialVersionUID = 1L;

    private final com.github.simy4.coregex.core.Set set;

    private Set(com.github.simy4.coregex.core.Set set) {
      this.set = set;
    }

    @Override
    protected Map.Entry<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      Map.Entry<RNG, Long> rngAndSeed = rng.genLong();
      return new AbstractMap.SimpleEntry<>(
          rng, String.valueOf(set.generate(rngAndSeed.getValue())));
    }

    @Override
    public int minLength() {
      return 1;
    }

    @Override
    public int maxLength() {
      return 1;
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
    private static final long serialVersionUID = 1L;

    private final Coregex quantified;
    private final int min;
    private final int max;

    private Quantified(Coregex quantified, int min, int max) {
      this.quantified = quantified;
      this.min = min;
      this.max = max;
    }

    @Override
    protected Map.Entry<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      Map.Entry<RNG, Integer> rngAndQuantifier = rng.genInteger(min, max);
      rng = rngAndQuantifier.getKey();
      int quantifier = rngAndQuantifier.getValue();
      StringBuilder sb = new StringBuilder(Math.min(remainder, maxLength()));
      for (int i = 0; i < quantifier && quantified.minLength() <= remainder; i++) {
        Map.Entry<RNG, String> rngAndCoregex = quantified.apply(rng, remainder);
        rng = rngAndCoregex.getKey();
        String value = rngAndCoregex.getValue();
        sb.append(value);
        remainder -= value.length();
      }
      return new AbstractMap.SimpleEntry<>(rng, sb.toString());
    }

    @Override
    public int minLength() {
      return quantified.minLength() * min;
    }

    @Override
    public int maxLength() {
      return quantified.maxLength() * max;
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
    protected Map.Entry<RNG, String> apply(RNG rng, int remainder) {
      List<Coregex> fits = new ArrayList<>(rest.length + 1);
      int weight = 0;
      if (first.minLength() <= remainder) {
        weight += first.weight();
        fits.add(first);
      }
      for (Coregex coregex : rest) {
        if (coregex.minLength() <= remainder) {
          weight += coregex.weight();
          fits.add(coregex);
        }
      }
      if (fits.isEmpty()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }

      Map.Entry<RNG, Integer> rngAndWeightedSeed = rng.genInteger(0, weight);
      rng = rngAndWeightedSeed.getKey();
      int sample = rngAndWeightedSeed.getValue();
      int threshold = 0;
      for (Coregex coregex : fits) {
        threshold += coregex.weight();
        if (sample < threshold) {
          return coregex.apply(rng, remainder);
        }
      }
      return fits.get(fits.size() - 1).apply(rng, remainder);
    }

    @Override
    public int minLength() {
      int min = first.minLength();
      for (Coregex coregex : rest) {
        min = Math.min(min, coregex.minLength());
      }
      return min;
    }

    @Override
    public int maxLength() {
      int max = first.maxLength();
      for (Coregex coregex : rest) {
        max = Math.max(max, coregex.maxLength());
      }
      return max;
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
      StringJoiner joiner = new StringJoiner("|", "(?:", ")");
      joiner.add(first.toString());
      for (Coregex coregex : rest) {
        joiner.add(coregex.toString());
      }
      return joiner.toString();
    }
  }
}
