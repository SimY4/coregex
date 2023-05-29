/*
 * Copyright 2021-2023 Alex Simkin
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

import com.github.simy4.coregex.core.rng.RandomRNG
import munit.{ Location, ScalaCheckSuite }

import java.util.regex.Pattern

class CoregexParserSuite extends ScalaCheckSuite with CoregexArbitraries {
  def shouldParseExampleRegex(expected: Coregex, pattern: Pattern, rng: RNG)(implicit loc: Location): Unit =
    test(s"should parse example regex: ${pattern.pattern()}") {
      val actual    = Coregex.from(pattern)
      val generated = actual.generate(rng)

      assertEquals(actual, expected)
      assert(pattern.matcher(generated).matches(), s"${pattern.pattern()} should match generated: $generated")
    }

  def shouldParseQuotedRegex(pattern: Pattern, rng: RNG)(implicit loc: Location): Unit =
    test(s"should parse quoted regex: ${pattern.pattern()}") {
      val expected  = Pattern.compile(Pattern.quote(pattern.pattern()))
      val actual    = Coregex.from(expected)
      val generated = actual.generate(rng)
      assert(expected.matcher(generated).matches(), s"${expected.pattern()} should match generated: $generated")
    }

  def shouldParseLiteralRegex(pattern: Pattern, rng: RNG)(implicit loc: Location): Unit =
    test(s"should parse literal regex: ${pattern.pattern()}") {
      val expected  = Pattern.compile(pattern.pattern(), Pattern.LITERAL)
      val actual    = Coregex.from(expected)
      val generated = actual.generate(rng)
      assert(expected.matcher(generated).matches(), s"${expected.pattern()} should match generated: $generated")
    }

  for {
    (expected, pattern) <-
      List(
        new Coregex.Concat(
          new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 8, 8),
          new Coregex.Literal("-"),
          new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 4, 4),
          new Coregex.Literal("-"),
          new Coregex.Set(Set.builder().range('0', '5').build()),
          new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 3, 3),
          new Coregex.Literal("-"),
          new Coregex.Set(Set.builder().set('0', '8', '9', 'a', 'b').build()),
          new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 3, 3),
          new Coregex.Literal("-"),
          new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 12, 12)
        ) ->
          Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}"),
        new Coregex.Concat(
          new Coregex.Union(
            new Coregex.Concat(new Coregex.Literal("http"), new Coregex.Quantified(new Coregex.Literal("s"), 0, 1)),
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
            0
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
          new Coregex.Quantified(new Coregex.Set(Set.builder().range('0', '9').build()), 4, 4),
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
        new Coregex.Concat(
          new Coregex.Quantified(
            new Coregex.Concat(
              new Coregex.Union(
                new Coregex.Concat(
                  new Coregex.Literal("25"),
                  new Coregex.Set(Set.builder().range('0', '5').build())
                ),
                new Coregex.Concat(
                  new Coregex.Quantified(
                    new Coregex.Union(
                      new Coregex.Concat(
                        new Coregex.Literal("2"),
                        new Coregex.Set(Set.builder().range('0', '4').build())
                      ),
                      new Coregex.Concat(
                        new Coregex.Quantified(
                          new Coregex.Literal("1"),
                          0,
                          1
                        ),
                        new Coregex.Set(Set.builder().range('0', '9').build())
                      )
                    ),
                    0,
                    1
                  ),
                  new Coregex.Set(Set.builder().range('0', '9').build())
                )
              ),
              new Coregex.Set(Set.builder().single('.').build())
            ),
            3,
            3
          ),
          new Coregex.Union(
            new Coregex.Concat(
              new Coregex.Literal("25"),
              new Coregex.Set(Set.builder().range('0', '5').build())
            ),
            new Coregex.Concat(
              new Coregex.Quantified(
                new Coregex.Union(
                  new Coregex.Concat(
                    new Coregex.Literal("2"),
                    new Coregex.Set(Set.builder().range('0', '4').build())
                  ),
                  new Coregex.Concat(
                    new Coregex.Quantified(
                      new Coregex.Literal("1"),
                      0,
                      1
                    ),
                    new Coregex.Set(Set.builder().range('0', '9').build())
                  )
                ),
                0,
                1
              ),
              new Coregex.Set(Set.builder().range('0', '9').build())
            )
          )
        ) -> Pattern.compile(
          "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"
        ),
        new Coregex.Union(
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                ),
                new Coregex.Literal(":")
              ),
              7,
              7
            ),
            new Coregex.Quantified(
              new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
              1,
              4
            )
          ),
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                ),
                new Coregex.Literal(":")
              ),
              1,
              7
            ),
            new Coregex.Literal(":")
          ),
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                ),
                new Coregex.Literal(":")
              ),
              1,
              6
            ),
            new Coregex.Literal(":"),
            new Coregex.Quantified(
              new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
              1,
              4
            )
          ),
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                ),
                new Coregex.Literal(":")
              ),
              1,
              5
            ),
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Literal(":"),
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                )
              ),
              1,
              2
            )
          ),
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                ),
                new Coregex.Literal(":")
              ),
              1,
              4
            ),
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Literal(":"),
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                )
              ),
              1,
              3
            )
          ),
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                ),
                new Coregex.Literal(":")
              ),
              1,
              3
            ),
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Literal(":"),
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                )
              ),
              1,
              4
            )
          ),
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                ),
                new Coregex.Literal(":")
              ),
              1,
              2
            ),
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Literal(":"),
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                )
              ),
              1,
              5
            )
          ),
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
              1,
              4
            ),
            new Coregex.Literal(":"),
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Literal(":"),
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                )
              ),
              1,
              6
            )
          ),
          new Coregex.Concat(
            new Coregex.Literal(":"),
            new Coregex.Union(
              new Coregex.Quantified(
                new Coregex.Concat(
                  new Coregex.Literal(":"),
                  new Coregex.Quantified(
                    new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                    1,
                    4
                  )
                ),
                1,
                7
              ),
              new Coregex.Literal(":")
            )
          ),
          new Coregex.Concat(
            new Coregex.Literal("fe80:"),
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Literal(":"),
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  0,
                  4
                )
              ),
              0,
              4
            ),
            new Coregex.Literal("%"),
            new Coregex.Quantified(
              new Coregex.Set(Set.builder().range('0', '9').range('a', 'z').range('A', 'Z').build()),
              1
            )
          ),
          new Coregex.Concat(
            new Coregex.Literal("::"),
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Literal("ffff"),
                new Coregex.Quantified(
                  new Coregex.Concat(
                    new Coregex.Literal(":"),
                    new Coregex.Quantified(
                      new Coregex.Literal("0"),
                      1,
                      4
                    )
                  ),
                  0,
                  1
                ),
                new Coregex.Literal(":")
              ),
              0,
              1
            ),
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Union(
                  new Coregex.Concat(
                    new Coregex.Literal("25"),
                    new Coregex.Set(Set.builder().range('0', '5').build())
                  ),
                  new Coregex.Concat(
                    new Coregex.Quantified(
                      new Coregex.Union(
                        new Coregex.Concat(
                          new Coregex.Literal("2"),
                          new Coregex.Set(Set.builder().range('0', '4').build())
                        ),
                        new Coregex.Concat(
                          new Coregex.Quantified(
                            new Coregex.Literal("1"),
                            0,
                            1
                          ),
                          new Coregex.Set(Set.builder().range('0', '9').build())
                        )
                      ),
                      0,
                      1
                    ),
                    new Coregex.Set(Set.builder().range('0', '9').build())
                  )
                ),
                new Coregex.Set(Set.builder().single('.').build())
              ),
              3,
              3
            ),
            new Coregex.Union(
              new Coregex.Concat(
                new Coregex.Literal("25"),
                new Coregex.Set(Set.builder().range('0', '5').build())
              ),
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Union(
                    new Coregex.Concat(
                      new Coregex.Literal("2"),
                      new Coregex.Set(Set.builder().range('0', '4').build())
                    ),
                    new Coregex.Concat(
                      new Coregex.Quantified(
                        new Coregex.Literal("1"),
                        0,
                        1
                      ),
                      new Coregex.Set(Set.builder().range('0', '9').build())
                    )
                  ),
                  0,
                  1
                ),
                new Coregex.Set(Set.builder().range('0', '9').build())
              )
            )
          ),
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').range('A', 'F').build()),
                  1,
                  4
                ),
                new Coregex.Literal(":")
              ),
              1,
              4
            ),
            new Coregex.Literal(":"),
            new Coregex.Quantified(
              new Coregex.Concat(
                new Coregex.Union(
                  new Coregex.Concat(
                    new Coregex.Literal("25"),
                    new Coregex.Set(Set.builder().range('0', '5').build())
                  ),
                  new Coregex.Concat(
                    new Coregex.Quantified(
                      new Coregex.Union(
                        new Coregex.Concat(
                          new Coregex.Literal("2"),
                          new Coregex.Set(Set.builder().range('0', '4').build())
                        ),
                        new Coregex.Concat(
                          new Coregex.Quantified(
                            new Coregex.Literal("1"),
                            0,
                            1
                          ),
                          new Coregex.Set(Set.builder().range('0', '9').build())
                        )
                      ),
                      0,
                      1
                    ),
                    new Coregex.Set(Set.builder().range('0', '9').build())
                  )
                ),
                new Coregex.Set(Set.builder().single('.').build())
              ),
              3,
              3
            ),
            new Coregex.Union(
              new Coregex.Concat(
                new Coregex.Literal("25"),
                new Coregex.Set(Set.builder().range('0', '5').build())
              ),
              new Coregex.Concat(
                new Coregex.Quantified(
                  new Coregex.Union(
                    new Coregex.Concat(
                      new Coregex.Literal("2"),
                      new Coregex.Set(Set.builder().range('0', '4').build())
                    ),
                    new Coregex.Concat(
                      new Coregex.Quantified(
                        new Coregex.Literal("1"),
                        0,
                        1
                      ),
                      new Coregex.Set(Set.builder().range('0', '9').build())
                    )
                  ),
                  0,
                  1
                ),
                new Coregex.Set(Set.builder().range('0', '9').build())
              )
            )
          )
        ) -> Pattern.compile(
          "(\n" +
            "([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|          # 1:2:3:4:5:6:7:8\n" +
            "([0-9a-fA-F]{1,4}:){1,7}:|                         # 1::                              1:2:3:4:5:6:7::\n" +
            "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|         # 1::8             1:2:3:4:5:6::8  1:2:3:4:5:6::8\n" +
            "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|  # 1::7:8           1:2:3:4:5::7:8  1:2:3:4:5::8\n" +
            "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|  # 1::6:7:8         1:2:3:4::6:7:8  1:2:3:4::8\n" +
            "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|  # 1::5:6:7:8       1:2:3::5:6:7:8  1:2:3::8\n" +
            "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|  # 1::4:5:6:7:8     1:2::4:5:6:7:8  1:2::8\n" +
            "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|       # 1::3:4:5:6:7:8   1::3:4:5:6:7:8  1::8  \n" +
            ":((:[0-9a-fA-F]{1,4}){1,7}|:)|                     # ::2:3:4:5:6:7:8  ::2:3:4:5:6:7:8 ::8       ::     \n" +
            "fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|     # fe80::7:8%eth0   fe80::7:8%1     (link-local IPv6 addresses with zone index)\n" +
            "::(ffff(:0{1,4}){0,1}:){0,1}\n" +
            "((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}\n" +
            "(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|          # ::255.255.255.255   ::ffff:255.255.255.255  ::ffff:0:255.255.255.255  (IPv4-mapped IPv6 addresses and IPv4-translated addresses)\n" +
            "([0-9a-fA-F]{1,4}:){1,4}:\n" +
            "((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}\n" +
            "(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])           # 2001:db8:3:4::192.0.2.33  64:ff9b::192.0.2.33 (IPv4-Embedded IPv6 Address)\n" +
            ")",
          Pattern.COMMENTS
        ),
        new Coregex.Concat(
          new Coregex.Literal("arn:"),
          new Coregex.Quantified(
            new Coregex.Set(Set.builder().range('0', '9').range('a', 'z').range('A', 'Z').single('_').build()),
            1
          ),
          new Coregex.Literal(":"),
          new Coregex.Quantified(
            new Coregex.Set(Set.builder().range('0', '9').range('a', 'z').range('A', 'Z').single('_').build()),
            1
          ),
          new Coregex.Literal(":"),
          new Coregex.Quantified(
            new Coregex.Set(
              Set
                .builder()
                .set(Set.builder().range('0', '9').range('a', 'z').range('A', 'Z').single('_').build())
                .single('-')
                .build()
            ),
            1
          ),
          new Coregex.Literal(":"),
          new Coregex.Quantified(
            new Coregex.Set(Set.builder().range('0', '9').build()),
            12,
            12
          ),
          new Coregex.Literal(":"),
          new Coregex.Quantified(
            new Coregex.Concat(
              new Coregex.Quantified(
                new Coregex.Set(
                  Set
                    .builder()
                    .set(Set.builder().range('0', '9').range('a', 'z').range('A', 'Z').single('_').build())
                    .single('-')
                    .build()
                ),
                1
              ),
              new Coregex.Set(Set.builder().single(':').set(Set.builder().single('/').build()).build())
            ),
            0,
            1
          ),
          new Coregex.Quantified(
            new Coregex.Set(
              Set
                .builder()
                .set(Set.builder().range('0', '9').range('a', 'z').range('A', 'Z').single('_').build())
                .set('.', '-')
                .build()
            ),
            1
          )
        ) -> Pattern.compile(
          "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$"
        ),
        new Coregex.Quantified(
          new Coregex.Concat(
            new Coregex.Quantified(
              new Coregex.Set(Set.builder(Pattern.CASE_INSENSITIVE).range('a', 'z').build()),
              1
            ),
            new Coregex.Literal("-"),
            new Coregex.Set(Set.builder().range('A', 'Z').build())
          ),
          3,
          6
        )               -> Pattern.compile("((?i)[a-z]+(?-i)-[A-Z]){3,6}"),
        Coregex.empty() -> Pattern.compile("^(?:||)$")
      )
    rng <- List(new RandomRNG())
  } {
    shouldParseExampleRegex(expected, pattern, rng)
    shouldParseQuotedRegex(pattern, rng)
    shouldParseLiteralRegex(pattern, rng)
  }
}
