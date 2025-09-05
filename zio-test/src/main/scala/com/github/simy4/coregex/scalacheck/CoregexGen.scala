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
import zio.Trace
import zio.stream.ZStream
import zio.test.{ Gen, Sample }

import java.util.regex.Pattern
import scala.util.matching.Regex

object CoregexGen {
  def fromRegex(regex: Regex)(implicit trace: Trace): Gen[Any, String] = fromPattern(regex.pattern)

  def fromPattern(regex: Pattern)(implicit trace: Trace): Gen[Any, String] = apply(Coregex.from(regex))

  def apply(coregex: Coregex)(implicit trace: Trace): Gen[Any, String] =
    Gen.long.flatMap { seed =>
      Gen.const(coregex.generate(seed)).reshrink(Sample(_, shrink(coregex, seed)))
    }

  private def shrink(coregex: Coregex, seed: Long): ZStream[Any, Nothing, Sample[Any, String]] =
    ZStream
      .fromJavaIterator(coregex.shrink().iterator())
      .orDie
      .map(coregex => Sample(coregex.generate(seed), shrink(coregex, seed)))
}
