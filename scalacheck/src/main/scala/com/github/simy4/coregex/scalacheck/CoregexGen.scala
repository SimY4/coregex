package com.github.simy4.coregex.scalacheck

import com.github.simy4.coregex.core.{ Coregex, CoregexParser }
import com.github.simy4.coregex.core.rng.SimpleRNG
import org.scalacheck.{ Arbitrary, Gen }

import java.util.regex.Pattern
import scala.util.matching.Regex

object CoregexGen {
  implicit def arbitrary(implicit coregex: Coregex): Arbitrary[String] = Arbitrary(apply(coregex))

  def apply(regex: Regex): Gen[String] = apply(regex.pattern)

  def apply(regex: Pattern): Gen[String] = apply(CoregexParser.getInstance().parse(regex))

  def apply(implicit coregex: Coregex): Gen[String] = {
    for (seed <- Gen.long) yield coregex.generate(new SimpleRNG(seed))
  }
}
