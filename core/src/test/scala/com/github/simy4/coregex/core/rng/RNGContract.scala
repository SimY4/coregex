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
package rng

import munit.ScalaCheckSuite
import org.scalacheck.Prop._

trait RNGContract { this: ScalaCheckSuite =>
  def rng(seed: Long): RNG

  property("should generate int in range") {
    forAll { (bound: Short, seed: Long) =>
      val gt0Bound  = 1 + bound.toInt.abs
      val generated = rng(seed).genInteger(gt0Bound).getSecond
      (0 <= generated && generated < gt0Bound) :| s"0 <= $generated < $gt0Bound"
    }
  }
}
