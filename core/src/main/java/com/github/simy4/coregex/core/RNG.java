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

/**
 * Random generator contract.
 *
 * <p><em>Implementation should be pure. Calling an instance 1 or more times should always produce
 * the same result</em>
 *
 * @author Alex Simkin
 * @since 0.1.0
 */
public interface RNG {
  /** @return random boolean value. */
  Pair<RNG, Boolean> genBoolean();

  /**
   * Generates a random int value between 0 and provided upper bound (exclusive).
   *
   * @param bound upper bound.
   * @return random int value.
   */
  Pair<RNG, Integer> genInteger(int bound);

  /** @return random long value. */
  Pair<RNG, Long> genLong();
}
