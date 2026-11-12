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

package com.github.simy4.coregex.core

import munit.ScalaCheckSuite
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Prop._

import java.util.Optional
import java.util.regex.Pattern

class NFASuite extends ScalaCheckSuite with CoregexArbitraries {

  def genPatternPairs(): Gen[(Pattern, Pattern)] = Gen.oneOf(
    List(
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}") ->
        Pattern.compile("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"),
      Pattern.compile("((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])") ->
        Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}"),
      Pattern.compile("#([0-9a-fA-F]{3}){1,2}") -> Pattern.compile("#[0-9a-fA-F]{3,6}"),
      Pattern.compile("\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)") ->
        Pattern.compile("[\\dTZ:+-]+"),
      Pattern.compile("4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}") -> Pattern.compile("\\d{13,16}"),
      Pattern.compile(
        "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$"
      ) -> Pattern.compile("arn:[\\w.:/-]+"),
      Pattern.compile(
        "^(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)(?:-(?<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"
      ) -> Pattern.compile("[0-9a-zA-Z.+-]+"),
      Pattern.compile("\\Qhttps://\\E(?<host>foo|bar|baz)\\Q.com/\\E(?<path>[\\w-]+)") ->
        Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"),
      // `\w*` alone allows empty, but concatenated with `\w` it does not.
      Pattern.compile("(?<a>\\w*)(?<b>\\w)") -> Pattern.compile("[\\w-]+"),
      Pattern.compile("^(a+)+$")             -> Pattern.compile("a+"),
      Pattern.compile("^([a-zA-Z]+)*$")      -> Pattern.compile("[a-zA-Z]*"),
      Pattern.compile("^(a|aa)+$")           -> Pattern.compile("a+"),
      Pattern.compile("^(a|a?)+$")           -> Pattern.compile("a*"),
      Pattern.compile("^(?:||)$")            -> Pattern.compile("")
    )
  )

  implicit val arbPatternPairs: Arbitrary[(Pattern, Pattern)] = Arbitrary(genPatternPairs())

  private def re(regex: String): Coregex = Coregex.from(Pattern.compile(regex))

  private def nfa(regex: String): NFA = NFA.of(re(regex))

  property("should cover example regex") {
    forAll { (pair: (Pattern, Pattern), seed: Long) =>
      val (sub, sup) = pair
      val subset     = Coregex.from(sub)
      val superset   = Coregex.from(sup)
      val generated  = subset.generate(seed)
      val witness    = superset.difference(subset)
      (subset.isSubsetOf(superset) :| s"${sub.pattern()} should be a subset of ${sup.pattern()}") &&
      (sup.matcher(generated).matches() :| s"${sup.pattern()} should match generated: $generated") &&
      (subset.intersection(superset).isPresent :| s"${sub.pattern()} should intersect ${sup.pattern()}") &&
      ((!witness.isPresent || sup.matcher(witness.get).matches() && !sub.matcher(witness.get).matches()) :|
        s"${sup.pattern()} but not ${sub.pattern()} should match witness: $witness")
    }
  }

  // region language operations
  test("possibly empty fragment is not a subset of non empty one") {
    assertEquals(re("(?<x>\\w*)").difference(re("[\\w-]+")), Optional.of(""))
  }

  test("dot fragment is not a subset of word characters") {
    val witness = re("(?<x>.+)").difference(re("[\\w-]+"))
    assert(witness.isPresent && 1 == witness.get.length, s"$witness should be a single char")
    assert(!Pattern.matches("[\\w-]+", witness.get), s"${witness.get} should not match [\\w-]+")
  }

  test("intersection of disjoint languages is empty") {
    assertEquals(re("[a-z]+").intersection(re("\\d+")), Optional.empty[String]())
  }

  test("intersection returns the shortest common string") {
    assertEquals(re("[a-z]+").intersection(re("x.*")), Optional.of("x"))
    assertEquals(re("a{3}b?").intersection(re("a{3}b?")), Optional.of("aaa"))
    assertEquals(re("[a&&b]").intersection(re(".*")), Optional.empty[String]())
  }

  test("toString shows states with transitions") {
    assertEquals(
      nfa("a|b*").toString,
      """0 (start) --[a]--> 1 --ε--> 2
        |1 (accept)
        |2 --[b]--> 2 --ε--> 1
        |""".stripMargin
    )
    assert(nfa(".[^ab]").toString.startsWith("0 (start) --[^\n\r\u0085\u2028\u2029]--> 2\n"))
  }

  test("witnesses use the simplest chars, same as shrinking") {
    assertEquals(re("#[0-9a-fA-F]{3,6}").difference(re("#(?:[0-9a-fA-F]{3}){1,2}")), Optional.of("#0000"))
    assertEquals(re("[a-zA-Z]+").difference(re("[a-z]+")), Optional.of("A"))
    assertEquals(re("[\\s\\u00E9]").intersection(re("\\W")), Optional.of(" "))
    assertEquals(re("\\W").difference(re("[ -~]")), Optional.of("\u0000"))
  }

  test("empty regex matches empty string only") {
    assertEquals(re("(?:)").intersection(re("")), Optional.of(""))
    assertEquals(re("a(?:)b").intersection(re("ab")), Optional.of("ab"))
    assertEquals(re("(?:a|)").difference(re("a")), Optional.of(""))
  }

  // (sub, sup, input matched by sub only if sub is not a subset of sup)
  List(
    ("[^a]", "[^a\\n]", Some("\n")),
    ("\\n", "\\D", None),
    ("\\n", "[^\\w]", None),
    ("\\uFF01", ".", None),
    ("(?s).", "[^\\uD800-\\uDFFF]", None),
    ("\u00C9", "(?i)\u00E9", Some("\u00C9")),
    ("\u00C9", "(?iu)\u00E9", None),
    ("K", "(?i)k", None),
    ("a\\Z", "a", None),
    ("a", "a\\Z", None)
  ).foreach { case (sub, sup, witness) =>
    test(s"$sub is ${if (witness.isEmpty) "" else "not "}a subset of $sup like in java.util.regex") {
      witness.foreach { w =>
        assert(Pattern.matches(sub, w) && !Pattern.matches(sup, w), s"$w should only match $sub")
      }
      assertEquals(re(sub).difference(re(sup)), witness.fold(Optional.empty[String]())(Optional.of(_)))
    }
  }

  List(
    ".",
    "(?s).",
    "(?d).",
    "[^a]",
    "[^\\n]",
    "\\d",
    "\\D",
    "\\w",
    "\\W",
    "\\s",
    "\\S",
    "\\h",
    "\\H",
    "\\v",
    "\\V",
    "\\t",
    "\\f",
    "\\a",
    "\\e",
    "\\cA",
    "\\p{all}",
    "\\p{Lower}",
    "\\p{Upper}",
    "\\p{ASCII}",
    "\\p{Digit}",
    "\\p{XDigit}",
    "\\p{Alnum}",
    "\\p{Alpha}",
    "\\p{Punct}",
    "\\p{Graph}",
    "\\p{Print}",
    "\\p{Blank}",
    "\\p{Space}",
    "\\p{javaWhitespace}",
    "\\p{javaLowerCase}",
    "\\p{javaUpperCase}",
    "(?i)[a-z]",
    "(?i)\u00E9",
    "(?iu)\u00E9",
    "(?iU)\u00E9",
    "(?U)\\p{ASCII}",
    "[\\w&&[^\\d]]"
  ).foreach { regex =>
    test(s"$regex should accept the same chars as java.util.regex") {
      val pattern = Pattern.compile(regex)
      val nfa     = NFA.of(Coregex.from(pattern))
      val wrong   = (Character.MIN_VALUE to Character.MAX_VALUE)
        .filterNot(Character.isSurrogate)
        .map(_.toString)
        .filter(ch => nfa.matches(ch) != pattern.matcher(ch).matches())
      assert(wrong.isEmpty, s"${wrong.take(5).map(ch => f"\\u${ch.charAt(0).toInt}%04X").mkString(", ")} ...")
    }
  }

  test("unicode character class only affects predefined char classes") {
    assert(re("(?U)abc").isSubsetOf(re("abc")))
    assert(re("(?U)[a-c]+").isSubsetOf(re("\\w+")))
  }

  test("reluctant and greedy quantifiers have the same language") {
    assert(re("a+?b*?").isSubsetOf(re("a+b*")))
    assert(re("a+b*").isSubsetOf(re("a+?b*?")))
  }

  test("nested stars") {
    val nested = nfa("(?:a*b*)*")
    assert(nested.matches(""))
    assert(nested.matches("abba"))
    assert(!nested.matches("abc"))
    assert(re("(?:a*b*)*").isSubsetOf(re("[ab]*")))
    assert(re("[ab]*").isSubsetOf(re("(?:a*b*)*")))
  }

  test("star inside union does not leak") {
    val union = nfa("a*|b")
    assert(!union.matches("bb"))
    assert(!union.matches("ab"))
  }

  List(
    "^abc$",
    "\\Aabc\\z",
    "(?m)^abc$",
    "(^abc$)",
    "^(?:abc)$",
    "^^abc$$",
    "^a$|^bc$|(?:^ab)",
    "(?:^a|^bc)$",
    "^abc\\Z",
    "^(?:^abc$)$",
    "^(?:a|^bc)$",
    "(?m)(?i)^abc$"
  ).foreach { regex =>
    test(s"boundary anchors in $regex are no-ops") {
      val pattern = Pattern.compile(regex)
      val nfa     = NFA.of(Coregex.from(pattern))
      List("", "a", "ab", "abc", "bc", "abcd", "\n", "abc\n").foreach { input =>
        assertEquals(nfa.matches(input), pattern.matcher(input).matches(), s"$input in $regex")
      }
    }
  }
  // endregion

  // region unsupported constructs
  List(
    "(a)\\1",
    "(?<x>a)\\k<x>",
    "(?>a*)",
    "a*+",
    "(?=a)a",
    "(?!a)b",
    "(?<=a)b",
    "(?<!a)b",
    "\\bfoo",
    "a^b",
    "a$b",
    "(?:^a)*",
    "^a?^b",
    "(?<=)abc",
    "abc(?=)",
    "$^",
    "(?m)a$\\n^b",
    "a\\Zb",
    "(?U)\\w",
    "(?U)[\\w-]",
    "(?U)[^\\d]",
    "(?U)\\p{Alpha}"
  ).foreach { regex =>
    test(s"$regex is not supported") {
      val _ = intercept[UnsupportedOperationException](re(regex).isSubsetOf(re("a")))
      val _ = intercept[UnsupportedOperationException](re("a").isSubsetOf(re(regex)))
    }
  }
  // endregion

  // region properties
  private val supportedRange: Gen[QuantifyRange] =
    for {
      min <- Gen.choose(0, 5)
      add <- Gen.choose(0, 5)
      max <- Gen.oneOf(
        Gen.const(-1),
        Gen.choose(min, min + add)
      )
    } yield QuantifyRange(min, max, Coregex.Quantified.Type.GREEDY)
  private val genSupported: Gen[Coregex] = Gen.resize(20, genCoregex(supportedRange))

  property("nfa should accept generated strings") {
    forAll(genSupported, Arbitrary.arbitrary[Long]) { (coregex, seed) =>
      val generated = coregex.generate(seed)
      NFA.of(coregex).matches(generated) :| s"$generated in $coregex"
    }
  }

  property("every coregex is a subset of itself and of its union") {
    forAll(genSupported, genSupported) { (left, right) =>
      (left.isSubsetOf(left) :| "reflexive") &&
      (left.isSubsetOf(new Coregex.Union(left, right)) :| "union")
    }
  }

  property("difference witness should be accepted by left and rejected by right") {
    forAll(genSupported, genSupported) { (left, right) =>
      val witness = left.difference(right)
      witness.isPresent ==> {
        (NFA.of(left).matches(witness.get) :| s"${witness.get} in $left") &&
        (!NFA.of(right).matches(witness.get) :| s"${witness.get} not in $right")
      }
    }
  }

  property("difference should find the shortest witness if any exists") {
    forAll(genSupported, genSupported, Arbitrary.arbitrary[Long]) { (left, right, seed) =>
      val generated = left.generate(seed)
      !NFA.of(right).matches(generated) ==> {
        val witness = left.difference(right)
        (witness.isPresent :| s"$generated in $left but not in $right") &&
        (witness.get.length <= generated.length) :| s"${witness.get} is no longer than $generated"
      }
    }
  }

  property("intersection should find the shortest witness if any exists") {
    forAll(genSupported, genSupported, Arbitrary.arbitrary[Long]) { (left, other, seed) =>
      val right     = new Coregex.Union(other, left)
      val generated = left.generate(seed)
      val witness   = left.intersection(right)
      (witness.isPresent :| s"$generated in both $left and $right") && {
        (witness.get.length <= generated.length) :| s"${witness.get} is no longer than $generated" &&
        (NFA.of(left).matches(witness.get) && NFA.of(right).matches(witness.get)) :| s"${witness.get} in both"
      }
    }
  }
  // endregion
}
