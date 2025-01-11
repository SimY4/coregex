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

import org.scalacheck.{ Arbitrary, Gen, Shrink }

import scala.annotation.nowarn

final case class QuantifyRange(min: Int, max: Int, `type`: Coregex.Quantified.Type)

object QuantifyRange {
  implicit val arbitraryQuantifyRange: Arbitrary[QuantifyRange] = Arbitrary {
    for {
      min <- Gen.choose(0, 20)
      add <- Gen.choose(0, 20)
      max <- Gen.oneOf(
        Gen.const(-1),
        Gen.choose(min, min + add)
      )
      tpe <- Gen.oneOf(Coregex.Quantified.Type.values.toIndexedSeq)
    } yield QuantifyRange(min, max, tpe)
  }

  @nowarn("cat=deprecation")
  implicit def shrinkQuantifyRange(implicit shrinkInt: Shrink[Int]): Shrink[QuantifyRange] =
    Shrink { case QuantifyRange(min, max, tpe) =>
      shrinkInt.shrink(min).map(QuantifyRange(_, max, tpe)) #:::
        shrinkInt.shrink(max).filter(min <= _).map(QuantifyRange(min, _, tpe))
    }
}
