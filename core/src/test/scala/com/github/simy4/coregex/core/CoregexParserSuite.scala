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

import java.util.regex.Pattern

class CoregexParserSuite extends ScalaCheckSuite with CoregexArbitraries {

  def genPatternExamples(): Gen[Pattern] = Gen.oneOf(
    List(
      Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"),
      Pattern.compile("\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)"),
      Pattern.compile(
        "P(\\d+W|\\d+Y(\\d+M)?(\\d+D)?(T(\\d+H)?(\\d+M)?(\\d+([.,]\\d+)?S)?)?|\\d+M(\\d+D)?(T(\\d+H)?(\\d+M)?(\\d+([.,]\\d+)?S)?)?|\\d+D(T(\\d+H)?(\\d+M)?(\\d+([.,]\\d+)?S)?)?|T(\\d+H(\\d+M)?(\\d+([.,]\\d+)?S)?|\\d+M(\\d+([.,]\\d+)?S)?|\\d+([.,]\\d+)?S))"
      ),
      Pattern.compile("([0-5]?\\d)\\s([01]?\\d|2[0-3])\\s([0-2]?\\d|3[01])\\s(0?\\d|1[0-2])\\s([0-6])"),
      Pattern.compile("((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"),
      Pattern.compile(
        """(
          |([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|          # 1:2:3:4:5:6:7:8
          |([0-9a-fA-F]{1,4}:){1,7}:|                         # 1::                              1:2:3:4:5:6:7::
          |([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|         # 1::8             1:2:3:4:5:6::8  1:2:3:4:5:6::8
          |([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|  # 1::7:8           1:2:3:4:5::7:8  1:2:3:4:5::8
          |([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|  # 1::6:7:8         1:2:3:4::6:7:8  1:2:3:4::8
          |([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|  # 1::5:6:7:8       1:2:3::5:6:7:8  1:2:3::8
          |([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|  # 1::4:5:6:7:8     1:2::4:5:6:7:8  1:2::8
          |[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|       # 1::3:4:5:6:7:8   1::3:4:5:6:7:8  1::8
          |:((:[0-9a-fA-F]{1,4}){1,7}|:)|                     # ::2:3:4:5:6:7:8  ::2:3:4:5:6:7:8 ::8       ::
          |fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|     # fe80::7:8%eth0   fe80::7:8%1     (link-local IPv6 addresses with zone index)
          |::(ffff(:0{1,4}){0,1}:){0,1}
          |((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}
          |(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|          # ::255.255.255.255   ::ffff:255.255.255.255  ::ffff:0:255.255.255.255  (IPv4-mapped IPv6 addresses and IPv4-translated addresses)
          |([0-9a-fA-F]{1,4}:){1,4}:
          |((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}
          |(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])           # 2001:db8:3:4::192.0.2.33  64:ff9b::192.0.2.33 (IPv4-Embedded IPv6 Address)
          |)""".stripMargin,
        Pattern.COMMENTS
      ),
      Pattern.compile(
        "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5e-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])"
      ),
      Pattern.compile(
        "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$"
      ),
      Pattern.compile(
        "^(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)(?:-(?<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"
      ),
      Pattern.compile("#([0-9a-fA-F]{3}){1,2}"),
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"),
      Pattern.compile("4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}"),
      Pattern.compile("(\\*{1,2})[^*]+\\1"),
      Pattern.compile("<([A-Z][A-Z0-9]*) [^>]*>.*?</\\1>"),
      Pattern.compile("((?i)[a-z]+(?-i)-[A-Z]){3,6}"),
      Pattern.compile("[a-z&&[^aeiou]]+[]][a-z&&aeiou&&ei]"),
      Pattern.compile("^(a+)+$"),
      Pattern.compile("^([a-zA-Z]+)*$"),
      Pattern.compile("^(a|aa)+$"),
      Pattern.compile("^(a|a?)+$"),
      Pattern.compile("^(?:||)$"),
      Pattern.compile("(\\d+) (\\w+) == (\\1) \\2 \\3"),
      Pattern.compile("(?!=.{10,}).+"),
      Pattern.compile("")
    ) ::: (if (scala.util.Properties.isJavaAtLeast(9)) {
             List(
               Pattern.compile("\\N{WHITE SMILING FACE}")
             )
           } else Nil)
  )

  implicit val arbPatternExamples: Arbitrary[Pattern] = Arbitrary(genPatternExamples())

  property("should parse example regex") {
    forAll { (pattern: Pattern, seed: Long) =>
      val actual    = Coregex.from(pattern)
      val generated = actual.generate(seed)

      assert(
        clue(pattern).matcher(clue(generated)).matches(),
        s"${pattern.pattern()} should match generated: $generated"
      )
    }
  }

  property("should parse quoted regex") {
    forAll { (pattern: Pattern, seed: Long) =>
      val expected  = Pattern.compile(Pattern.quote(pattern.pattern()))
      val actual    = Coregex.from(expected)
      val generated = actual.generate(seed)
      assert(
        clue(expected).matcher(clue(generated)).matches(),
        s"${expected.pattern()} should match generated: $generated"
      )
    }
  }

  property("should parse literal regex") {
    forAll { (pattern: Pattern, seed: Long) =>
      val expected  = Pattern.compile(pattern.pattern(), Pattern.LITERAL)
      val actual    = Coregex.from(expected)
      val generated = actual.generate(seed)
      assert(
        clue(expected).matcher(clue(generated)).matches(),
        s"${expected.pattern()} should match generated: $generated"
      )
    }
  }

  property("should fail to generate strings") {
    forAll(
      Gen.oneOf(
        Pattern.compile("(?=(abc))"),
        Pattern.compile("\\d+(?= dollars)"),
        Pattern.compile("(?<=[a-z0-9])(?=[A-Z])")
      ),
      Gen.long
    ) { (pattern, seed) =>
      val _ = intercept[IllegalStateException] {
        val coregex = Coregex.from(pattern)
        coregex.generate(seed)
      }
    }
  }
}
