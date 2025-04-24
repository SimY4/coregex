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
import java.util.BitSet;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Data representation of a set of characters AKA regular expression's char classes.
 *
 * @see Coregex
 * @see Set.Builder
 * @author Alex Simkin
 * @since 0.1.0
 */
public final class Set extends Coregex implements IntPredicate, Serializable {

  private static final long serialVersionUID = 1L;

  static final Lazy<Set> ALL =
      new Lazy<>(
          () -> {
            BitSet chars = new BitSet(Character.MIN_SURROGATE);
            chars.set(Character.MIN_VALUE, Character.MIN_SURROGATE);
            chars.clear('\r');
            chars.clear('\n');
            return new Set(chars, ".");
          });
  static final Lazy<Set> UNIX_LINES =
      new Lazy<>(
          () -> {
            BitSet chars = new BitSet(Character.MIN_SURROGATE);
            chars.set(Character.MIN_VALUE, Character.MIN_SURROGATE);
            chars.clear('\n');
            return new Set(chars, ".");
          });
  static final Lazy<Set> DOTALL =
      new Lazy<>(
          () -> {
            BitSet chars = new BitSet(Character.MIN_SURROGATE);
            chars.set(Character.MIN_VALUE, Character.MIN_SURROGATE);
            return new Set(chars, ".");
          });

  /**
   * Creates an instance of {@link Set} builder.
   *
   * <p><em>This builder is not thread-safe and generally should not be stored in a field or
   * collection, but instead used immediately to create instances.</em>
   *
   * @return staged set builder instance
   * @see #builder(int)
   */
  public static Builder builder() {
    return builder(0);
  }

  /**
   * Creates an instance of {@link Set} builder.
   *
   * <p><em>This builder is not thread-safe and generally should not be stored in a field or
   * collection, but instead used immediately to create instances.</em>
   *
   * @param flags regex flags
   * @return staged set builder instance
   * @see #builder()
   */
  public static Builder builder(int flags) {
    return new Builder(flags, 128);
  }

  private final BitSet chars;
  private final String description;

  private Set(BitSet chars, String description) {
    this.chars = chars;
    this.description = description;
  }

  /** {@inheritDoc} */
  @Override
  void apply(Context ctx) {
    ctx.ensureCapacity(minLength());
    OptionalInt sample = sample(ctx.rng.nextLong());
    if (sample.isPresent()) {
      ctx.append((char) sample.getAsInt());
    }
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

  /**
   * Checks if given character is included in this set.
   *
   * @param value character to check
   * @return {@code true} if given character is included in this set, {@code false} otherwise
   */
  @Override
  public boolean test(int value) {
    return chars.get(value);
  }

  /**
   * Randomly selects one character in this set based on provided seed.
   *
   * @param seed seed to use for random selection
   * @return selected character
   */
  public OptionalInt sample(long seed) {
    if (chars.isEmpty()) {
      return OptionalInt.empty();
    }
    long skip = Math.abs(seed % chars.cardinality());
    int sample = chars.nextSetBit(0);
    while (skip-- > 0) {
      sample = chars.nextSetBit(sample + 1);
    }
    return OptionalInt.of(sample);
  }

  /**
   * @return partitions this set into chunks.
   */
  public Stream<Set> shrink() {
    int partitionSize = chars.size() / 2;
    if (partitionSize < 64) {
      return Stream.empty();
    }
    return Stream.of(
            new Set(chars.get(0, partitionSize), description + "~"),
            new Set(chars.get(partitionSize, chars.size()), "~" + description))
        .filter(set -> !set.chars.isEmpty());
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
    return chars.equals(set.chars) && description.equals(set.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chars, description);
  }

  @Override
  public String toString() {
    return description;
  }

  /**
   * Set of characters (AKA regular expression's char classes) builder.
   *
   * @see Set
   * @author Alex Simkin
   * @since 0.1.0
   */
  public static final class Builder {

    private final int flags;
    private final BitSet chars;
    private final StringBuilder description = new StringBuilder("[");

    private Builder(int flags, int size) {
      this.flags = flags;
      this.chars = new BitSet(size);
    }

    /**
     * Adds a character range to this set.
     *
     * @param start first character in range
     * @param end last character in range
     * @return this builder instance
     */
    public Builder range(char start, char end) {
      if (start >= end) {
        throw new IllegalArgumentException("start: " + start + " should be < end: " + end);
      }
      chars.set(start, end + 1);
      if (0 != (flags & Pattern.CASE_INSENSITIVE)) {
        for (char ch = start; ch <= end; ch++) {
          if (Character.isLowerCase(ch)) {
            chars.set(Character.toUpperCase(ch));
          }
          if (Character.isUpperCase(ch)) {
            chars.set(Character.toLowerCase(ch));
          }
        }
      }
      description.append(start).append('-').append(end);
      return this;
    }

    /**
     * Adds a set of characters to this set.
     *
     * @param first first character in set
     * @param rest rest of characters in set
     * @return this builder instance
     */
    public Builder set(char first, char... rest) {
      single(first);
      for (char ch : rest) {
        single(ch);
      }
      return this;
    }

    @Deprecated
    public Builder set(Set set) {
      return union(set);
    }

    /**
     * Combines this set with another compiled set of characters.
     *
     * @param set set of characters
     * @return this builder instance
     */
    public Builder union(Set set) {
      chars.or(requireNonNull(set, "set").chars);
      description.append(set);
      return this;
    }

    /**
     * Intersects this set with another compiled set of characters.
     *
     * @param set set of characters
     * @return this builder instance
     */
    public Builder intersect(Set set) {
      chars.and(requireNonNull(set, "set").chars);
      description.append("&&").append(set);
      return this;
    }

    /**
     * Adds a single character to this set.
     *
     * @param ch character
     * @return this builder instance
     */
    public Builder single(char ch) {
      chars.set(ch);
      if (0 != (flags & Pattern.CASE_INSENSITIVE)) {
        if (Character.isLowerCase(ch)) {
          chars.set(Character.toUpperCase(ch));
        }
        if (Character.isUpperCase(ch)) {
          chars.set(Character.toLowerCase(ch));
        }
      }
      description.append(ch);
      return this;
    }

    /**
     * Negates this set.
     *
     * @return this builder instance
     */
    public Builder negate() {
      chars.flip(0, chars.size());
      description.insert(1, '^');
      return this;
    }

    /**
     * @return compiled set
     */
    public Set build() {
      return new Set(
          chars,
          chars.cardinality() == 1
              ? String.valueOf((char) chars.nextSetBit(0))
              : description.append(']').toString());
    }
  }
}
