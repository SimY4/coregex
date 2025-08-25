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

import java.util.Locale
import java.util.regex.Pattern

class SetSuite extends ScalaCheckSuite with CoregexArbitraries {
  import scala.jdk.CollectionConverters._

  property("generated should be in set") {
    forAll { (set: Set, seed: Long) =>
      val generated = set.generate(seed)

      val inSetCheck  = set.matches(generated, null) :| s"$generated in $set"
      val lengthCheck = (generated.length ?= 1) :| s"$generated.length == 1"

      inSetCheck && lengthCheck
    }
  }

  property("quantified generated should be in set") {
    forAll { (set: Set, range: QuantifyRange, `type`: Coregex.Quantified.Type, seed: Long) =>
      val quantified = set.quantify(range.min, range.max, `type`)
      val generated  = quantified.generate(seed)

      quantified.matches(generated, null) :| s"$generated in $set"
    }
  }

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
      rest.map(_.toInt =? generated).foldLeft(first.toInt =? generated)(_ || _) :| s"$generated in $set"
    }
  }

  property("sampled should be in union") {
    forAll { (left: Set, right: Set, seed: Long) =>
      val union     = Set.builder().union(left).union(right).build()
      val generated = union.generate(seed)
      (left.matches(generated, null) || right.matches(generated, null)) :| s"$generated in $union"
    }
  }

  property("sampled should not be in intersection") {
    forAll { (left: Set, right: Set, seed1: Long, seed2: Long) =>
      val leftWithCommon = Set.builder().union(left).single(right.sample(seed1).orElse(-1).toChar).build()
      val intersection   = Set.builder().union(leftWithCommon).intersect(right).build()
      val generated      = intersection.generate(seed2)
      (leftWithCommon.matches(generated, null) && right.matches(generated, null)) :| s"$generated in $intersection"
    }
  }

  test("empty set should not shrink") {
    !Set.builder().build().shrink().iterator().hasNext :| "empty set should not shrink"
  }

  property("shrunk should be in set") {
    forAll { (set: Set) =>
      set
        .shrink()
        .iterator()
        .asScala
        .map(shrink => Set.builder().union(set).union(shrink).build().equals(set) :| s"shrunk $shrink in $set")
        .foldLeft(passed)(_ && _)
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
    forAll { (set: Set) =>
      set ?= Set.builder().union(set).negate().negate().build()
    }
  }

  property("case-insensitive set") {
    forAll { (set: Set, seed: Long) =>
      val result = Set
        .builder(Pattern.CASE_INSENSITIVE)
        .union(set)
        .build()
        .generate(seed)

      set.matches(result.toLowerCase(Locale.ENGLISH), null) || set.matches(result.toUpperCase(Locale.ENGLISH), null)
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
