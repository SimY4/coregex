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
import java.util.OptionalInt;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  /**
   * @return predefined constructor for empty regex.
   */
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
   * @return simplified and more memory efficient version of this regex.
   */
  public abstract Coregex simplify();

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
      return new Pair<>(rng, sb.toString());
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
    public Coregex simplify() {
      List<Coregex> concat =
          concat().stream()
              .flatMap(
                  coregex -> {
                    coregex = coregex.simplify();
                    if (coregex instanceof Concat) {
                      return ((Concat) coregex).concat().stream();
                    } else if ((0 == coregex.minLength() && 0 == coregex.maxLength())) {
                      return Stream.empty();
                    } else {
                      return Stream.of(coregex);
                    }
                  })
              .collect(Collectors.toList());
      if (concat.isEmpty()) {
        return Coregex.empty();
      } else if (1 == concat.size()) {
        return concat.get(0);
      } else {
        return new Concat(concat.get(0), concat.subList(1, concat.size()).toArray(new Coregex[0]));
      }
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
    private static final long serialVersionUID = 1L;

    private final int index;
    private final String name;
    private final Coregex group;

    /**
     * Non-capturing group.
     *
     * @param group group body
     * @see Group(int, Coregex)
     * @see Group(int, String, Coregex)
     */
    public Group(Coregex group) {
      this(-1, null, group);
    }

    /**
     * Capturing group.
     *
     * @param index group index
     * @param group group body
     * @throws IllegalArgumentException if index is negative
     * @see Group(Coregex)
     * @see Group(int, String, Coregex)
     */
    public Group(int index, Coregex group) {
      this(index, null, group);
    }

    /**
     * Named capturing group.
     *
     * @param index group index
     * @param name group name
     * @param group group body
     * @throws IllegalArgumentException if index is negative
     * @see Group(Coregex)
     * @see Group(int, Coregex)
     */
    public Group(int index, String name, Coregex group) {
      if (-1 != index && 0 > index) {
        throw new IllegalArgumentException("index: " + index + " has to be positive");
      }
      this.index = index;
      this.name = name;
      this.group = requireNonNull(group, "group");
    }

    /** {@inheritDoc} */
    @Override
    protected Pair<RNG, String> apply(RNG rng, int remainder) {
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

    /** {@inheritDoc} */
    @Override
    public Coregex simplify() {
      Coregex simplified = group.simplify();
      if (0 == index && null == name) {
        return simplified;
      } else {
        return new Group(index, name, simplified);
      }
    }

    /**
     * @return group index if group is a capturing group
     */
    public OptionalInt index() {
      return -1 == index ? OptionalInt.empty() : OptionalInt.of(index);
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
      return index == group.index
          && Objects.equals(this.name, group.name)
          && this.group.equals(group.group);
    }

    @Override
    public int hashCode() {
      return Objects.hash(index, name, group);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("(");
      if (null != name) {
        sb.append("?<").append(name).append('>');
      }
      if (0 > index) {
        sb.append("?:");
      }
      return sb.append(group).append(')').toString();
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
        return new Pair<>(rng, literal.toString());
      } else {
        rngAndBoolean =
            rng.genBoolean(); // need to burn one random number to make result deterministic
        return new Pair<>(rngAndBoolean.getFirst(), literal);
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

    /** {@inheritDoc} */
    @Override
    public Coregex simplify() {
      return this;
    }

    /**
     * @return literal
     */
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
      return new Pair<>(rng, sb.toString());
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
    public Coregex simplify() {
      Coregex quantified = this.quantified.simplify();
      return (0 == quantified.minLength() && 0 == quantified.maxLength()) || (1 == min && 1 == max)
          ? quantified
          : new Quantified(quantified, min, max, type);
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

    /**
     * @param set set of characters
     */
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
      return new Pair<>(rng, sample);
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
    public Coregex simplify() {
      return this;
    }

    /**
     * @return set of characters
     */
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

    @Override
    public Coregex simplify() {
      return 0 == rest.length
          ? first.simplify()
          : new Union(
              first.simplify(), Arrays.stream(rest).map(Coregex::simplify).toArray(Coregex[]::new));
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
