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

package com.github.simy4.coregex.core

import org.scalacheck.Prop._
import org.scalacheck.Properties

import java.util.regex.Pattern

object CoregexSpecification extends Properties("Coregex") with CoregexArbitraries {
  include(ConcatSpecification)
  include(LiteralSpecification)
  include(SetSpecification)
  include(UnionSpecification)

  object ConcatSpecification extends Properties("Concat") {
    property("generated should be in concat") = forAll { (concat: Coregex.Concat, rng: RNG) =>
      val length    = (concat.maxLength() + concat.minLength()) / 2
      val generated = concat.sized(length).generate(rng)
      (0 <= concat.minLength() && concat.minLength() <= concat
        .maxLength()) :| s"0 <= concat.minLength(${concat.minLength()}) <= ${concat.maxLength()}" &&
      (concat.minLength() <= generated.length() && generated
        .length() <= length) :| s"concat.minLength(${concat.minLength()}) <= $generated.length(${generated.length}) <= $length"
    }

    property("quantify") = forAll { (concat: Coregex.Concat, i1: SmallNat, i2: SmallNat, greedy: Boolean, rng: RNG) =>
      val start      = i1.value min i2.value
      val end        = i1.value max i2.value
      val quantified = concat.quantify(start, end, greedy)
      val length     = (quantified.maxLength() + quantified.minLength()) / 2
      val generated  = quantified.sized(length).generate(rng)
      (0 <= quantified.minLength() && quantified.minLength() <= quantified
        .maxLength()) :| s"0 <= quantified.minLength(${quantified.minLength()}) <= ${quantified.maxLength()}" &&
      (quantified.minLength() <= generated.length() && generated
        .length() <= length) :| s"quantified.minLength(${quantified
          .minLength()}) <= $generated.length(${generated.length}) <= $length"
    }
  }

  object LiteralSpecification extends Properties("Literal") {
    property("generated should be literal") = forAll { (literal: Coregex.Literal, rng: RNG) =>
      literal.literal() ?= literal.generate(rng)
    }

    property("quantify") = forAll { (literal: Coregex.Literal, i1: SmallNat, i2: SmallNat, greedy: Boolean, rng: RNG) =>
      val start      = i1.value min i2.value
      val end        = i1.value max i2.value
      val quantified = literal.quantify(start, end, greedy)
      (0 <= quantified.minLength() && quantified.minLength() <= quantified
        .maxLength()) :| s"0 <= quantified.minLength(${quantified.minLength()}) <= ${quantified.maxLength()}" &&
      quantified.generate(rng).matches(s"(${Pattern.quote(literal.literal())})*")
    }
  }

  object SetSpecification extends Properties("Set") {
    property("generated should be in set") = forAll { (set: Coregex.Set, rng: RNG, length: Short) =>
      val gt0Length = 1 + length.toInt.abs
      val generated = set.sized(gt0Length).generate(rng)
      generated.chars().allMatch(ch => set.set().stream().anyMatch(_ == ch)) :| s"$generated all match ${set.set()}" &&
      (generated.length <= gt0Length) :| s"$generated.length(${generated.length}) <= $gt0Length"
    }

    property("quantify") = forAll { (set: Coregex.Set, i1: SmallNat, i2: SmallNat, greedy: Boolean, rng: RNG) =>
      val start      = i1.value min i2.value
      val end        = i1.value max i2.value
      val gt0Length  = 1 + ((start + end) / 2)
      val quantified = set.quantify(start, end, greedy)
      val generated  = quantified.sized(gt0Length).generate(rng)
      (0 <= quantified.minLength() && quantified.minLength() <= quantified
        .maxLength()) :| s"0 <= quantified.minLength(${quantified.minLength()}) <= ${quantified.maxLength()}" &&
      (quantified.minLength() <= generated.length() && generated
        .length() <= gt0Length) :| s"quantified.minLength(${quantified
          .minLength()}) <= $generated.length(${generated.length}) <= $gt0Length"
    }
  }

  object UnionSpecification extends Properties("Union") {
    property("generated should be in set") = forAll { (union: Coregex.Union, rng: RNG) =>
      val length    = (union.maxLength() + union.minLength()) / 2
      val generated = union.sized(length).generate(rng)
      val nextRng   = rng.genLong().getFirst
      (0 <= union.minLength() && union.minLength() <= union
        .maxLength()) :| s"0 <= union.minLength(${union.minLength()}) <= ${union.maxLength()}" &&
      (union.minLength() <= generated.length() && generated
        .length() <= length) :| s"union.minLength(${union.minLength()}) <= $generated.length(${generated.length}) <= $length" &&
      union
        .union()
        .stream()
        .filter(_.minLength() <= length)
        .map(_.sized(length).generate(nextRng))
        .anyMatch(_ == generated) :| s"$generated in $union"
    }

    property("quantify") = forAll { (union: Coregex.Union, i1: SmallNat, i2: SmallNat, greedy: Boolean, rng: RNG) =>
      val start      = i1.value min i2.value
      val end        = i1.value max i2.value
      val quantified = union.quantify(start, end, greedy)
      val length     = (quantified.maxLength() + quantified.minLength()) / 2
      val generated  = quantified.sized(length).generate(rng)
      (0 <= quantified.minLength() && quantified.minLength() <= quantified
        .maxLength()) :| s"0 <= quantified.minLength(${quantified.minLength()}) <= ${quantified.maxLength()}" &&
      (quantified.minLength() <= generated.length() && generated
        .length() <= length) :| s"quantified.minLength(${quantified
          .minLength()}) <= $generated.length(${generated.length}) <= $length"
    }
  }
}
