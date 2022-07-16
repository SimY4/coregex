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

package com.github.simy4.coregex.scalacheck

import com.github.simy4.coregex.core.CoregexParser
import com.github.simy4.coregex.core.rng.RandomRNG
import org.scalacheck.{ Arbitrary, Gen, Shrink }

import java.util.regex.Pattern
import scala.annotation.nowarn

trait CoregexInstances {
  type Matching[A >: Null <: String, Regex >: Null <: String with Singleton] <: A

  implicit def arbitraryInputStringMatchingRegexStringWithSingleton[
    A >: Null <: String,
    Regex >: Null <: String with Singleton
  ](implicit regex: ValueOf[Regex]): Arbitrary[Matching[A, Regex]] =
    Arbitrary(CoregexGen(Pattern.compile(regex.value)).asInstanceOf[Gen[Matching[A, Regex]]])

  @nowarn("cat=deprecation")
  implicit def shrinkInputStringMatchingRegexStringWithSingleton[
    A >: Null <: String,
    Regex >: Null <: String with Singleton
  ](implicit regex: ValueOf[Regex]): Shrink[Matching[A, Regex]] =
    Shrink { larger =>
      val coregex = CoregexParser.getInstance().parse(Pattern.compile(regex.value))
      val rng     = new RandomRNG()
      Stream
        .iterate(coregex.minLength())(remainder => (remainder * 2) + 1)
        .takeWhile(remainder => remainder < larger.length)
        .map(remainder => coregex.sized(remainder).generate(rng).asInstanceOf[Matching[A, Regex]])
    }
}
