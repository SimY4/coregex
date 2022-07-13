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

package com.github.simy4.coregex
package scalacheck

import core.{ Coregex, CoregexParser }
import core.rng.RandomRNG
import org.scalacheck.{ Arbitrary, Gen }

import java.util.regex.Pattern
import scala.util.matching.Regex

object CoregexGen {
  implicit def arbitrary(implicit coregex: Coregex): Arbitrary[String] = Arbitrary(apply(coregex))

  def apply(regex: Regex): Gen[String] = apply(regex.pattern)

  def apply(regex: Pattern): Gen[String] = apply(CoregexParser.getInstance().parse(regex))

  def apply(coregex: Coregex): Gen[String] =
    for {
      seed <- Gen.long
      size <- Gen.size
    } yield coregex.sized(coregex.minLength() max size).generate(new RandomRNG(seed))
}
