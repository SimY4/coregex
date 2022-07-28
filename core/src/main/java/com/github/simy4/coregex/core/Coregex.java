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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Data representation of regex language.
 *
 * <p><em>Effectively sealed.</em>
 *
 * @see Coregex.Concat
 * @see Coregex.Literal
 * @see Coregex.Quantified
 * @see Coregex.Set
 * @see Coregex.Sized
 * @see Coregex.Union
 * @author Alex Simkin
 * @since 0.1.0
 */
public abstract class Coregex implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs {@link Coregex} from provided {@link Pattern} instance.
   *
   * @param pattern regular expression to parse.
   * @return parsed coregex instance.
   * @throws UnsupportedOperationException if provided pattern constructs are not yet supported.
   */
  public static Coregex from(Pattern pattern) {
    return CoregexParser.getInstance().parse(pattern);
  }

  /** @return predefined constructor for regex that matches any character. */
  public static Coregex any() {
    return new Set(com.github.simy4.coregex.core.Set.ALL);
  }

  /** @return predefined constructor for empty regex. */
  public static Coregex empty() {
    return new Literal("");
  }

  private Coregex() {}

  /**
   * Internal sampler of random strings.
   *
   * @param rng random number generator to use
   * @param remainder remaining permitted length of the string to be generated
   * @return next random number generator state with sampled string
   */
  protected abstract Pair<RNG, String> apply(RNG rng, int remainder);

  /**
   * Samples one random string that matches this regex.
   *
   * @param rng random number generator to use
   * @return sampled string
   */
  public final String generate(RNG rng) {
    int remainder = maxLength();
    remainder = -1 == remainder ? Integer.MAX_VALUE - 2 : remainder;
    return apply(requireNonNull(rng, "rng"), remainder).getSecond();
  }

  /**
   * Quantify this regex.
   *
   * @param min min number of times this regex should be repeated
   * @param max max number of times this regex should be repeated. {@code -1} means no limit.
   * @return quantified regex
   * @see Quantified
   * @throws IllegalArgumentException if min is greater than max or if min is negative or if called
   *     on already quantified regex
   */
  public final Coregex quantify(int min, int max, boolean greedy) {
    return 1 == min && 1 == max ? this : new Quantified(this, min, max, greedy);
  }

  /**
   * Size this regex. Generated string will be at most this long.
   *
   * @param size preferred size of generated string
   * @return sized regex
   * @see Sized
   * @throws IllegalArgumentException if size is lesser than {@link #minLength()}
   */
  public final Coregex sized(int size) {
    int maxLength = maxLength();
    return -1 != maxLength && maxLength <= size ? this : new Sized(this, size);
  }

  /** @return minimal possible length of all generated strings of this regex */
  public abstract int minLength();

  /**
   * @return maximal possible length of all generated strings of this regex. {@code -1} means no
   *     upper limit.
   */
  public abstract int maxLength();

  /**
   * @return weight of this regex. Used in sampling between multiple options.
   * @see Union
   */
  abstract int weight();

  /** Sequential concatenation of regexes. */
  public static final class Concat extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    /**
     * @param first first regex
     * @param rest rest of regexes
     */
    public Concat(Coregex first, Coregex... rest) {
      this.first = requireNonNull(first, "first");
      this.rest = Arrays.copyOf(rest, rest.length);
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      StringBuilder sb = new StringBuilder(minLength() + 16);
      Coregex chunk = first;
      int i = 0;
      do {
        Pair<RNG, String> rngAndCoregex =
            chunk.apply(rng, remainder - minLength() + chunk.minLength());
        rng = rngAndCoregex.getFirst();
        String value = rngAndCoregex.getSecond();
        sb.append(value);
        remainder -= (value.length() - chunk.minLength());
      } while (i < rest.length && (chunk = rest[i++]) != null);
      return new Pair<>(rng, sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      int min = first.minLength();
      for (Coregex coregex : rest) {
        min += coregex.minLength();
      }
      return min;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    int weight() {
      int weight = first.weight();
      for (Coregex coregex : rest) {
        weight += coregex.weight();
      }
      return weight / (rest.length + 1);
    }

    /** @return underlying regexes in order of concatenation. */
    public List<Coregex> concat() {
      List<Coregex> concat = new ArrayList<>(rest.length + 1);
      concat.add(first);
      concat.addAll(Arrays.asList(rest));
      return concat;
    }

    /** @return simplified and more memory efficient version of this regex. */
    public Coregex simplify() {
      List<Coregex> concat = new ArrayList<>(rest.length + 1);
      concat.add(first);
      int idx = 0;
      for (Coregex coregex : rest) {
        Coregex last;
        if (coregex instanceof Literal && (last = concat.get(idx)) instanceof Literal) {
          concat.set(idx, new Literal(((Literal) last).literal + ((Literal) coregex).literal));
        } else {
          concat.add(coregex);
          idx++;
        }
      }
      return rest.length + 1 == concat.size()
          ? this
          : 1 == concat.size()
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

  /** Literal string regex. */
  public static final class Literal extends Coregex {
    private static final long serialVersionUID = 1L;

    private final String literal;

    /** @param literal literal */
    public Literal(String literal) {
      this.literal = requireNonNull(literal, "literal");
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      return new Pair<>(rng, literal);
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      return literal.length();
    }

    /** {@inheritDoc} */
    @Override
    public int maxLength() {
      return literal.length();
    }

    /** {@inheritDoc} */
    @Override
    int weight() {
      return literal.length();
    }

    /** @return literal */
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
      return -1 == literal.indexOf('\\') ? literal : Pattern.quote(literal);
    }
  }

  /** Quantified regex. */
  public static final class Quantified extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex quantified;
    private final int min;
    private final int max;
    private final boolean greedy;

    /**
     * @param quantified quantified regex
     * @param min min number of times this regex should be repeated
     * @param max max number of times this regex should be repeated. {@code -1} means no limit.
     * @throws IllegalArgumentException if min is greater than max or if min is negative or if
     *     called on already quantified regex
     */
    public Quantified(Coregex quantified, int min, int max, boolean greedy) {
      if (quantified instanceof Quantified) {
        throw new IllegalArgumentException("already quantified regex: " + quantified);
      }
      this.quantified = requireNonNull(quantified, "quantified");
      if (min < 0 || (-1 != max && min > max)) {
        throw new IllegalArgumentException(
            "min: " + min + " and max: " + max + " has to be positive with min being <= max");
      }
      this.min = min;
      this.max = max;
      this.greedy = greedy;
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      StringBuilder sb = new StringBuilder(minLength() + 16);
      Pair<RNG, Boolean> rngAndNext = rng.genBoolean();
      rng = rngAndNext.getFirst();
      boolean next = rngAndNext.getSecond();
      int quantifier = 0;
      while (quantifier < min
          || (quantified.minLength() <= remainder && (-1 == max || quantifier < max) && next)) {
        Pair<RNG, String> rngAndCoregex = quantified.apply(rng, remainder);
        rng = rngAndCoregex.getFirst();
        String value = rngAndCoregex.getSecond();
        sb.append(value);
        remainder -= value.length();

        quantifier++;
        rngAndNext = rng.genBoolean();
        rng = rngAndNext.getFirst();
        next = rngAndNext.getSecond();
      }
      return new Pair<>(rng, sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      return quantified.minLength() * min;
    }

    /** {@inheritDoc} */
    @Override
    public int maxLength() {
      return -1 == max ? max : quantified.maxLength() * max;
    }

    /** {@inheritDoc} */
    @Override
    int weight() {
      return quantified.weight();
    }

    /** @return quantified regex */
    public Coregex quantified() {
      return quantified;
    }

    /** @return min number of times this regex should be repeated */
    public int min() {
      return min;
    }

    /** @return max number of times this regex should be repeated. {@code -1} means no limit. */
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

  /**
   * Character class regex.
   *
   * @see com.github.simy4.coregex.core.Set
   */
  public static final class Set extends Coregex {
    private static final long serialVersionUID = 1L;

    private final com.github.simy4.coregex.core.Set set;

    /** @param set set of characters */
    public Set(com.github.simy4.coregex.core.Set set) {
      this.set = requireNonNull(set, "set");
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      Pair<RNG, Long> rngAndSeed = rng.genLong();
      return new Pair<>(rng, String.valueOf(set.generate(rngAndSeed.getSecond())));
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      return 1;
    }

    /** {@inheritDoc} */
    @Override
    public int maxLength() {
      return 1;
    }

    /** {@inheritDoc} */
    @Override
    int weight() {
      return set.weight();
    }

    /** @return set of characters */
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
      return com.github.simy4.coregex.core.Set.ALL == set ? "." : "[" + set + ']';
    }
  }

  /** Sized regex. */
  public static final class Sized extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex sized;
    private final int size;

    /**
     * @param sized sized regex
     * @param size preferred size of generated string
     */
    public Sized(Coregex sized, int size) {
      this.sized = requireNonNull(sized, "sized");
      if (size < sized.minLength()) {
        throw new IllegalArgumentException(
            "size: " + size + " has to be greater than " + sized.minLength());
      }
      this.size = size;
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalArgumentException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      return sized.apply(rng, maxLength());
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      return Math.min(size, sized.minLength());
    }

    /** {@inheritDoc} */
    @Override
    public int maxLength() {
      return -1 == sized.maxLength() ? size : Math.min(size, sized.maxLength());
    }

    /** {@inheritDoc} */
    @Override
    int weight() {
      return sized.weight();
    }

    /** @return sized regex */
    public Coregex sized() {
      return sized;
    }

    /** @return preferred size of generated string */
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

  /** Unification of regexes. */
  public static final class Union extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    /**
     * @param first first regex
     * @param rest rest of regexes
     */
    public Union(Coregex first, Coregex... rest) {
      this.first = requireNonNull(first, "first");
      this.rest = Arrays.copyOf(rest, rest.length);
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
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

      Pair<RNG, Integer> rngAndWeightedSeed = rng.genInteger(0, weight);
      rng = rngAndWeightedSeed.getFirst();
      int sample = rngAndWeightedSeed.getSecond();
      int threshold = 0;
      for (Coregex coregex : fits) {
        threshold += coregex.weight();
        if (sample < threshold) {
          return coregex.apply(rng, remainder);
        }
      }
      return fits.get(fits.size() - 1).apply(rng, remainder);
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      int min = first.minLength();
      for (Coregex coregex : rest) {
        min = Math.min(min, coregex.minLength());
      }
      return min;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    int weight() {
      int result = first.weight();
      for (Coregex coregex : rest) {
        result += coregex.weight();
      }
      return result;
    }

    /** @return underlying regexes forming this unification. */
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
