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

package com.github.simy4.coregex.scalacheck

import com.github.simy4.coregex.core.Coregex
import org.scalacheck.{ Arbitrary, Gen, Shrink }

import java.util.regex.Pattern

trait CoregexInstances {
  import scala.jdk.CollectionConverters._

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
    val shrinks = LazyList.from(Coregex.from(Pattern.compile(regex.value)).shrink().iterator().asScala)
    Shrink.withLazyList { larger =>
      shrinks
        .map(coregex => coregex.generate(larger.length.toLong).asInstanceOf[Matching[A, Regex]])
        .filter(string => string.length < larger.length)
    }
  }
}
