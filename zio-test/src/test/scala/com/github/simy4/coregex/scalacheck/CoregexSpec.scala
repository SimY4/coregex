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

package com.github.simy4.coregex.scalacheck

import zio.Scope
import zio.test._

import java.net.InetAddress
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.regex.Pattern

object CoregexSpec extends ZIOSpecDefault {
  def spec: Spec[TestEnvironment with Scope, Any] = suite("Coregex")(
    test("should generate matching UUID string") {
      check(
        CoregexGen.fromPattern(
          Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}")
        )
      ) { uuid =>
        assertTrue(uuid == UUID.fromString(uuid).toString)
      }
    },
    test("should generate matching IPv4 string") {
      check(
        CoregexGen.fromPattern(
          Pattern.compile("((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])")
        )
      ) { ipv4 =>
        ipv4
          .split('.')
          .zip(InetAddress.getByName(ipv4).getHostAddress.split('.'))
          .map { case (expected, actual) => assertTrue(expected.toInt == actual.toInt) }
          .reduce(_ && _)
      }
    },
    test("should generate matching ISO-8601 date string") {
      check(
        CoregexGen.fromPattern(
          Pattern.compile(
            "[12]\\d{3}-(?:0[1-9]|1[012])-(?:0[1-9]|1\\d|2[0-8])T(?:1\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d{2}[1-9])?Z"
          )
        )
      ) { iso8601Date =>
        val formatter = DateTimeFormatter.ISO_INSTANT
        assertTrue(iso8601Date == formatter.format(formatter.parse(iso8601Date)))
      }
    },
    test("should generate unique strings") {
      check(Gen.listOf(CoregexGen.fromPattern(Pattern.compile("[a-zA-Z0-9]{32,}")))) { strings =>
        assertTrue(strings.forall { s =>
          s.length >= 32 && s.forall(_.isLetterOrDigit)
        }) && assertTrue(strings.size == strings.toSet.size)
      }
    }
  )
}
