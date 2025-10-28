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
import org.scalacheck.Shrink

private[scalacheck] trait CoregexGenVersionSpecific {
  import scala.compat.java8.StreamConverters._

  def shrinkFor(coregex: Coregex): Shrink[String] = {
    val shrinks = coregex.shrink().toScala[Stream]
    Shrink { larger =>
      shrinks.map(coregex => coregex.generate(larger.length.toLong)).filter(_.length < larger.length)
    }
  }
}
