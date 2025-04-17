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

package com.github.simy4.coregex
package scalacheck

import core.Coregex
import org.scalacheck.Gen

import java.util.regex.Pattern
import scala.util.matching.Regex

object CoregexGen {
  def fromRegex(regex: Regex, size: Option[Int] = None): Gen[String] = fromPattern(regex.pattern, size)

  def fromPattern(regex: Pattern, size: Option[Int] = None): Gen[String] = apply(Coregex.from(regex), size)

  def apply(coregex: Coregex, size: Option[Int] = None): Gen[String] =
    for {
      seed <- Gen.long
      size <- size.fold(Gen.size)(Gen.const)
    } yield coregex.sized(coregex.minLength() max size).generate(seed)
}
