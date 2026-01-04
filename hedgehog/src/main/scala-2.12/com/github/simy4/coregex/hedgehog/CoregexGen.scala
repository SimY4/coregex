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

package com.github.simy4.coregex
package hedgehog

import _root_.hedgehog.{ Gen, MonadGen, Range }
import core.Coregex

import java.util.regex.Pattern
import scala.util.matching.Regex

object CoregexGen {
  import scala.compat.java8.StreamConverters._

  def fromRegex[M[_]: MonadGen](regex: Regex): M[String] = fromPattern(regex.pattern)

  def fromPattern[M[_]: MonadGen](regex: Pattern): M[String] = apply(Coregex.from(regex))

  def apply[M[_]](coregex: Coregex)(implicit M: MonadGen[M]): M[String] =
    M.lift {
      Gen.long(Range.constant(Long.MinValue, Long.MaxValue)).flatMap { seed =>
        Gen.constant(coregex.generate(seed)).shrink { str =>
          coregex
            .shrink()
            .toScala[Stream]
            .map(_.generate(seed))
            .filter(_.length < str.length)
            .toList
        }
      }
    }
}
