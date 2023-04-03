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

package com.github.simy4.coregex.core

import org.scalacheck.{ Arbitrary, Gen, Shrink }
import rng.RandomRNG

import java.util.regex.Pattern

trait CoregexArbitraries {
  import scala.jdk.StreamConverters._

  type Flags <: Int
  implicit val arbitraryFlags: Arbitrary[Flags] = Arbitrary(genFlags)
  def genFlags: Gen[Flags] =
    for {
      n <- Gen.choose(0, 9)
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

  implicit val arbitraryCoregex: Arbitrary[Coregex] = Arbitrary(genCoregex())
  def genCoregex(level: Int = 10, charGen: Gen[Char] = Gen.alphaNumChar): Gen[Coregex] =
    if (level > 0)
      Gen.lzy(
        Gen.frequency(
          (1, genCoregexConcat(level - 1, charGen)),
          (3, genCoregexLiteral(charGen)),
          (3, genCoregexSet(charGen)),
          (1, genCoregexUnion(level - 1, charGen))
        )
      )
    else
      Gen.oneOf(genCoregexLiteral(charGen), genCoregexSet(charGen))

  implicit val arbitraryCoregexConcat: Arbitrary[Coregex.Concat] = Arbitrary(genCoregexConcat())
  def genCoregexConcat(level: Int = 10, charGen: Gen[Char] = Gen.alphaNumChar): Gen[Coregex.Concat] =
    Gen.lzy(for {
      first <- genCoregex(level - 1, charGen)
      size  <- Gen.choose(0, 10)
      rest  <- Gen.listOfN(size, genCoregex(level - 1, charGen))
    } yield new Coregex.Concat(first, rest: _*))

  implicit val arbitraryCoregexLiteral: Arbitrary[Coregex.Literal] = Arbitrary(genCoregexLiteral())
  def genCoregexLiteral(charGen: Gen[Char] = Gen.alphaNumChar): Gen[Coregex.Literal] =
    for (literal <- Gen.stringOf(charGen); flags <- genFlags) yield new Coregex.Literal(literal, flags)
  implicit def shrinkCoregexLiteral(implicit shrinkLiteral: Shrink[String]): Shrink[Coregex.Literal] =
    Shrink(literal => shrinkLiteral.shrink(literal.literal()).map(new Coregex.Literal(_)))

  implicit val arbitraryCoregexSet: Arbitrary[Coregex.Set]                   = Arbitrary(genCoregexSet())
  implicit val shrinkCoregexSet: Shrink[Coregex.Set]                         = Shrink.withLazyList(shrinkCoregexSet(_))
  def genCoregexSet(charGen: Gen[Char] = Gen.alphaNumChar): Gen[Coregex.Set] = genSet(charGen).map(new Coregex.Set(_))
  def shrinkCoregexSet(set: Coregex.Set): LazyList[Coregex.Set] = shrinkSet(set.set()).map(new Coregex.Set(_))

  implicit val arbitraryCoregexUnion: Arbitrary[Coregex.Union] = Arbitrary(genCoregexUnion())
  def genCoregexUnion(level: Int = 10, charGen: Gen[Char] = Gen.alphaNumChar): Gen[Coregex.Union] =
    Gen.lzy(for {
      first <- genCoregex(level - 1, charGen)
      size  <- Gen.choose(0, 10)
      rest  <- Gen.listOfN(size, genCoregex(level - 1, charGen))
    } yield new Coregex.Union(first, rest: _*))

  implicit val arbitraryRNG: Arbitrary[RNG] = Arbitrary(genRNG)
  def genRNG: Gen[RNG]                      = Gen.long.map(new RandomRNG(_))

  implicit val arbitrarySet: Arbitrary[Set] = Arbitrary(genSet())
  implicit val shrinkSet: Shrink[Set]       = Shrink.withLazyList(shrinkSet(_))
  def genSet(charGen: Gen[Char] = Gen.asciiPrintableChar): Gen[Set] = Gen.recursive[Set] { fix =>
    Gen.frequency(
      (
        2,
        for (flags <- genFlags; ch <- charGen; rest <- Gen.stringOf(charGen))
          yield Set.builder(flags).set(ch, rest.toCharArray: _*).build()
      ),
      (
        2,
        for (flags <- genFlags; ch1 <- charGen; ch2 <- charGen)
          yield {
            val start = ch1 min ch2
            val end = {
              val end = ch1 max ch2
              if (start == end) (end + 1).toChar else end
            }
            Set.builder(flags).set(start, end).build()
          }
      ),
      (1, for (flags <- genFlags; set <- fix.map(set => Set.builder(flags).set(set).build())) yield set)
    )
  }
  def shrinkSet(set: Set): LazyList[Set] = set.shrink().toScala(LazyList)
}
