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

object CoregexSpecification extends Properties("Coregex") with CoregexArbitraries {
  include(ConcatSpecification)
  include(LiteralSpecification)
  include(SetSpecification)
  include(UnionSpecification)

  propertyWithSeed("quantified length should be in range", Some("FzHsymzNXfRpo-sA6018dIa-5TKectB1vVTkPgXFUKG=")) =
    forAll { (coregex: Coregex, range: QuantifyRange, `type`: Coregex.Quantified.Type, length: Byte, rng: RNG) =>
      (-1 == coregex.maxLength() || 0 < coregex.maxLength()) :| "non-empty" ==> {
        val quantified  = coregex.quantify(range.min, range.max, `type`)
        val geMinLength = quantified.minLength() + length.toInt.abs
        val generated   = quantified.sized(geMinLength).generate(rng)

        val quantifiedMinLengthCheck = (coregex.minLength() <= quantified
          .minLength()) :| s"$coregex.minLength(${coregex.minLength()}) <= quantified.minLength(${quantified.minLength()})"
        val quantifiedMaxLengthCheck = (-1 == range.max || coregex.maxLength() <= quantified
          .maxLength()) :| s"$coregex.maxLength(${coregex.minLength()}) <= quantified.maxLength(${quantified.minLength()})"
        val generatedLength = (quantified.minLength() <= generated.length() && generated
          .length() <= geMinLength) :| s"quantified.minLength(${quantified
            .minLength()}) <= $generated.length(${generated.length}) <= $geMinLength"

        quantifiedMinLengthCheck && quantifiedMaxLengthCheck && generatedLength
      }
    }

  object ConcatSpecification extends Properties("Concat") {
    property("concat with empty should be identity") = forAll { (coregex: Coregex, rng: RNG) =>
      val concat1 = new Coregex.Concat(coregex, Coregex.empty()).simplify()
      val concat2 = new Coregex.Concat(Coregex.empty(), coregex).simplify()
      (coregex.generate(rng) ?= concat1.generate(rng)) && (coregex.generate(rng) ?= concat2.generate(rng))
    }

    property("concat two coregexes should be ") = forAll { (concat: Coregex.Concat, rng: RNG) =>
      val length    = (concat.maxLength() + concat.minLength()) / 2
      val generated = concat.sized(length).generate(rng)
      (0 <= concat.minLength() && concat.minLength() <= concat
        .maxLength()) :| s"0 <= concat.minLength(${concat.minLength()}) <= ${concat.maxLength()}" &&
      (concat.minLength() <= generated.length() && generated
        .length() <= length) :| s"concat.minLength(${concat.minLength()}) <= $generated.length(${generated.length}) <= $length"
    }
  }

  object LiteralSpecification extends Properties("Literal") {
    property("generated should be literal") = forAll { (literal: String, rng: RNG, length: Byte) =>
      val geMinLength    = literal.length + length.toInt.abs
      val literalCoregex = new Coregex.Literal(literal)
      val generated      = literalCoregex.sized(geMinLength).generate(rng)

      (literal ?= generated) && (literal.length ?= literalCoregex.minLength()) && (literal.length ?= literalCoregex
        .maxLength())
    }

    property("concat literals should be literal of concat") = forAll { (str: String, rng: RNG) =>
      val (s1, s2) = str.splitAt(str.length / 2)
      val l1       = new Coregex.Literal(s1)
      val l2       = new Coregex.Literal(s2)
      val concat   = new Coregex.Concat(l1, l2).simplify()
      str ?= concat.generate(rng)
    }
  }

  object SetSpecification extends Properties("Set") {
    property("generated should be in set") = forAll { (set: Coregex.Set, rng: RNG, length: Byte) =>
      val gt0Length = 1 + length.toInt.abs
      val generated = set.sized(gt0Length).generate(rng)

      val inSetCheck  = generated.chars().allMatch(ch => set.set().stream().anyMatch(_ == ch)) :| s"$generated in $set"
      val lengthCheck = (generated.length ?= 1) :| s"$generated.length == 1"

      inSetCheck && lengthCheck
    }
  }

  object UnionSpecification extends Properties("Union") {
    property("generated should be in set") = forAll { (union: Coregex.Union, rng: RNG, length: Byte) =>
      val geMinLength = union.minLength() + length.toInt.abs
      val generated   = union.sized(geMinLength).generate(rng)

      val nextRng = rng.genLong().getFirst
      val inSetCheck = union
        .union()
        .stream()
        .filter(_.minLength() <= geMinLength)
        .map(_.sized(geMinLength).generate(nextRng))
        .anyMatch(_ == generated) :| s"$generated in $union"
      val lengthCheck = (union.minLength() <= generated.length() && generated
        .length() <= geMinLength) :| s"union.minLength(${union.minLength()}) <= $generated.length(${generated.length}) <= $geMinLength"

      inSetCheck && lengthCheck
    }
  }
}
