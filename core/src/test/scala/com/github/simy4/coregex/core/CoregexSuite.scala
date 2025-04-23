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

class CoregexSuite extends ScalaCheckSuite with CoregexArbitraries {
  property("quantified zero times should give empty") {
    forAll { (coregex: Coregex, `type`: Coregex.Quantified.Type, seed: Long) =>
      coregex.quantify(0, 0, `type`).generate(seed).isEmpty
    }
  }

  property("empty quantified should give empty") {
    forAll { (range: QuantifyRange, seed: Long) =>
      Coregex.empty().quantify(range.min, range.max, range.`type`).generate(seed).isEmpty
    }
  }

  property("quantified length should be in range") {
    forAll { (coregex: Coregex, range: QuantifyRange, seed: Long) =>
      val quantified = coregex.quantify(range.min, range.max, range.`type`)
      val generated  = quantified.generate(seed)

      val quantifiedMinLengthCheck = (0 < range.min) ==>
        (coregex.minLength() <= quantified
          .minLength()) :| s"$coregex.minLength(${coregex.minLength()}) <= $quantified.minLength(${quantified.minLength()})"
      val quantifiedMaxLengthCheck = (0 < range.max) ==>
        (coregex.maxLength() <= quantified
          .maxLength()) :| s"$coregex.maxLength(${coregex.minLength()}) <= $quantified.maxLength(${quantified.minLength()})"
      val generatedLength = (quantified.minLength() <= generated
        .length()) :| s"$quantified.minLength(${quantified.minLength()}) <= $generated.length(${generated.length})"

      quantifiedMinLengthCheck && quantifiedMaxLengthCheck && generatedLength
    }
  }

  property("double quantified should multiply quantification") {
    forAll { (coregex: Coregex, first: QuantifyRange, second: QuantifyRange, seed: Long) =>
      val quantified       = coregex.quantify(first.min * second.min, first.min * second.min, first.`type`)
      val doubleQuantified =
        coregex.quantify(first.min, first.min, first.`type`).quantify(second.min, second.min, second.`type`)
      quantified.generate(seed) ?= doubleQuantified.generate(seed)
    }
  }

  property("quantified sized respect both") {
    forAll { (coregex: Coregex, range: QuantifyRange, length: Byte, seed: Long) =>
      val quantified        = coregex.quantify(range.min, range.max, range.`type`)
      val size              = quantified.minLength() + length.toInt.abs
      val expectedMaxLength = if (quantified.maxLength() < 0) size else quantified.maxLength() min size
      val generated         = quantified.sized(size).generate(seed)
      (generated.length() <= expectedMaxLength) :| s"${generated.length()} <= $expectedMaxLength"
    }
  }

  property("sized length should be withing limits") {
    forAll { (coregex: Coregex, length: Byte, seed: Long) =>
      val size      = coregex.minLength() + length.toInt.abs
      val sized     = coregex.sized(size)
      val generated = sized.generate(seed)

      val sizedMinLengthCheck = (coregex.minLength() ?= sized
        .minLength()) :| s"$coregex.minLength(${coregex.minLength()}) == $sized.minLength(${sized.minLength()})"
      val sizedMaxLengthCheck = (-1 < coregex.maxLength()) ==> (coregex.maxLength() min size ?= sized.maxLength())
      val sizedLength         =
        (sized.minLength() <= generated.length() && generated.length <= size) :| s"$sized.minLength(${sized
            .minLength()}) <= $generated.length(${generated.length}) <= $size"

      sizedMinLengthCheck && sizedMaxLengthCheck && sizedLength
    }
  }

  // region Concat
  property("concat with empty should be identity") {
    forAll { (coregex: Coregex, seed: Long) =>
      val concat1 = new Coregex.Concat(coregex, Coregex.empty())
      val concat2 = new Coregex.Concat(Coregex.empty(), coregex)
      (coregex.generate(seed) ?= concat1.generate(seed)) && (coregex.generate(seed) ?= concat2.generate(seed))
    }
  }
  // endregion

  // region Set
  property("generated should be in set") {
    forAll { (set: Set, seed: Long) =>
      val generated = set.generate(seed)

      val inSetCheck  = generated.chars().allMatch(set) :| s"$generated in $set"
      val lengthCheck = (generated.length ?= 1) :| s"$generated.length == 1"

      inSetCheck && lengthCheck
    }
  }

  property("quantified generated should be in set") {
    forAll { (set: Set, range: QuantifyRange, `type`: Coregex.Quantified.Type, seed: Long) =>
      val generated = set.quantify(range.min, range.max, `type`).generate(seed)

      generated.chars().allMatch(set) :| s"$generated in $set"
    }
  }
  // endregion

  // region Union
  // endregion
}
