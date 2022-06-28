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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public abstract class Coregex implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final Coregex ANY =
      new Set(
          com.github.simy4.coregex.core.Set.builder()
              .range(Character.MIN_VALUE, (char) (Character.MIN_SURROGATE - 1))
              .build());

  public static Coregex any() {
    return ANY;
  }

  public static Coregex empty() {
    return new Literal("");
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
    return 1 == min && 1 == max ? this : new Quantified(this, min, max);
  }

  public abstract int minLength();

  public abstract int maxLength();

  abstract int weight();

  public static final class Concat extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    public Concat(Coregex first, Coregex[] rest) {
      this.first = requireNonNull(first, "first");
      this.rest = Arrays.copyOf(rest, rest.length);
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

    public List<Coregex> concat() {
      List<Coregex> concat = new ArrayList<>(rest.length + 1);
      concat.add(first);
      concat.addAll(Arrays.asList(rest));
      return concat;
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

  public static final class Literal extends Coregex {
    private static final long serialVersionUID = 1L;

    private final String literal;

    public Literal(String literal) {
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

    public String literal() {
      return literal;
    }

    @Override
    public String toString() {
      return literal.replaceAll("[.\\[(^$\\\\]", "\\\\$0");
    }
  }

  public static final class Set extends Coregex {
    private static final long serialVersionUID = 1L;

    private final com.github.simy4.coregex.core.Set set;

    public Set(com.github.simy4.coregex.core.Set set) {
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

    public com.github.simy4.coregex.core.Set set() {
      return set;
    }

    @Override
    public String toString() {
      return ANY == this ? "." : "[" + set + ']';
    }
  }

  public static final class Quantified extends Coregex {
    private static final long serialVersionUID = 1L;

    private static final int MAX_QUANTIFIER = 32;

    private final Coregex quantified;
    private final int min;
    private final int max;

    public Quantified(Coregex quantified, int min, int max) {
      this.quantified = requireNonNull(quantified, "quantified");
      if (min < 0 || (-1 != max && min > max)) {
        throw new IllegalArgumentException(
            "min: " + min + " and max: " + max + " has to be positive with min being <= max");
      }
      this.min = min;
      this.max = max;
    }

    @Override
    protected Map.Entry<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      Map.Entry<RNG, Integer> rngAndQuantifier =
          rng.genInteger(min, -1 == max ? MAX_QUANTIFIER : max);
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
      return quantified.maxLength() * (-1 == max ? MAX_QUANTIFIER : max);
    }

    @Override
    int weight() {
      return quantified.weight();
    }

    public Coregex quantified() {
      return quantified;
    }

    public int min() {
      return min;
    }

    public int max() {
      return max;
    }

    @Override
    public String toString() {
      if (-1 == max) {
        switch (min) {
          case 0:
            return quantified + "*";
          case 1:
            return quantified + "+";
          default:
            return quantified.toString() + '{' + min + ",}";
        }
      } else {
        return quantified.toString() + '{' + min + ',' + max + '}';
      }
    }
  }

  public static final class Union extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    public Union(Coregex first, Coregex[] rest) {
      this.first = requireNonNull(first, "first");
      this.rest = Arrays.copyOf(rest, rest.length);
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

    public List<Coregex> union() {
      List<Coregex> union = new ArrayList<>(rest.length + 1);
      union.add(first);
      union.addAll(Arrays.asList(rest));
      return union;
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
