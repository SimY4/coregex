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
import java.util.Objects;
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

  public String generate(RNG rng) {
    int remainder = -1 == maxLength() ? Integer.MAX_VALUE - 2 : maxLength();
    return apply(requireNonNull(rng, "rng"), remainder).getValue();
  }

  public final Coregex quantify(int min, int max, boolean greedy) {
    return 1 == min && 1 == max ? this : new Quantified(this, min, max, greedy);
  }

  public final Coregex sized(int size) {
    return new Sized(this, size);
  }

  public abstract int minLength();

  public abstract int maxLength();

  abstract int weight();

  public static final class Concat extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    public Concat(Coregex first, Coregex... rest) {
      this.first = requireNonNull(first, "first");
      this.rest = Arrays.copyOf(rest, rest.length);
    }

    @Override
    protected Map.Entry<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      StringBuilder sb = new StringBuilder(minLength() + 16);
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
      int sum = first.maxLength();
      if (-1 == sum) {
        return sum;
      }
      for (Coregex coregex : rest) {
        int max = coregex.maxLength();
        if (-1 == max) {
          return max;
        }
        sum += max;
      }
      return sum;
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

    public Coregex simplify() {
      List<Coregex> concat = new ArrayList<>(rest.length + 1);
      concat.add(requireNonNull(first, "first"));
      int idx = 0;
      for (Coregex coregex : requireNonNull(rest, "rest")) {
        Coregex last;
        if (coregex instanceof Literal && (last = concat.get(idx)) instanceof Literal) {
          concat.set(idx, new Literal(((Literal) last).literal + ((Literal) coregex).literal));
        } else {
          concat.add(coregex);
          idx++;
        }
      }
      return 1 == concat.size()
          ? concat.get(0)
          : new Concat(concat.get(0), concat.subList(1, concat.size()).toArray(new Coregex[0]));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Concat concat = (Concat) o;
      return first.equals(concat.first) && Arrays.equals(rest, concat.rest);
    }

    @Override
    public int hashCode() {
      int result = first.hashCode();
      result = 31 * result + Arrays.hashCode(rest);
      return result;
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
      this.literal = requireNonNull(literal, "literal");
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
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Literal literal = (Literal) o;
      return this.literal.equals(literal.literal);
    }

    @Override
    public int hashCode() {
      return literal.hashCode();
    }

    @Override
    public String toString() {
      return literal.replaceAll("[.\\[(^$\\\\]", "\\\\$0");
    }
  }

  public static final class Quantified extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex quantified;
    private final int min;
    private final int max;
    private final boolean greedy;

    public Quantified(Coregex quantified, int min, int max, boolean greedy) {
      this.quantified = requireNonNull(quantified, "quantified");
      if (min < 0 || (-1 != max && min > max)) {
        throw new IllegalArgumentException(
            "min: " + min + " and max: " + max + " has to be positive with min being <= max");
      }
      this.min = min;
      this.max = max;
      this.greedy = greedy;
    }

    @Override
    protected Map.Entry<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      StringBuilder sb = new StringBuilder(minLength());
      Map.Entry<RNG, Boolean> rngAndNext = rng.genBoolean();
      rng = rngAndNext.getKey();
      boolean next = rngAndNext.getValue();
      int quantifier = 0;
      while (quantifier < min
          || (quantified.minLength() <= remainder && (-1 == max || quantifier < max) && next)) {
        Map.Entry<RNG, String> rngAndCoregex = quantified.apply(rng, remainder);
        rng = rngAndCoregex.getKey();
        String value = rngAndCoregex.getValue();
        sb.append(value);
        remainder -= value.length();

        quantifier++;
        rngAndNext = rng.genBoolean();
        rng = rngAndNext.getKey();
        next = rngAndNext.getValue();
      }
      return new AbstractMap.SimpleEntry<>(rng, sb.toString());
    }

    @Override
    public int minLength() {
      return quantified.minLength() * min;
    }

    @Override
    public int maxLength() {
      return -1 == max ? max : quantified.maxLength() * max;
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

    public boolean greedy() {
      return greedy;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Quantified that = (Quantified) o;
      return min == that.min
          && max == that.max
          && greedy == that.greedy
          && quantified.equals(that.quantified);
    }

    @Override
    public int hashCode() {
      return Objects.hash(quantified, min, max, greedy);
    }

    @Override
    @SuppressWarnings("fallthrough")
    public String toString() {
      StringBuilder string = new StringBuilder(quantified.toString());
      switch (max) {
        case -1:
          switch (min) {
            case 0:
              string.append('*');
              break;
            case 1:
              string.append('+');
              break;
            default:
              string.append('{').append(min).append(",}");
              break;
          }
          break;
        case 1:
          if (min == 0) {
            string.append('?');
            break;
          }
          // fall through
        default:
          if (min == max) {
            string.append('{').append(min).append('}');
          } else {
            string.append('{').append(min).append(',').append(max).append('}');
          }
          break;
      }
      return (greedy ? string : string.append('?')).toString();
    }
  }

  public static final class Set extends Coregex {
    private static final long serialVersionUID = 1L;

    private final com.github.simy4.coregex.core.Set set;

    public Set(com.github.simy4.coregex.core.Set set) {
      this.set = requireNonNull(set, "set");
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
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Set set = (Set) o;
      return this.set.equals(set.set);
    }

    @Override
    public int hashCode() {
      return set.hashCode();
    }

    @Override
    public String toString() {
      return ANY == this ? "." : "[" + set + ']';
    }
  }

  public static final class Sized extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex sized;
    private final int size;

    public Sized(Coregex sized, int size) {
      this.sized = requireNonNull(sized, "sized");
      if (size < sized.minLength()) {
        throw new IllegalArgumentException(
            "size: " + size + " has to be greater than " + sized.minLength());
      }
      this.size = size;
    }

    @Override
    protected Map.Entry<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalArgumentException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      return sized.apply(rng, maxLength());
    }

    @Override
    public String generate(RNG rng) {
      return apply(requireNonNull(rng, "rng"), size).getValue();
    }

    @Override
    public int minLength() {
      return Math.min(size, sized.minLength());
    }

    @Override
    public int maxLength() {
      return -1 == sized.maxLength() ? size : Math.min(size, sized.maxLength());
    }

    @Override
    int weight() {
      return sized.weight();
    }

    public Coregex sized() {
      return sized;
    }

    public int size() {
      return size;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Sized sized = (Sized) o;
      return size == sized.size && this.sized.equals(sized.sized);
    }

    @Override
    public int hashCode() {
      return Objects.hash(sized, size);
    }

    @Override
    public String toString() {
      return sized.toString();
    }
  }

  public static final class Union extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    public Union(Coregex first, Coregex... rest) {
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
      int agg = first.maxLength();
      if (-1 == agg) {
        return agg;
      }
      for (Coregex coregex : rest) {
        int max = coregex.maxLength();
        if (-1 == max) {
          return max;
        }
        agg = Math.max(agg, max);
      }
      return agg;
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
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Union union = (Union) o;
      return first.equals(union.first) && Arrays.equals(rest, union.rest);
    }

    @Override
    public int hashCode() {
      int result = first.hashCode();
      result = 31 * result + Arrays.hashCode(rest);
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
