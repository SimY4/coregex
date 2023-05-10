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

class CoregexSuite extends ScalaCheckSuite with CoregexArbitraries {
  property("coregex should generate a string that it accepts") {
    forAll { (coregex: Coregex, rng: RNG) =>
      val generated = coregex.generate(rng)
      coregex.test(generated) :| s"$coregex should accept $generated"
    }
  }

  property("negated coregex should not accept generated string") {
    forAll { (coregex: Coregex, rng: RNG) =>
      (0 != coregex.minLength() || 0 != coregex.maxLength()) ==> {
        val generated = coregex.generate(rng)
        !coregex.negate().test(generated) :| s"negated $coregex should not accept $generated"
      }
    }
  }

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
  property("generated should be concat") {
    forAll { (concat: Coregex.Concat, rng: RNG) =>
      val generated = concat.generate(rng)

      val inConcatCheck = concat.test(generated) :| s"$generated in $concat"
      val minLengthCheck = (concat.minLength() <= generated
        .length()) :| s"concat.minLength(${concat.minLength()}) <= $generated.length(${generated.length})"
      val maxLengthCheck = -1 != concat.maxLength() ==> (generated.length() <= concat
        .maxLength()) :| s"$generated.length(${generated.length}) <= concat.maxLength(${concat.maxLength()})"

      inConcatCheck && minLengthCheck && maxLengthCheck
    }
  }
  // endregion

  // region Intersect
  property("generated should be in intersection".ignore) {
    forAll { (intersection: Coregex.Intersection, rng: RNG) =>
      val generated = intersection.generate(rng)

      val inIntersectionCheck =
        intersection.intersection().stream().allMatch(_.test(generated)) :| s"$generated in $intersection"
      val minLengthCheck = (intersection.minLength() <= generated
        .length()) :| s"intersection.minLength(${intersection.minLength()}) <= $generated.length(${generated.length})"
      val maxLengthCheck = -1 != intersection.maxLength() ==> (generated.length() <= intersection
        .maxLength()) :| s"$generated.length(${generated.length}) <= intersection.maxLength(${intersection.maxLength()})"

      inIntersectionCheck && minLengthCheck && maxLengthCheck
    }
  }
  // endregion

  // region Literal
  property("generated should be literal") {
    forAll { (literal: Coregex.Literal, rng: RNG) =>
      val generated = literal.generate(rng)

      val literalIsGenerated = literal.test(generated) :| s"$generated is $literal"
      val literalLengthIsMinLength = (literal.literal().length ?= literal
        .minLength()) :| s"literal.length(${literal.literal().length}) == literal.minLength(${literal.minLength()})"
      val literalLengthIsMaxLength = (literal.literal().length ?= literal
        .maxLength()) :| s"literal.length(${literal.literal().length}) == literal.maxLength(${literal.maxLength()})"
      literalIsGenerated && literalLengthIsMinLength && literalLengthIsMaxLength
    }
  }
  // endregion

  // region Set
  property("generated should be in set") {
    forAll { (set: Coregex.Set, rng: RNG) =>
      val generated = set.generate(rng)

      val inSetCheck     = set.test(generated) :| s"$generated in $set"
      val lengthCheck    = (generated.length =? 1) :| s"$generated.length == 1"
      val minLengthCheck = (set.minLength() =? 1) :| s"set.minLength(${set.minLength()}) == 1"
      val maxLengthCheck = (set.maxLength() =? 1) :| s"set.maxLength(${set.maxLength()}) == 1"

      inSetCheck && lengthCheck && minLengthCheck && maxLengthCheck
    }
  }
  // endregion

  // region Union
  property("generated should be in union") {
    forAll { (union: Coregex.Union, rng: RNG) =>
      val generated = union.generate(rng)

      val inUnionCheck = union.union().stream().anyMatch(_.test(generated)) :| s"$generated in $union"
      val minLengthCheck = (union.minLength() <= generated
        .length()) :| s"union.minLength(${union.minLength()}) <= $generated.length(${generated.length})"
      val maxLengthCheck = -1 != union.maxLength() ==> (generated.length() <= union
        .maxLength()) :| s"$generated.length(${generated.length}) <= union.maxLength(${union.maxLength()})"

      inUnionCheck && minLengthCheck && maxLengthCheck
    }
  }
  // endregion
}
