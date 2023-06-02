/*
 * Copyright 2021-2023 Alex Simkin
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

class CoregexSuite extends ScalaCheckSuite with CoregexArbitraries {
  property("quantified zero times should give empty") {
    forAll { (coregex: Coregex, `type`: Coregex.Quantified.Type, rng: RNG) =>
      coregex.quantify(0, 0, `type`).generate(rng).isEmpty
    }
  }

  property("empty quantified should give empty") {
    forAll { (range: QuantifyRange, `type`: Coregex.Quantified.Type, rng: RNG) =>
      Coregex.empty().quantify(range.min, range.max, `type`).generate(rng).isEmpty
    }
  }

  property("quantified length should be in range") {
    forAll { (coregex: Coregex, range: QuantifyRange, `type`: Coregex.Quantified.Type, rng: RNG) =>
      val quantified = coregex.quantify(range.min, range.max, `type`)
      val generated  = quantified.generate(rng)

      val quantifiedMinLengthCheck = (0 < quantified.minLength()) ==>
        (coregex.minLength() <= quantified
          .minLength()) :| s"$coregex.minLength(${coregex.minLength()}) <= $quantified.minLength(${quantified.minLength()})"
      val quantifiedMaxLengthCheck = (0 < quantified.maxLength()) ==>
        (coregex.maxLength() <= quantified
          .maxLength()) :| s"$coregex.maxLength(${coregex.minLength()}) <= $quantified.maxLength(${quantified.minLength()})"
      val generatedLength = (quantified.minLength() <= generated
        .length()) :| s"$quantified.minLength(${quantified.minLength()}) <= $generated.length(${generated.length})"

      quantifiedMinLengthCheck && quantifiedMaxLengthCheck && generatedLength
    }
  }

  property("double quantified should multiply quantification") {
    forAll {
      (coregex: Coregex, first: QuantifyRange, second: QuantifyRange, `type`: Coregex.Quantified.Type, rng: RNG) =>
        val quantified       = coregex.quantify(first.min * second.min, first.min * second.min, `type`)
        val doubleQuantified = coregex.quantify(first.min, first.min, `type`).quantify(second.min, second.min, `type`)
        quantified.generate(rng) ?= doubleQuantified.generate(rng)
    }
  }

  property("sized length should be withing limits") {
    forAll { (coregex: Coregex, length: Byte, rng: RNG) =>
      val size      = coregex.minLength() + length.toInt.abs
      val sized     = coregex.sized(size)
      val generated = sized.generate(rng)

      val sizedMinLengthCheck = (coregex.minLength() ?= sized
        .minLength()) :| s"$coregex.minLength(${coregex.minLength()}) == $sized.minLength(${sized.minLength()})"
      val sizedMaxLengthCheck = (-1 < coregex.maxLength()) ==> (coregex.maxLength() min size ?= sized.maxLength())
      val sizedLength =
        (sized.minLength() <= generated.length() && generated.length <= size) :| s"$sized.minLength(${sized
            .minLength()}) <= $generated.length(${generated.length}) <= $size"

      sizedMinLengthCheck && sizedMaxLengthCheck && sizedLength
    }
  }

  // region Concat
  property("concat with empty should be identity") {
    forAll { (coregex: Coregex, rng: RNG) =>
      val concat1 = new Coregex.Concat(coregex, Coregex.empty()).simplify()
      val concat2 = new Coregex.Concat(Coregex.empty(), coregex).simplify()
      (coregex.simplify().generate(rng) ?= concat1.generate(rng)) && (coregex.simplify().generate(rng) ?= concat2
        .generate(rng))
    }
  }
  // endregion

  // region Literal
  property("generated should be literal") {
    forAll { (literal: String, flags: Flags, rng: RNG) =>
      val literalCoregex = new Coregex.Literal(literal, flags)
      val generated      = literalCoregex.generate(rng)

      val literalIsGenerated =
        (0 != (Pattern.CASE_INSENSITIVE & flags)) ==> (literal equalsIgnoreCase generated) || (literal ?= generated)
      val literalLengthIsMinLength = literal.length ?= literalCoregex.minLength()
      val literalLengthIsMaxLength = literal.length ?= literalCoregex.maxLength()
      literalIsGenerated && literalLengthIsMinLength && literalLengthIsMaxLength
    }
  }

  property("concat literals should be literal of concat") {
    forAll { (s1: String, s2: String, rng: RNG) =>
      (s1.length + s2.length < Int.MaxValue - 2) ==> {
        val l1     = new Coregex.Literal(s1)
        val l2     = new Coregex.Literal(s2)
        val concat = new Coregex.Concat(l1, l2).simplify()
        (s1 + s2) ?= concat.generate(rng)
      }
    }
  }

  property("quantified generated should be repeated literal") {
    forAll { (literal: String, range: QuantifyRange, `type`: Coregex.Quantified.Type, rng: RNG) =>
      val literalCoregex = new Coregex.Literal(literal).quantify(range.min, range.max, `type`)
      val generated      = literalCoregex.generate(rng)

      s"(${Pattern.quote(literal)})*".r.matches(generated)
    }
  }
  // endregion

  // region Set
  property("generated should be in set") {
    forAll { (set: Coregex.Set, rng: RNG) =>
      val generated = set.generate(rng)

      val inSetCheck  = generated.chars().allMatch(ch => set.set().test(ch)) :| s"$generated in $set"
      val lengthCheck = (generated.length ?= 1) :| s"$generated.length == 1"

      inSetCheck && lengthCheck
    }
  }

  property("quantified generated should be in set") {
    forAll { (set: Coregex.Set, range: QuantifyRange, `type`: Coregex.Quantified.Type, rng: RNG) =>
      val generated = set.quantify(range.min, range.max, `type`).generate(rng)

      generated.chars().allMatch(ch => set.set().test(ch)) :| s"$generated in $set"
    }
  }
  // endregion

  // region Union
  property("generated should be in set") {
    forAll { (union: Coregex.Union, rng: RNG) =>
      val generated = union.generate(rng)

      val nextRng = rng.genLong().getFirst
      val inSetCheck = union
        .union()
        .stream()
        .map(_.generate(nextRng))
        .anyMatch(_ == generated) :| s"$generated in $union"
      val lengthCheck = (union.minLength() <= generated
        .length()) :| s"union.minLength(${union.minLength()}) <= $generated.length(${generated.length})"

      inSetCheck && lengthCheck
    }
  }
  // endregion
}
