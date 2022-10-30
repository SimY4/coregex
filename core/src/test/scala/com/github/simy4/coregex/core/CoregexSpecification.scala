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

  property("quantified zero times should give empty") = forAll {
    (coregex: Coregex, `type`: Coregex.Quantified.Type, rng: RNG) =>
      coregex.quantify(0, 0, `type`).generate(rng).isEmpty
  }

  property("empty quantified should give empty") = forAll {
    (range: QuantifyRange, `type`: Coregex.Quantified.Type, rng: RNG) =>
      Coregex.empty().quantify(range.min, range.max, `type`).generate(rng).isEmpty
  }

  property("quantified length should be in range") = forAll {
    (coregex: Coregex, range: QuantifyRange, `type`: Coregex.Quantified.Type, rng: RNG) =>
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

  property("sized length should be withing limits") = forAll { (coregex: Coregex, length: Byte, rng: RNG) =>
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

  object ConcatSpecification extends Properties("Concat") {
    property("concat with empty should be identity") = forAll { (coregex: Coregex, rng: RNG) =>
      val concat1 = new Coregex.Concat(coregex, Coregex.empty()).simplify()
      val concat2 = new Coregex.Concat(Coregex.empty(), coregex).simplify()
      (coregex.simplify().generate(rng) ?= concat1.generate(rng)) && (coregex.simplify().generate(rng) ?= concat2
        .generate(rng))
    }

    property("concat two should be associative") = forAll { (coregex1: Coregex, coregex2: Coregex, rng: RNG) =>
      val concat = new Coregex.Concat(coregex1, coregex2).simplify()

      concat.generate(rng) ?= {
        val pairAndRes1 = coregex1.simplify().apply(rng, coregex1.maxLength())
        pairAndRes1.getSecond + coregex2.simplify().generate(pairAndRes1.getFirst)
      }
    }
  }

  object LiteralSpecification extends Properties("Literal") {
    property("generated should be literal") = forAll { (literal: String, rng: RNG) =>
      val literalCoregex = new Coregex.Literal(literal)
      val generated      = literalCoregex.generate(rng)

      val literalIsGenerated       = literal ?= generated
      val literalLengthIsMinLength = literal.length ?= literalCoregex.minLength()
      val literalLengthIsMaxLength = literal.length ?= literalCoregex.maxLength()
      literalIsGenerated && literalLengthIsMinLength && literalLengthIsMaxLength
    }

    property("concat literals should be literal of concat") = forAll { (str: String, rng: RNG) =>
      val (s1, s2) = str.splitAt(str.length / 2)
      val l1       = new Coregex.Literal(s1)
      val l2       = new Coregex.Literal(s2)
      val concat   = new Coregex.Concat(l1, l2).simplify()
      str ?= concat.generate(rng)
    }

    property("quantified generated should be repeated literal") = forAll {
      (literal: String, range: QuantifyRange, `type`: Coregex.Quantified.Type, rng: RNG) =>
        val literalCoregex = new Coregex.Literal(literal).quantify(range.min, range.max, `type`)
        val generated      = literalCoregex.generate(rng)

        s"(${Pattern.quote(literal)})*".r.matches(generated)
    }
  }

  object SetSpecification extends Properties("Set") {
    property("generated should be in set") = forAll { (set: Coregex.Set, rng: RNG) =>
      val generated = set.generate(rng)

      val inSetCheck  = generated.chars().allMatch(ch => set.set().test(ch)) :| s"$generated in $set"
      val lengthCheck = (generated.length ?= 1) :| s"$generated.length == 1"

      inSetCheck && lengthCheck
    }

    property("quantified generated should be in set") = forAll {
      (set: Coregex.Set, range: QuantifyRange, `type`: Coregex.Quantified.Type, rng: RNG) =>
        val generated = set.quantify(range.min, range.max, `type`).generate(rng)

        generated.chars().allMatch(ch => set.set().test(ch)) :| s"$generated in $set"
    }
  }

  object UnionSpecification extends Properties("Union") {
    property("generated should be in set") = forAll { (union: Coregex.Union, rng: RNG) =>
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
}
