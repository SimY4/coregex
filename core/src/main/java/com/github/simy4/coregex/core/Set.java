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
import java.util.stream.IntStream;

public final class Set implements Serializable {
  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  private final BitSet chars;

  private Set(BitSet chars) {
    this.chars = chars;
  }

  public char generate(long seed) {
    return (char)
        stream()
            .skip(Math.abs(seed % weight()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("empty set"));
  }

  public int weight() {
    return chars.cardinality();
  }

  public IntStream stream() {
    return chars.stream();
  }

  @Override
  public String toString() {
    return (weight() > 50 ? IntStream.concat(stream().limit(50L), IntStream.of('…')) : stream())
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }

  public static final class Builder {
    private final BitSet chars = new BitSet(256);

    private Builder() {}

    public Builder range(char start, char end) {
      if (start >= end) {
        throw new IllegalArgumentException("start: " + start + " should be < end: " + end);
      }
      chars.set(start, end + 1);
      return this;
    }

    public Builder set(char first, char... rest) {
      chars.set(first);
      for (char ch : rest) {
        chars.set(ch);
      }
      return this;
    }

    public Builder set(Set set) {
      chars.or(set.chars);
      return this;
    }

    public Builder single(char ch) {
      chars.set(ch);
      return this;
    }

    public Builder negate() {
      chars.flip(0, chars.size());
      return this;
    }

    public Set build() {
      return new Set(chars);
    }
  }
}
