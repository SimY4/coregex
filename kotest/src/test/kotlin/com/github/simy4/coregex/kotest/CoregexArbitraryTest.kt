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

package com.github.simy4.coregex.kotest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldMatchEach
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.should
import io.kotest.matchers.string.beUUID
import io.kotest.matchers.string.haveMinLength
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import java.net.InetAddress
import java.time.format.DateTimeFormatter

public class CoregexArbitraryTest: FunSpec() {
  init {
    test("should generate matching UUID string") {
      checkAll(CoregexArbitrary.of("[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}")) { uuid ->
        uuid should beUUID()
      }
    }

    test("should generate matching IPv4 string") {
      checkAll(CoregexArbitrary.of("((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])")) { ipv4 ->
        val expected = ipv4.split('.')
        val actual = InetAddress.getByName(ipv4).hostAddress.split('.')
        expected.zip(actual).shouldMatchEach({ (expected, actual) ->
          expected.toInt() shouldBeEqual actual.toInt()
        })
      }
    }

    test("should generate matching ISO date string") {
      val formatter = DateTimeFormatter.ISO_INSTANT
      checkAll(CoregexArbitrary.of("[12]\\d{3}-(?:0[1-9]|1[012])-(?:0[1-9]|1\\d|2[0-8])T(?:1\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d{2}[1-9])?Z")) { iso8601Date ->
        iso8601Date shouldBeEqual formatter.format(formatter.parse(iso8601Date))
      }
    }

    test("should generate unique strings") {
      checkAll(Arb.list(CoregexArbitrary.of("[a-zA-Z0-9]{32,}"))) { strings ->
        strings.shouldMatchEach({ s ->
          s should haveMinLength(32)
          s.toList().shouldMatchEach({ Character.isLetterOrDigit(it).shouldBeTrue() })
        })
      }
    }
  }
}
