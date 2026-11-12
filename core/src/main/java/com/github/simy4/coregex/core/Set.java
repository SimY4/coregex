/*
 * Copyright 2021-2026 Alex Simkin
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
public final class Set extends Coregex implements Serializable {

  private static final long serialVersionUID = 1L;

  static final Set EMPTY = new Set(new BitSet(0), "", false);

  /*
   * All chars except surrogates, which only exist in pairs: supplementary code points are out of
   * scope. Every set, including negated ones, is a subset of this one.
   */
  static final Set DOTALL;
  static final Set UNIX_LINES;
  static final Set ALL;

  static {
    BitSet dotAll = new BitSet(Character.MAX_VALUE + 1);
    dotAll.set(Character.MIN_VALUE, Character.MAX_VALUE + 1);
    dotAll.clear(Character.MIN_SURROGATE, Character.MAX_SURROGATE + 1);
    DOTALL = new Set(dotAll, ".", false);
    BitSet unixLines = (BitSet) dotAll.clone();
    unixLines.clear('\n');
    UNIX_LINES = new Set(unixLines, ".", false);
    BitSet all = (BitSet) unixLines.clone();
    all.clear('\r');
    all.clear('\u0085');
    all.clear('\u2028');
    all.clear('\u2029');
    ALL = new Set(all, ".", false);
  }

  public static Set any(int flags) {
    if (0 != (flags & Pattern.DOTALL)) {
      return DOTALL;
    } else if (0 != (flags & Pattern.UNIX_LINES)) {
      return UNIX_LINES;
    } else {
      return ALL;
    }
  }

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

  /**
   * Creates a specialized instance of {@link Set} for a single character.
   *
   * @param ch character
   * @return set instance
   * @see #single(char, int)
   */
  public static Set single(char ch) {
    return single(ch, 0);
  }

  /**
   * Creates a specialized instance of {@link Set} for a single character.
   *
   * @param ch character
   * @param flags regex flags
   * @return set instance
   * @see #single(char)
   */
  public static Set single(char ch, int flags) {
    return builder(flags).single(ch).build();
  }

  private final BitSet chars;
  private final String description;

  /*
   * Whether chars are only an approximation of the actual char class, i.e. ASCII-only \w for
   * UNICODE_CHARACTER_CLASS. Good enough for generation, as all generated chars still match, but not
   * for exact language operations.
   */
  private final boolean approximated;

  private Set(BitSet chars, String description, boolean approximated) {
    this.chars = chars;
    this.description = description;
    this.approximated = approximated;
  }

  @Override
  void generate(Context ctx) {
    int sample = sample(ctx.rng.nextLong());
    if (0 <= sample) {
      ctx.append((char) sample);
    }
  }

  boolean single() {
    return 1 == chars.cardinality();
  }

  /** {@inheritDoc} */
  @Override
  public Stream<Set> shrink() {
    Stream.Builder<Set> builder = Stream.builder();
    Set.Builder smaller = builder().range('0', '9').intersect(this);
    if (!smaller.chars.isEmpty() && !smaller.chars.equals(this.chars)) {
      builder.add(smaller.build());
    }
    smaller = builder(0).range('0', '9').range('a', 'z').intersect(this);
    if (!smaller.chars.isEmpty() && !smaller.chars.equals(this.chars)) {
      builder.add(smaller.build());
    }
    smaller = builder(0).range('0', '9').range('a', 'z').range('A', 'Z').intersect(this);
    if (!smaller.chars.isEmpty() && !smaller.chars.equals(this.chars)) {
      builder.add(smaller.build());
    }
    return builder.build();
  }

  @Override
  void compile(NFA nfa, int from, int to) {
    if (approximated) {
      throw new UnsupportedOperationException(
          "char class with UNICODE_CHARACTER_CLASS is not supported: " + this);
    }
    if (!chars.isEmpty()) {
      nfa.edge(from, chars, to);
    }
  }

  /*
   * @return sampled char or -1 if this set is empty.
   */
  int sample(long seed) {
    if (chars.isEmpty()) {
      return -1;
    }
    long skip = Math.abs(seed % chars.cardinality());
    int sample = chars.nextSetBit(0);
    while (skip-- > 0) {
      sample = chars.nextSetBit(sample + 1);
    }
    return sample;
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
    return chars.equals(set.chars);
  }

  @Override
  public int hashCode() {
    return chars.hashCode();
  }

  @Override
  public String toString() {
    return single() ? description : "[" + description + ']';
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
    private final StringBuilder description = new StringBuilder();
    private boolean approximated;

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
        for (int ch = start; ch <= end; ch++) {
          foldCase((char) ch);
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

    /**
     * Combines this set with another compiled set of characters.
     *
     * @param set set of characters
     * @return this builder instance
     */
    public Builder union(Set set) {
      chars.or(requireNonNull(set, "set").chars);
      approximated |= set.approximated;
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
      approximated |= set.approximated;
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
      foldCase(ch);
      description.append(ch);
      return this;
    }

    /*
     * Marks this set as only an approximation of the actual char class, see Set#approximated.
     */
    Builder approximated() {
      approximated = true;
      return this;
    }

    /*
     * Adds all chars matching given predicate, i.e. Character::isLowerCase.
     */
    Builder matching(IntPredicate predicate, String description) {
      for (int ch = DOTALL.chars.nextSetBit(0); ch >= 0; ch = DOTALL.chars.nextSetBit(ch + 1)) {
        if (predicate.test(ch)) {
          chars.set(ch);
          foldCase((char) ch);
        }
      }
      this.description.append(description);
      return this;
    }

    /*
     * Same as Pattern: CASE_INSENSITIVE alone only folds US-ASCII, UNICODE_CASE (implied by
     * UNICODE_CHARACTER_CLASS) folds the rest.
     */
    private void foldCase(char ch) {
      if (0 == (flags & Pattern.CASE_INSENSITIVE)
          || (ch > 0x7F
              && 0 == (flags & (Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS)))) {
        return;
      }
      chars.set(Character.toUpperCase(ch));
      chars.set(Character.toLowerCase(ch));
    }

    /**
     * Negates this set.
     *
     * @return this builder instance
     */
    public Builder negate() {
      // negation is not affected by DOTALL or UNIX_LINES: [^a] matches line terminators
      chars.flip(Character.MIN_VALUE, Character.MAX_VALUE + 1);
      chars.and(DOTALL.chars);
      description.insert(0, '^');
      return this;
    }

    /**
     * @return compiled set
     */
    public Set build() {
      return new Set(chars, description.toString(), approximated);
    }
  }
}
