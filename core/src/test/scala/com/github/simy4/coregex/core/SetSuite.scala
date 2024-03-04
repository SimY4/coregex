/*
 * Copyright 2021-2024 Alex Simkin
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
      val end = {
        val end = ch1 max ch2
        if (start == end) (end + 1).asInstanceOf[Char] else end
      }
      val range     = Set.builder().range(start, end).build()
      val generated = range.sample(seed)
      (start <= generated && generated <= end) :| s"$start <= $generated <= $end"
    }
  }

  property("sampled should be in set") {
    forAll { (first: Char, rest: String, seed: Long) =>
      val set       = Set.builder().set(first, rest.toCharArray: _*).build()
      val generated = set.sample(seed)
      rest.map(_ =? generated).foldLeft(first =? generated)(_ || _) :| s"$generated in [$set]"
    }
  }

  property("sampled should be in union") {
    forAll { (first: Set, rest: List[Set], seed: Long) =>
      val union     = rest.foldLeft(Set.builder().set(first))(_ set _).build()
      val generated = union.sample(seed)
      (first :: rest).exists(_.test(generated.toInt)) :| s"$generated in [$union]"
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
      set.sample(seed) ?= Set.builder().set(set).negate().negate().build().sample(seed)
    }
  }

  property("case-insensitive set") {
    forAll { (set: Set, seed: Long) =>
      val result = Set
        .builder(Pattern.CASE_INSENSITIVE)
        .set(set)
        .build()
        .sample(seed)

      set.test(Character.toLowerCase(result.toInt)) || set.test(Character.toUpperCase(result.toInt))
    }
  }

  property("all generates all except line breaks") {
    forAll { (seed: Long) =>
      val result = Set
        .builder()
        .set(Set.ALL.get())
        .negate()
        .build()
        .sample(seed)

      (result ?= '\r') || (result ?= '\n')
    }
  }

  property("unix lines generates all except CR line break") {
    forAll { (seed: Long) =>
      val result = Set
        .builder()
        .set(Set.UNIX_LINES.get())
        .negate()
        .build()
        .sample(seed)

      result ?= '\n'
    }
  }
}
