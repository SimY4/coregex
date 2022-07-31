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

import org.scalacheck.{ Gen, Properties }
import org.scalacheck.Prop._

import java.util.regex.Pattern

object CoregexParserSpecification extends Properties("CoregexParser") {
  property("should parse example regex") = forAll(
    Gen.oneOf[(Coregex, Pattern)](
      new Coregex.Concat(
        new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 8, 8, true),
        new Coregex.Literal("-"),
        new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 4, 4, true),
        new Coregex.Literal("-"),
        new Coregex.Set(Set.builder().range('0', '5').build()),
        new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 3, 3, true),
        new Coregex.Literal("-"),
        new Coregex.Set(Set.builder().set('0', '8', '9', 'a', 'b').build()),
        new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 3, 3, true),
        new Coregex.Literal("-"),
        new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 12, 12, true)
      ) ->
        Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}"),
      new Coregex.Concat(
        new Coregex.Literal(""),
        new Coregex.Union(
          new Coregex.Concat(new Coregex.Literal("http"), new Coregex.Quantified(new Coregex.Literal("s"), 0, 1, true)),
          new Coregex.Literal("ftp"),
          new Coregex.Literal("file")
        ),
        new Coregex.Literal("://"),
        new Coregex.Quantified(
          new Coregex.Set(
            Set
              .builder()
              .single('-')
              .range('a', 'z')
              .range('A', 'Z')
              .range('0', '9')
              .set('+', '&', '@', '#', '/', '%', '?', '=', '~', '_', '|', '!', ':', ',', '.', ';')
              .build()
          ),
          0,
          -1,
          true
        ),
        new Coregex.Set(
          Set
            .builder()
            .single('-')
            .range('a', 'z')
            .range('A', 'Z')
            .range('0', '9')
            .set('+', '&', '@', '#', '/', '%', '=', '~', '_', '|')
            .build()
        )
      ) ->
        Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"),
      new Coregex.Concat(
        new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').build()), 4, 4, true),
        new Coregex.Literal("-"),
        new Coregex.Set(Set.builder().set('0', '1').build()),
        new Coregex.Set(Set.builder().range('0', '9').build()),
        new Coregex.Literal("-"),
        new Coregex.Set(Set.builder().range('0', '3').build()),
        new Coregex.Set(Set.builder().range('0', '9').build()),
        new Coregex.Literal("T"),
        new Coregex.Set(Set.builder().range('0', '2').build()),
        new Coregex.Set(Set.builder().range('0', '9').build()),
        new Coregex.Literal(":"),
        new Coregex.Set(Set.builder().range('0', '5').build()),
        new Coregex.Set(Set.builder().range('0', '9').build()),
        new Coregex.Literal(":"),
        new Coregex.Set(Set.builder().range('0', '5').build()),
        new Coregex.Set(Set.builder().range('0', '9').build()),
        new Coregex.Union(
          new Coregex.Concat(
            new Coregex.Set(Set.builder().set('+', '-').build()),
            new Coregex.Set(Set.builder().range('0', '2').build()),
            new Coregex.Set(Set.builder().range('0', '9').build()),
            new Coregex.Literal(":"),
            new Coregex.Set(Set.builder().range('0', '5').build()),
            new Coregex.Set(Set.builder().range('0', '9').build())
          ),
          new Coregex.Literal("Z")
        )
      ) ->
        Pattern.compile("\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)"),
      new Coregex.Literal("whatever")           -> Pattern.compile(Pattern.quote("whatever")),
      new Coregex.Literal("double\\Q\\Equoted") -> Pattern.compile(Pattern.quote("double\\Q\\Equoted"))
    )
  ) { case (coregex, regex) => coregex == Coregex.from(regex) }

  final case class ~(coregex: Coregex, regex: Pattern)
}