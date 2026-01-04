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

package com.github.simy4.coregex.hedgehog

import hedgehog._
import hedgehog.runner.{ property, Properties, Test }

import java.net.InetAddress
import java.time.format.DateTimeFormatter
import java.util.UUID

object CoregexTest extends Properties {

  def tests: List[Test] = List(
    property("should generate matching UUID string", shouldGenerateMatchingUUIDString),
    property("should generate matching IPv4 string", shouldGenerateMatchingIPv4String),
    property("should generate matching ISO-8601 date string", shouldGenerateMatchingISO8601DateString),
    property("should generate unique strings", shouldGenerateUniqueStrings)
  )

  def shouldGenerateMatchingUUIDString: Property = for {
    uuid <- CoregexGen.fromRegex("[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}".r).forAll
  } yield uuid ==== UUID.fromString(uuid).toString

  def shouldGenerateMatchingIPv4String: Property = for {
    ipv4 <- CoregexGen.fromRegex("((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])".r).forAll
  } yield Result.all(
    ipv4
      .split('.')
      .zip(InetAddress.getByName(ipv4).getHostAddress.split('.'))
      .map { case (expected, actual) => expected.toInt ==== actual.toInt }
      .toList
  )

  def shouldGenerateMatchingISO8601DateString: Property = for {
    iso8601Date <- CoregexGen
      .fromRegex(
        "[12]\\d{3}-(?:0[1-9]|1[012])-(?:0[1-9]|1\\d|2[0-8])T(?:1\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d{2}[1-9])?Z".r
      )
      .forAll
  } yield {
    val formatter = DateTimeFormatter.ISO_INSTANT
    iso8601Date ==== formatter.format(formatter.parse(iso8601Date))
  }

  def shouldGenerateUniqueStrings: Property = for {
    strings <- Gen.list(CoregexGen.fromRegex("[a-zA-Z0-9]{32,}".r), Range.linear(0, 10)).forAll
  } yield {
    Result.all(strings.map { s =>
      Result.assert(s.length >= 32 && s.forall(_.isLetterOrDigit))
    }) and (strings.size ==== strings.toSet.size)
  }
}
