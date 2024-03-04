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

package com.github.simy4.coregex.scalacheck

import com.github.simy4.coregex.core.CoregexParser
import com.github.simy4.coregex.core.rng.RandomRNG
import org.scalacheck.{ Arbitrary, Gen, Shrink }

import java.util.regex.Pattern

trait CoregexInstances {
  type Matching[A <: String, Regex <: String with Singleton] <: A

  implicit def arbitraryInputStringMatchingRegexStringWithSingleton[
    A <: String,
    Regex <: String with Singleton
  ](implicit regex: ValueOf[Regex]): Arbitrary[Matching[A, Regex]] =
    Arbitrary(CoregexGen.fromPattern(Pattern.compile(regex.value)).asInstanceOf[Gen[Matching[A, Regex]]])

  implicit def shrinkInputStringMatchingRegexStringWithSingleton[
    A <: String,
    Regex <: String with Singleton
  ](implicit regex: ValueOf[Regex]): Shrink[Matching[A, Regex]] = {
    val coregex = CoregexParser.getInstance().parse(Pattern.compile(regex.value))
    Shrink.withLazyList { larger =>
      val rng = new RandomRNG()
      LazyList.unfold(coregex.minLength()) { remainder =>
        Option.when(remainder < larger.length) {
          (coregex.sized(remainder).generate(rng).asInstanceOf[Matching[A, Regex]], (remainder * 2) + 1)
        }
      }
    }
  }
}
