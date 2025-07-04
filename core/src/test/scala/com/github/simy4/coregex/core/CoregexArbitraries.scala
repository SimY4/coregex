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

import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary

import java.util.regex.Pattern

trait CoregexArbitraries {
  type Flags <: Int
  implicit val arbitraryFlags: Arbitrary[Flags] = Arbitrary(genFlags)
  def genFlags: Gen[Flags]                      =
    for {
      n     <- Gen.choose(0, 9)
      flags <- Gen.listOfN(
        n,
        Gen.oneOf(
          Pattern.CANON_EQ,
          Pattern.CASE_INSENSITIVE,
          Pattern.COMMENTS,
          Pattern.DOTALL,
          Pattern.LITERAL,
          Pattern.MULTILINE,
          Pattern.UNICODE_CASE,
          Pattern.UNICODE_CHARACTER_CLASS,
          Pattern.UNIX_LINES
        )
      )
    } yield flags.foldLeft(0)(_ | _).asInstanceOf[Flags]

  implicit val arbitraryCoregex: Arbitrary[Coregex]                   = Arbitrary(genCoregex())
  def genCoregex(charGen: Gen[Char] = Gen.alphaNumChar): Gen[Coregex] =
    for {
      single <- Gen.sized { height =>
        if (height > 0)
          Gen.oneOf(
            genCoregexConcat(charGen),
            genCoregexUnion(charGen),
            genSet(charGen),
            Gen.const(Coregex.empty())
          )
        else Gen.oneOf(genSet(charGen), Gen.const(Coregex.empty()))
      }
      coregex <- Gen.frequency(
        9 -> Gen.const(single),
        1 -> arbitrary[QuantifyRange].map(r => single.quantify(r.min, r.max, r.`type`))
      )
    } yield coregex

  implicit val arbitraryCoregexConcat: Arbitrary[Coregex.Concat]                   = Arbitrary(genCoregexConcat())
  def genCoregexConcat(charGen: Gen[Char] = Gen.alphaNumChar): Gen[Coregex.Concat] =
    for {
      first <- Gen.sized(h => Gen.resize(h / 4, genCoregex(charGen)))
      size  <- Gen.size
      rest  <- Gen.listOfN(size % 10, Gen.resize(size / 4, genCoregex(charGen)))
    } yield new Coregex.Concat(first, rest: _*)

  implicit val arbitraryCoregexRef: Arbitrary[Coregex.Ref] = Arbitrary(genCoregexRef)
  def genCoregexRef: Gen[Coregex.Ref]                      =
    for {
      fst  <- Gen.alphaChar
      size <- Gen.size
      rest <- Gen.listOfN(size % 10, Gen.alphaNumChar)
    } yield new Coregex.Ref(rest.mkString(fst.toString, "", ""))

  implicit val arbitrarySet: Arbitrary[Set]                         = Arbitrary(genSet())
  def genSet(charGen: Gen[Char] = Gen.asciiPrintableChar): Gen[Set] = Gen.recursive[Set] { fix =>
    Gen.oneOf(
      for (flags <- genFlags; ch <- charGen; rest <- Gen.stringOf(charGen))
        yield Set.builder(flags).set(ch, rest.toCharArray: _*).build(),
      for (flags <- genFlags; ch1 <- charGen; ch2 <- charGen; if ch1 != ch2)
        yield Set.builder(flags).range(ch1 min ch2, ch1 max ch2).build(),
      for (flags <- genFlags; set <- fix.map(set => Set.builder(flags).union(set).build())) yield set
    )
  }

  implicit val arbitraryCoregexUnion: Arbitrary[Coregex.Union]                   = Arbitrary(genCoregexUnion())
  def genCoregexUnion(charGen: Gen[Char] = Gen.alphaNumChar): Gen[Coregex.Union] =
    for {
      first <- Gen.sized(h => Gen.resize(h / 4, genCoregex(charGen)))
      size  <- Gen.size
      rest  <- Gen.listOfN(size % 10, Gen.resize(size / 4, genCoregex(charGen)))
    } yield new Coregex.Union(first, rest: _*)
}
