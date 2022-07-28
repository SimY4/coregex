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
import java.util.BitSet;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Data representation of a set of characters AKA regular expression's char classes.
 *
 * @see Set.Builder
 * @author Alex Simkin
 * @since 0.1.0
 */
public final class Set implements Serializable {
  private static final long serialVersionUID = 1L;

  static final Set ALL =
      builder().range(Character.MIN_VALUE, (char) (Character.MIN_SURROGATE - 1)).build();

  /**
   * Creates an instance of {@link Set} builder.
   *
   * <p><em>This builder is not thread-safe and generally should not be stored in a field or
   * collection, but instead used immediately to create instances.</em>
   *
   * @return staged set builder instance
   */
  public static Builder builder() {
    return new Builder(256);
  }

  private final BitSet chars;
  private final String description;

  private Set(BitSet chars, String description) {
    this.chars = chars;
    this.description = description;
  }

  /**
   * Randomly selects one character in this set based on provided seed.
   *
   * @param seed seed to use for random selection
   * @return selected character
   */
  public char generate(long seed) {
    return (char)
        stream()
            .skip(Math.abs(seed % chars.cardinality()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("empty set: " + description));
  }

  /** @return all characters in this set as a stream. */
  public IntStream stream() {
    return chars.stream();
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
    private final BitSet chars;
    private final StringBuilder description = new StringBuilder();

    private Builder(int size) {
      chars = new BitSet(size);
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
      chars.set(first);
      description.append(first);
      for (char ch : rest) {
        chars.set(ch);
        description.append(ch);
      }
      return this;
    }

    /**
     * Adds a compiled set of characters to this set.
     *
     * @param set set of characters
     * @return this builder instance
     */
    public Builder set(Set set) {
      chars.or(requireNonNull(set, "set").chars);
      description.append(set);
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
      description.insert(0, '^');
      return this;
    }

    /** @return compiled set */
    public Set build() {
      return new Set(chars, description.toString());
    }
  }
}
