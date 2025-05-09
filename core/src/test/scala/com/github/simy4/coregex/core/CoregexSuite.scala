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

  property("double quantified should multiply quantification") {
    forAll { (coregex: Coregex, first: QuantifyRange, second: QuantifyRange, seed: Long) =>
      val quantified       = coregex.quantify(first.min * second.min, first.min * second.min, first.`type`)
      val doubleQuantified =
        coregex.quantify(first.min, first.min, first.`type`).quantify(second.min, second.min, second.`type`)
      quantified.generate(seed) ?= doubleQuantified.generate(seed)
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

  property("concat is associative") {
    forAll { (fst: Coregex, snd: Coregex, trd: Coregex, seed: Long) =>
      val left  = new Coregex.Concat(new Coregex.Concat(fst, snd), trd).generate(seed)
      val right = new Coregex.Concat(fst, new Coregex.Concat(snd, trd)).generate(seed)
      (left ?= right) :| s"$left == $right"
    }
  }
  // endregion

  // region Ref
  property("generated ref by name should return group second time") {
    forAll { (group: Coregex, ref: Coregex.Ref, seed: Long) =>
      val re        = new Coregex.Concat(new Coregex.Group(ref.ref().toString, group), ref).generate(seed)
      val generated = group.generate(seed)
      (generated + generated ?= re) :| s"$generated$generated == $re"
    }
  }

  property("generated ref by index should return group second time") {
    forAll { (group: Coregex, seed: Long) =>
      val re        = new Coregex.Concat(new Coregex.Group(group), new Coregex.Ref(1)).generate(seed)
      val generated = group.generate(seed)
      (generated + generated ?= re) :| s"$generated$generated == $re"
    }
  }
  // endregion

  // region Union
  property("generated should be in union") {
    forAll { (fst: String, snd: String, trd: String, seed: Long) =>
      val generated =
        new Coregex.Union(Coregex.literal(fst, 0), Coregex.literal(snd, 0), Coregex.literal(trd, 0)).generate(seed)
      ((generated ?= fst) || (generated ?= snd) || (generated ?= trd)) :| s"$generated in ($fst|$snd|$trd)"
    }
  }
  // endregion
}
