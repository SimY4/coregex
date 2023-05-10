/*
 * Copyright 2021-2023 Alex Simkin
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

import static com.github.simy4.coregex.core.Pair.pair;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

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

  /**
   * @return predefined constructor for regex that matches any character.
   * @see #any(int)
   */
  public static Coregex any() {
    return any(0);
  }

  /**
   * @param flags regex flags
   * @return predefined constructor for regex that matches any character.
   * @see #any()
   */
  public static Coregex any(int flags) {
    if (0 != (flags & Pattern.DOTALL)) {
      return new Set(com.github.simy4.coregex.core.Set.DOTALL.get());
    } else if (0 != (flags & Pattern.UNIX_LINES)) {
      return new Set(com.github.simy4.coregex.core.Set.UNIX_LINES.get());
    } else {
      return new Set(com.github.simy4.coregex.core.Set.ALL.get());
    }
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
   * @throws IllegalArgumentException if remainder is lesser than {@link #minLength()}
   */
  protected abstract Pair<RNG, String> apply(RNG rng, int remainder);

  /**
   * @return maximal possible length of all generated strings of this coregex. {@code -1} means no
   *     upper limit.
   */
  public abstract int maxLength();

  /** @return minimal possible length of all generated strings of this coregex */
  public abstract int minLength();

  /**
   * Creates a version of this coregex with {@link #test(CharSequence)} returning inverted result
   * from the original.
   *
   * <pre>{@code
   * ¬''       = ''
   * ¬[abc]    = [^abc]
   * ¬'abc'    = [^a][^b][^c]
   * ¬(a|b)    = ¬a&¬b
   * ¬(a&b)    = ¬a|¬b
   * ¬(a*)     = ¬a{1}
   * ¬(a+)     = ''
   * ¬(a{0,3}) = a{4,}
   * ¬(a{4,})  = a{0,3}
   * ¬(a{2})   = a{0,1}|a{3,}
   * ¬(a{2,3}) = a{0,1}|a{4,}
   *
   * ¬.        = ∅
   * }</pre>
   *
   * @see #test(CharSequence)
   * @return negated coregex instance
   */
  public abstract Coregex negate();

  /** @return simplified and more memory efficient version of this coregex. */
  public abstract Coregex simplify();

  /**
   * Tests that given input matches this coregex. In a way this method verifies that coregex in an
   * inverse of a regular expression.
   *
   * <p>This should hold true for a supported subset of regular expressions:
   *
   * <pre>{@code
   * Pattern pattern = ...
   * pattern.matcher().matches(input) == Coregex.from(pattern).test(input)
   * }</pre>
   *
   * @param input input to test
   * @return {@code true} if input matches this regular expression or {@code false} otherwise
   * @see #negate()
   */
  public abstract boolean test(CharSequence input);

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
   * Quantify this coregex.
   *
   * @param min min number of times this regex should be repeated
   * @param max max number of times this regex should be repeated. {@code -1} means no limit.
   * @param type quantifier type.
   * @return quantified coregex
   * @see Quantified
   * @throws IllegalArgumentException if min is greater than max or if min is negative or if called
   *     on already quantified regex
   * @see Quantified.Type
   */
  public final Coregex quantify(int min, int max, Quantified.Type type) {
    return 1 == min && 1 == max ? this : new Quantified(this, min, max, type);
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
      int minLength = minLength();
      if (remainder < minLength) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength);
      }
      StringBuilder sb = new StringBuilder(minLength + 16);
      remainder -= minLength;
      Coregex chunk = first;
      int i = 0;
      do {
        int chunkMinLength = chunk.minLength();
        Pair<RNG, String> rngAndCoregex = chunk.apply(rng, remainder + chunkMinLength);
        rng = rngAndCoregex.getFirst();
        String value = rngAndCoregex.getSecond();
        sb.append(value);
        remainder -= value.length() - chunkMinLength;
      } while (i < rest.length && (chunk = rest[i++]) != null);
      return pair(rng, sb.toString());
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
    public int minLength() {
      int min = first.minLength();
      for (Coregex coregex : rest) {
        min += coregex.minLength();
      }
      return min;
    }

    /** {@inheritDoc} */
    @Override
    public Coregex negate() {
      List<Coregex> negated = new ArrayList<>(rest.length + 1);
      if (0 != first.minLength() || 0 != first.maxLength()) {
        negated.add(first.negate());
      }
      for (Coregex coregex : rest) {
        if (0 != coregex.minLength() || 0 != coregex.maxLength()) {
          negated.add(coregex.negate());
        }
      }
      if (negated.isEmpty()) {
        return empty();
      } else if (1 == negated.size()) {
        return negated.get(0);
      } else {
        return new Concat(
            negated.get(0), negated.subList(1, negated.size()).toArray(new Coregex[0]));
      }
    }

    /** {@inheritDoc} */
    @Override
    public Coregex simplify() {
      List<Coregex> concat = new ArrayList<>(rest.length + 1);
      Coregex simplified = first.simplify();
      if (0 != simplified.minLength() || 0 != simplified.maxLength()) {
        concat.add(simplified);
      }
      for (Coregex coregex : rest) {
        simplified = coregex.simplify();
        if (0 != simplified.minLength() || 0 != simplified.maxLength()) {
          concat.add(simplified);
        }
      }
      if (concat.isEmpty()) {
        return empty();
      } else if (1 == concat.size()) {
        return concat.get(0);
      } else {
        return new Concat(concat.get(0), concat.subList(1, concat.size()).toArray(new Coregex[0]));
      }
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(CharSequence input) {
      java.util.Set<Integer> indices = new HashSet<>();
      int minLength = first.minLength(), maxLength = first.maxLength();
      maxLength = -1 == maxLength ? input.length() : Math.min(input.length(), maxLength);
      for (int i = minLength; i <= maxLength; i++) {
        if (first.test(input.subSequence(0, i))) {
          indices.add(i);
        }
      }
      if (indices.isEmpty()) {
        return false;
      }

      for (Coregex coregex : rest) {
        java.util.Set<Integer> newIndices = new HashSet<>();
        for (int startAt : indices) {
          minLength = startAt + coregex.minLength();
          maxLength = coregex.maxLength();
          maxLength =
              -1 == maxLength ? input.length() : Math.min(input.length(), startAt + maxLength);
          for (int i = minLength; i <= maxLength; i++) {
            if (coregex.test(input.subSequence(startAt, i))) {
              newIndices.add(i);
            }
          }
        }
        if (newIndices.isEmpty()) {
          return false;
        }
        indices = newIndices;
      }
      return indices.contains(input.length());
    }

    /** @return underlying regexes in order of concatenation. */
    public List<Coregex> concat() {
      List<Coregex> concat = new ArrayList<>(rest.length + 1);
      concat.add(first);
      concat.addAll(Arrays.asList(rest));
      return concat;
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

  /** Intersection of regexes. */
  public static final class Intersection extends Coregex {
    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    /**
     * @param first first regex
     * @param rest rest of regexes
     */
    public Intersection(Coregex first, Coregex... rest) {
      this.first = requireNonNull(first, "first");
      this.rest = Arrays.copyOf(rest, rest.length);
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
      int attempt = 0;
      String result;
      boolean matches;
      do {
        matches = false;
        Pair<RNG, String> rngAndResult = first.apply(rng, remainder);
        rng = rngAndResult.getFirst();
        result = rngAndResult.getSecond();
        for (Coregex coregex : rest) {
          matches = coregex.test(result);
          if (matches) {
            break;
          }
        }
      } while (!matches && ++attempt < 100);
      if (!matches) {
        throw new IllegalStateException("Unable to generate intersection for: " + this);
      }
      return pair(rng, result);
    }

    /** {@inheritDoc} */
    @Override
    public int maxLength() {
      int agg = first.maxLength();
      for (Coregex coregex : rest) {
        int max = coregex.maxLength();
        if (-1 == max) {
          continue;
        }
        agg = -1 == agg ? max : Math.min(agg, max);
      }
      return agg;
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      int min = first.minLength();
      for (Coregex coregex : rest) {
        min = Math.max(min, coregex.minLength());
      }
      return min;
    }

    /** {@inheritDoc} */
    @Override
    public Coregex negate() {
      if (0 == rest.length) {
        return first.negate();
      }
      Coregex[] negatedRest = new Coregex[rest.length];
      for (int i = 0; i < rest.length; i++) {
        negatedRest[i] = rest[i].negate();
      }
      return new Intersection(first.simplify(), negatedRest);
    }

    /** {@inheritDoc} */
    @Override
    public Coregex simplify() {
      if (0 == rest.length) {
        return first.simplify();
      }
      Coregex[] simplifiedRest = new Coregex[rest.length];
      for (int i = 0; i < rest.length; i++) {
        simplifiedRest[i] = rest[i].simplify();
      }
      return new Intersection(first.simplify(), simplifiedRest);
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(CharSequence input) {
      if (!first.test(input)) {
        return false;
      }
      for (Coregex coregex : rest) {
        if (!coregex.test(input)) {
          return false;
        }
      }
      return true;
    }

    /** @return underlying regexes forming this intersection. */
    public List<Coregex> intersection() {
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
      Intersection intersect = (Intersection) o;
      return first.equals(intersect.first) && Arrays.equals(rest, intersect.rest);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(first);
      result = 31 * result + Arrays.hashCode(rest);
      return result;
    }

    @Override
    public String toString() {
      StringJoiner joiner = new StringJoiner("&", "(?:", ")");
      joiner.add(first.toString());
      for (Coregex coregex : rest) {
        joiner.add(coregex.toString());
      }
      return joiner.toString();
    }
  }

  /** Literal string regex. */
  public static final class Literal extends Coregex {
    private static final long serialVersionUID = 1L;

    private final String literal;
    private final int flags;

    /**
     * @param literal literal
     * @see Literal(String, int)
     */
    public Literal(String literal) {
      this(literal, 0);
    }

    /**
     * @param literal literal
     * @param flags regex flags
     * @see Literal(String)
     */
    public Literal(String literal, int flags) {
      this.literal = requireNonNull(literal, "literal");
      this.flags = flags;
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      Pair<RNG, Boolean> rngAndBoolean;
      if (0 != (flags & Pattern.CASE_INSENSITIVE)) {
        StringBuilder literal = new StringBuilder(this.literal);
        for (int i = 0; i < literal.length(); i++) {
          char ch = literal.charAt(i);
          if (Character.isLowerCase(ch)) {
            rngAndBoolean = rng.genBoolean();
            rng = rngAndBoolean.getFirst();
            if (rngAndBoolean.getSecond()) {
              literal.setCharAt(i, Character.toUpperCase(ch));
            }
          }
          if (Character.isUpperCase(ch)) {
            rngAndBoolean = rng.genBoolean();
            rng = rngAndBoolean.getFirst();
            if (rngAndBoolean.getSecond()) {
              literal.setCharAt(i, Character.toLowerCase(ch));
            }
          }
        }
        return pair(rng, literal.toString());
      } else {
        rngAndBoolean =
            rng.genBoolean(); // need to burn one random number to make result deterministic
        return pair(rngAndBoolean.getFirst(), literal);
      }
    }

    /** {@inheritDoc} */
    @Override
    public int maxLength() {
      return literal.length();
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      return literal.length();
    }

    @Override
    public Coregex negate() {
      if (literal.isEmpty()) {
        return empty();
      }
      char[] chars = literal.toCharArray();
      Coregex negatedFirst =
          new Set(
              com.github.simy4.coregex.core.Set.builder(flags).single(chars[0]).negate().build());
      Coregex[] negatedRest = new Coregex[chars.length - 1];
      for (int i = 0; i < negatedRest.length; i++) {
        negatedRest[i] =
            new Set(
                com.github.simy4.coregex.core.Set.builder(flags)
                    .single(chars[i + 1])
                    .negate()
                    .build());
      }
      return new Concat(negatedFirst, negatedRest);
    }

    /** {@inheritDoc} */
    @Override
    public Coregex simplify() {
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(CharSequence input) {
      if (0 != (flags & Pattern.CASE_INSENSITIVE)) {
        return literal.equalsIgnoreCase(input.toString());
      } else {
        return literal.equals(input.toString());
      }
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
    private final Type type;

    /**
     * Greedily quantified regex with no upper limit.
     *
     * @param quantified quantified regex
     * @param min min number of times this regex should be repeated
     * @throws IllegalArgumentException if min is negative
     * @see Quantified(Coregex, int, int)
     * @see Quantified(Coregex, int, int, Type)
     */
    public Quantified(Coregex quantified, int min) {
      this(quantified, min, -1, Type.GREEDY);
    }

    /**
     * Greedily quantified regex.
     *
     * @param quantified quantified regex
     * @param min min number of times this regex should be repeated
     * @param max max number of times this regex should be repeated. {@code -1} means no limit.
     * @throws IllegalArgumentException if min is greater than max or if min is negative
     * @see Quantified(Coregex, int)
     * @see Quantified(Coregex, int, int, Type)
     */
    public Quantified(Coregex quantified, int min, int max) {
      this(quantified, min, max, Type.GREEDY);
    }

    /**
     * @param quantified quantified regex
     * @param min min number of times this regex should be repeated
     * @param max max number of times this regex should be repeated. {@code -1} means no limit.
     * @param type quantifier type.
     * @throws IllegalArgumentException if min is greater than max or if min is negative
     * @see Quantified(Coregex, int)
     * @see Quantified(Coregex, int, int)
     * @see Type
     */
    public Quantified(Coregex quantified, int min, int max, Type type) {
      this.quantified = requireNonNull(quantified, "quantified");
      if (min < 0 || (-1 != max && min > max)) {
        throw new IllegalArgumentException(
            "min: " + min + " and max: " + max + " has to be positive with min being <= max");
      }
      this.min = min;
      this.max = max;
      this.type = requireNonNull(type, "type");
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
      int minLength = minLength(), quantifiedMinLength = quantified.minLength();
      if (remainder < minLength) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength);
      }
      StringBuilder sb = new StringBuilder(minLength + 16);
      int quantifier = 0;
      remainder -= minLength;
      for (; quantifier < min; quantifier++) {
        Pair<RNG, String> rngAndCoregex = quantified.apply(rng, remainder + quantifiedMinLength);
        rng = rngAndCoregex.getFirst();
        String value = rngAndCoregex.getSecond();
        sb.append(value);
        remainder -= value.length() - quantifiedMinLength;
      }
      while (quantifiedMinLength <= remainder && (-1 == max || quantifier++ < max)) {
        Pair<RNG, Boolean> rngAndNext = rng.genBoolean();
        rng = rngAndNext.getFirst();
        if (!rngAndNext.getSecond()) {
          break;
        }

        Pair<RNG, String> rngAndCoregex = quantified.apply(rng, remainder);
        rng = rngAndCoregex.getFirst();
        String value = rngAndCoregex.getSecond();
        sb.append(value);
        remainder -= value.length();
      }
      return pair(rng, sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public int maxLength() {
      int maxLength;
      return -1 == max || -1 == (maxLength = quantified.maxLength()) ? -1 : maxLength * max;
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      return quantified.minLength() * min;
    }

    /** {@inheritDoc} */
    @Override
    public Coregex negate() {
      if (0 == quantified.minLength() && 0 == quantified.maxLength()) {
        return Coregex.empty();
      } else if (0 == min && -1 == max) {
        return quantified.negate();
      } else if (1 == min && -1 == max) {
        return new Quantified(quantified, 0, 0, type);
      } else if (0 == min) {
        return new Quantified(quantified, max + 1, -1, type);
      } else if (-1 == max) {
        return new Quantified(quantified, 0, min - 1, type);
      } else {
        return new Coregex.Union(
            new Quantified(quantified, 0, min - 1, type),
            new Quantified(quantified, max + 1, -1, type));
      }
    }

    /** {@inheritDoc} */
    @Override
    public Coregex simplify() {
      Coregex simplified = this.quantified.simplify();
      if (0 == simplified.minLength() && 0 == simplified.maxLength()) {
        return Coregex.empty();
      } else if (1 == min && 1 == max) {
        return simplified;
      } else {
        return new Quantified(simplified, min, max, type);
      }
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(CharSequence input) {
      java.util.Set<Integer> indices = new HashSet<>(Collections.singleton(0));
      int quantifiedMinLength = quantified.minLength(),
          quantifiedMaxLength = quantified.maxLength(),
          quantifier = 0;
      for (; quantifier < min; quantifier++) {
        java.util.Set<Integer> newIndices = new HashSet<>();
        for (int startAt : indices) {
          int minLength = startAt + quantifiedMinLength;
          int maxLength = quantifiedMaxLength;
          maxLength =
              -1 == maxLength ? input.length() : Math.min(input.length(), startAt + maxLength);
          for (int i = minLength; i <= maxLength; i++) {
            if (quantified.test(input.subSequence(startAt, i))) {
              newIndices.add(i);
            }
          }
        }
        if (newIndices.isEmpty()) {
          return false;
        }
        indices = newIndices;
      }
      if (indices.contains(input.length())) {
        return true;
      }

      while (-1 == max || quantifier++ < max) {
        java.util.Set<Integer> newIndices = new HashSet<>();
        for (int startAt : indices) {
          int minLength = startAt + quantifiedMinLength;
          int maxLength = quantifiedMaxLength;
          maxLength =
              -1 == maxLength ? input.length() : Math.min(input.length(), startAt + maxLength);
          for (int i = minLength; i <= maxLength; i++) {
            if (quantified.test(input.subSequence(startAt, i))) {
              if (i + 1 == input.length()) {
                return true;
              }
              newIndices.add(i);
            }
          }
        }
        if (!indices.addAll(newIndices)) {
          break;
        }
      }
      return indices.contains(input.length());
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

    /**
     * @return quantifier type. Currently, doesn't affect the generation flow - only display.
     * @see Type
     */
    public Type type() {
      return type;
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
          && type == that.type
          && quantified.equals(that.quantified);
    }

    @Override
    public int hashCode() {
      return Objects.hash(quantified, min, max, type);
    }

    @Override
    @SuppressWarnings("fallthrough")
    public String toString() {
      StringBuilder string = new StringBuilder();
      boolean wrapInBraces = quantified instanceof Concat;
      if (wrapInBraces) {
        string.append('(').append(quantified).append(')');
      } else {
        string.append(quantified);
      }
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
      switch (type) {
        case RELUCTANT:
          string.append('?');
          break;
        case POSSESSIVE:
          string.append('+');
          break;
        default:
          break;
      }
      return string.toString();
    }

    /** Quantifier type. */
    public enum Type {
      GREEDY,
      RELUCTANT,
      POSSESSIVE
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
      rng = rngAndSeed.getFirst();
      String sample = String.valueOf(set.sample(rngAndSeed.getSecond()));
      return pair(rng, sample);
    }

    /** {@inheritDoc} */
    @Override
    public int maxLength() {
      return 1;
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      return 1;
    }

    /** {@inheritDoc} */
    @Override
    public Coregex negate() {
      return new Set(com.github.simy4.coregex.core.Set.builder().set(set).negate().build());
    }

    /** {@inheritDoc} */
    @Override
    public Coregex simplify() {
      return this;
    }

    @Override
    public boolean test(CharSequence input) {
      return 1 == input.length() && set.test(input.charAt(0));
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
      return set.toString();
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
     * @throws IllegalArgumentException if size is lesser than {@link #minLength()}
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
    public int maxLength() {
      return -1 == sized.maxLength() ? size : Math.min(size, sized.maxLength());
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      return Math.min(size, sized.minLength());
    }

    /** {@inheritDoc} */
    @Override
    public Coregex negate() {
      return new Sized(sized.negate(), size);
    }

    /** {@inheritDoc} */
    @Override
    public Coregex simplify() {
      Coregex simplified = sized.simplify();
      if (simplified instanceof Sized) {
        Sized sized = (Sized) simplified;
        return new Sized(sized.sized(), Math.min(size, sized.size()));
      } else {
        return new Sized(simplified, size);
      }
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(CharSequence input) {
      return sized.test(input);
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
      if (first.minLength() <= remainder) {
        fits.add(first);
      }
      for (Coregex coregex : rest) {
        if (coregex.minLength() <= remainder) {
          fits.add(coregex);
        }
      }
      if (fits.isEmpty()) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }

      Pair<RNG, Integer> rngAndIndex = rng.genInteger(fits.size());
      rng = rngAndIndex.getFirst();
      int index = rngAndIndex.getSecond();
      return fits.get(index).apply(rng, remainder);
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
    public int minLength() {
      int min = first.minLength();
      for (Coregex coregex : rest) {
        min = Math.min(min, coregex.minLength());
      }
      return min;
    }

    /** {@inheritDoc} */
    @Override
    public Coregex negate() {
      if (0 == rest.length) {
        return first.negate();
      }
      Coregex[] negatedRest = new Coregex[rest.length];
      for (int i = 0; i < rest.length; i++) {
        negatedRest[i] = rest[i].negate();
      }
      return new Intersection(first.negate(), negatedRest);
    }

    /** {@inheritDoc} */
    @Override
    public Coregex simplify() {
      if (0 == rest.length) {
        return first.simplify();
      }
      Coregex[] simplifiedRest = new Coregex[rest.length];
      for (int i = 0; i < rest.length; i++) {
        simplifiedRest[i] = rest[i].simplify();
      }
      return new Union(first.simplify(), simplifiedRest);
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(CharSequence input) {
      if (first.test(input)) {
        return true;
      }
      for (Coregex coregex : rest) {
        if (coregex.test(input)) {
          return true;
        }
      }
      return false;
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
