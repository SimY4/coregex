/*
 * Copyright 2021-2026 Alex Simkin
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
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Prop._

class CoregexSuite extends ScalaCheckSuite with CoregexArbitraries {
  import Coregex._

  import scala.jdk.CollectionConverters._

  final private class Simple(val value: Coregex)

  implicit private def arbSimple(implicit coregex: Arbitrary[Coregex]): Arbitrary[Simple] = Arbitrary(
    Gen.resize(0, coregex.arbitrary.map(new Simple(_)))
  )

  property("quantified zero times should give empty") {
    forAll { (coregex: Coregex, `type`: Quantified.Type, seed: Long) =>
      coregex.quantify(0, 0, `type`).generate(seed) ?= ""
    }
  }

  property("empty quantified should give empty") {
    forAll { (range: QuantifyRange, seed: Long) =>
      Coregex.empty().quantify(range.min, range.max, range.`type`).generate(seed) ?= ""
    }
  }

  test("empty should match empty") {
    assert(Coregex.empty().matches("", null))
  }

  property("literal don't shrink") {
    forAll { (str: String) =>
      !literal(str, 0).shrink().iterator().hasNext :| s"literals don't shrink: $str"
    }
  }

  property("literal should match self") {
    forAll { (literal: String, flags: Int) =>
      Coregex.literal(literal, flags).matches(literal, null) :| s"$literal match self"
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
      val concat1 = new Concat(coregex, Coregex.empty())
      val concat2 = new Concat(Coregex.empty(), coregex)
      (coregex.generate(seed) ?= concat1.generate(seed)) && (coregex.generate(seed) ?= concat2.generate(seed))
    }
  }

  property("concat is associative") {
    forAll { (fst: Coregex, snd: Coregex, trd: Coregex, seed: Long) =>
      val left  = new Concat(new Concat(fst, snd), trd).generate(seed)
      val right = new Concat(fst, new Concat(snd, trd)).generate(seed)
      (left ?= right) :| s"$left == $right"
    }
  }

  property("concat should match self") {
    forAll { (head: Simple, tail: Seq[Simple], seed: Long) =>
      val concat    = new Concat(head.value, tail.map(_.value): _*)
      val generated = concat.generate(seed)
      concat.matches(generated, null) :| s"$generated in $concat"
    }
  }
  // endregion

  // region Ref
  property("generated ref by name should return group second time") {
    forAll { (group: Coregex, ref: Ref, seed: Long) =>
      val re        = new Concat(new Group(1, ref.ref().toString, group), ref).generate(seed)
      val generated = group.generate(seed)
      (generated + generated ?= re) :| s"$generated$generated == $re"
    }
  }

  property("generated ref by index should return group second time") {
    forAll { (group: Coregex, seed: Long) =>
      val re        = new Concat(new Group(1, group), new Ref(1)).generate(seed)
      val generated = group.generate(seed)
      (generated + generated ?= re) :| s"$generated$generated == $re"
    }
  }

  property("ref don't shrink") {
    forAll { (ref: Int Either String) =>
      !ref
        .fold(new Ref(_), new Ref(_))
        .shrink()
        .iterator()
        .hasNext :| s"refs don't shrink: ${ref.fold(_.toString, identity)}"
    }
  }
  // endregion

  // region Union
  property("generated should be in union") {
    forAll { (fst: String, snd: String, trd: String, seed: Long) =>
      val generated = new Union(literal(fst, 0), literal(snd, 0), literal(trd, 0)).generate(seed)
      ((generated ?= fst) || (generated ?= snd) || (generated ?= trd)) :| s"$generated in ($fst|$snd|$trd)"
    }
  }

  property("union should match self") {
    forAll { (head: Simple, tail: Seq[Simple], seed: Long) =>
      val union     = new Union(head.value, tail.map(_.value): _*)
      val generated = union.generate(seed)
      union.matches(generated, null) :| s"$generated in ($union)"
    }
  }

  property("union shrink should eliminate options") {
    forAll { (fst: Byte, snd: Byte) =>
      val union = new Union(literal(fst.toString, 0), literal(snd.toString, 0))

      union
        .shrink()
        .iterator()
        .asScala
        .map { shrunk =>
          (shrunk == new Union(literal(fst.toString, 0)) || shrunk == new Union(
            literal(snd.toString, 0)
          )) :| s"$union was not shrunk to segments: $shrunk"
        }
        .reduce(_ && _)
    }
  }
  // endregion
}
