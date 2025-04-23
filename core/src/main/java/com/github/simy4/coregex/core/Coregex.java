/*
 * Copyright 2021-2025 Alex Simkin
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

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Data representation of regex language.
 *
 * <p><em>Effectively sealed.</em>
 *
 * @see Coregex.Concat
 * @see Coregex.Quantified
 * @see Coregex.Sized
 * @see Coregex.Union
 * @author Alex Simkin
 * @since 0.1.0
 */
public abstract class Coregex implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Lazy<Coregex> EMPTY =
      new Lazy<>(() -> Set.builder().build().quantify(0, 0, Quantified.Type.GREEDY));

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
      return Set.DOTALL.get();
    } else if (0 != (flags & Pattern.UNIX_LINES)) {
      return Set.UNIX_LINES.get();
    } else {
      return Set.ALL.get();
    }
  }

  /**
   * @return predefined constructor for empty regex.
   */
  public static Coregex empty() {
    return EMPTY.get();
  }

  /**
   * @param literal literal
   * @param flags regex flags
   * @return predefined constructor for literal regex.
   */
  public static Coregex literal(String literal, int flags) {
    if (literal.isEmpty()) {
      return empty();
    }
    Set first = Set.builder(flags).single(literal.charAt(0)).build();
    Set[] rest = new Set[literal.length() - 1];
    for (int i = 1; i < literal.length(); i++) {
      rest[i - 1] = Set.builder(flags).single(literal.charAt(i)).build();
    }
    return new Concat(first, rest);
  }

  Coregex() {}

  /**
   * Internal sampler of random strings.
   *
   * @param rng random number generator to use
   * @param remainder remaining permitted length of the string to be generated
   * @return next random number generator state with sampled string
   * @throws IllegalArgumentException if remainder is lesser than {@link #minLength()}
   */
  protected abstract String apply(Random rng, int remainder);

  /**
   * Samples one random string that matches this regex.
   *
   * @param seed random seed to use for sampling
   * @return sampled string
   */
  public final String generate(long seed) {
    int remainder = maxLength();
    remainder = -1 == remainder ? Integer.MAX_VALUE - 2 : remainder;
    return apply(new Random(seed), remainder);
  }

  /**
   * @return maximal possible length of all generated strings of this regex. {@code -1} means no
   *     upper limit.
   */
  public abstract int maxLength();

  /**
   * @return minimal possible length of all generated strings of this regex
   */
  public abstract int minLength();

  /**
   * Quantify this regex.
   *
   * @param min min number of times this regex should be repeated
   * @param max max number of times this regex should be repeated. {@code -1} means no limit.
   * @param type quantifier type.
   * @return quantified regex
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
    protected String apply(Random rng, int remainder) {
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
        String value = chunk.apply(rng, remainder + chunkMinLength);
        sb.append(value);
        remainder -= value.length() - chunkMinLength;
      } while (i < rest.length && (chunk = rest[i++]) != null);
      return sb.toString();
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

    /**
     * @return underlying regexes in order of concatenation.
     */
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

  /** Regex group. */
  public static final class Group extends Coregex {

    private static final long serialVersionUID = 2L;

    private final Type type;
    private final String name;
    private final Coregex group;

    /**
     * Unnamed capturing group.
     *
     * @param group group body
     * @see Group(Type, Coregex)
     * @see Group(Type, String, Coregex)
     */
    public Group(Coregex group) {
      this(Type.CAPTURING, requireNonNull(group, "group"));
    }

    /**
     * Unnamed group.
     *
     * @param type group type
     * @param group group body
     * @see Group(Coregex)
     * @see Group(Type, String, Coregex)
     */
    public Group(Type type, Coregex group) {
      this(requireNonNull(type, "type"), null, requireNonNull(group, "group"));
    }

    /**
     * Named group.
     *
     * @param name group name
     * @param group group body
     * @see Group(Coregex)
     * @see Group(Type, Coregex)
     */
    public Group(String name, Coregex group) {
      this(Type.NAMED, requireNonNull(name, "name"), requireNonNull(group, "group"));
    }

    private Group(Type type, String name, Coregex group) {
      this.type = type;
      this.name = name;
      this.group = group;
    }

    /** {@inheritDoc} */
    @Override
    protected String apply(Random rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalArgumentException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      return group.apply(rng, remainder);
    }

    /** {@inheritDoc} */
    @Override
    public int maxLength() {
      return group.maxLength();
    }

    /** {@inheritDoc} */
    @Override
    public int minLength() {
      return group.minLength();
    }

    /**
     * @return group type
     */
    public Type type() {
      return type;
    }

    /**
     * @return group name if group is a named group
     */
    public Optional<String> name() {
      return Optional.ofNullable(name);
    }

    /**
     * @return group body
     */
    public Coregex group() {
      return group;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Group group = (Group) o;
      return type == group.type
          && Objects.equals(this.name, group.name)
          && this.group.equals(group.group);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, name, group);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("(");
      switch (type) {
        case NON_CAPTURING:
          sb.append("?:");
          break;
        case ATOMIC:
          sb.append("?>");
          break;
        case NAMED:
          sb.append("?<").append(name).append('>');
          break;
        case LOOKAHEAD:
          sb.append("?=");
          break;
        case LOOKBEHIND:
          sb.append("?<=");
          break;
        case NEGATIVE_LOOKAHEAD:
          sb.append("?!");
          break;
        case NEGATIVE_LOOKBEHIND:
          sb.append("?<!");
          break;
        default:
          break;
      }
      return sb.append(group).append(')').toString();
    }

    /** Regex group type. * */
    public enum Type {
      NON_CAPTURING,
      CAPTURING,
      ATOMIC,
      NAMED,
      LOOKAHEAD,
      LOOKBEHIND,
      NEGATIVE_LOOKAHEAD,
      NEGATIVE_LOOKBEHIND
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
    protected String apply(Random rng, int remainder) {
      int minLength = minLength();
      int quantifiedMinLength = quantified.minLength();
      if (remainder < minLength) {
        throw new IllegalStateException(
            "remainder: " + remainder + " has to be greater than " + minLength);
      }
      StringBuilder sb = new StringBuilder(minLength + 16);
      int quantifier = 0;
      remainder -= minLength;
      for (; quantifier < min; quantifier++) {
        String value = quantified.apply(rng, remainder + quantifiedMinLength);
        sb.append(value);
        remainder -= value.length() - quantifiedMinLength;
      }
      int max = -1 == this.max ? remainder / Math.max(1, quantifiedMinLength) : this.max;
      while (quantifiedMinLength <= remainder && quantifier++ < max) {
        if (0 == rng.nextInt(4)) {
          break;
        }

        String value = quantified.apply(rng, remainder);
        sb.append(value);
        remainder -= value.length();
      }
      return sb.toString();
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

    /**
     * @return quantified regex
     */
    public Coregex quantified() {
      return quantified;
    }

    /**
     * @return min number of times this regex should be repeated
     */
    public int min() {
      return min;
    }

    /**
     * @return max number of times this regex should be repeated. {@code -1} means no limit.
     */
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
      string.append(quantified);
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
      POSSESSIVE,
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
    protected String apply(Random rng, int remainder) {
      if (remainder < minLength()) {
        throw new IllegalArgumentException(
            "remainder: " + remainder + " has to be greater than " + minLength());
      }
      return sized.apply(rng, Math.min(remainder, maxLength()));
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

    /**
     * @return sized regex
     */
    public Coregex sized() {
      return sized;
    }

    /**
     * @return preferred size of generated string
     */
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
    protected String apply(Random rng, int remainder) {
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

      int index = rng.nextInt(fits.size());
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

    /**
     * @return underlying regexes forming this unification.
     */
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
      StringJoiner joiner = new StringJoiner("|");
      joiner.add(first.toString());
      for (Coregex coregex : rest) {
        joiner.add(coregex.toString());
      }
      return joiner.toString();
    }
  }
}
