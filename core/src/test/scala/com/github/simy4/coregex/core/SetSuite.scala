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

package com.github.simy4.coregex.core

import munit.ScalaCheckSuite
import org.scalacheck.Prop._

import java.util.regex.Pattern

class SetSuite extends ScalaCheckSuite with CoregexArbitraries {
  property("sampled should be in range") {
    forAll { (ch1: Char, ch2: Char, seed: Long) =>
      val start = ch1 min ch2
      val end   = {
        val end = ch1 max ch2
        if (start == end) (end + 1).asInstanceOf[Char] else end
      }
      val range     = Set.builder().range(start, end).build()
      val generated = range.sample(seed).orElse(-1)
      (start.toInt <= generated && generated <= end.toInt) :| s"$start <= $generated <= $end"
    }
  }

  property("sampled should be in set") {
    forAll { (first: Char, rest: String, seed: Long) =>
      val set       = Set.builder().set(first, rest.toCharArray: _*).build()
      val generated = set.sample(seed).orElse(-1)
      rest.map(_.toInt =? generated).foldLeft(first.toInt =? generated)(_ || _) :| s"$generated in [$set]"
    }
  }

  property("sampled should be in union") {
    forAll { (left: Set, right: Set, seed: Long) =>
      val union     = Set.builder().union(left).union(right).build()
      val generated = union.sample(seed).orElse(-1)
      (left.test(generated) || right.test(generated)) :| s"$generated in [$union]"
    }
  }

  property("sampled should not be in intersection") {
    forAll { (left: Set, right: Set, seed1: Long, seed2: Long) =>
      val leftWithCommon = Set.builder().union(left).single(right.sample(seed1).orElse(-1).toChar).build()
      val intersection   = Set.builder().union(leftWithCommon).intersect(right).build()
      val generated      = intersection.sample(seed2).orElse(-1)
      (leftWithCommon.test(generated) && right.test(generated)) :| s"$generated in [$intersection]"
    }
  }

  property("same seed same result") {
    forAll { (set: Set, seed: Long) =>
      val generated1 = set.sample(seed)
      val generated2 = set.sample(seed)
      generated1 ?= generated2
    }
  }

  property("double negation") {
    forAll { (set: Set, seed: Long) =>
      set.sample(seed) ?= Set.builder().union(set).negate().negate().build().sample(seed)
    }
  }

  property("case-insensitive set") {
    forAll { (set: Set, seed: Long) =>
      val result = Set
        .builder(Pattern.CASE_INSENSITIVE)
        .union(set)
        .build()
        .sample(seed)
        .orElse(-1)

      set.test(Character.toLowerCase(result)) || set.test(Character.toUpperCase(result))
    }
  }

  property("all generates all except line breaks") {
    forAll { (seed: Long) =>
      val result = Set
        .builder()
        .union(Set.ALL.get())
        .negate()
        .build()
        .sample(seed)
        .orElse(-1)

      (result ?= '\r') || (result ?= '\n')
    }
  }

  property("unix lines generates all except CR line break") {
    forAll { (seed: Long) =>
      val result = Set
        .builder()
        .union(Set.UNIX_LINES.get())
        .negate()
        .build()
        .sample(seed)
        .orElse(-1)

      result ?= '\n'
    }
  }
}
