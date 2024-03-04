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

import munit.{ Location, ScalaCheckSuite }
import rng.RandomRNG

import java.util.regex.Pattern
import scala.util.control.NonFatal

class CoregexParserSuite extends ScalaCheckSuite with CoregexArbitraries {
  @inline private def set(flags: Int = 0)(set: Set.Builder => Set.Builder): Coregex.Set =
    new Coregex.Set(set(Set.builder(flags)).build())
  @inline implicit private def stringToLiteral(str: String): Coregex.Literal = new Coregex.Literal(str)

  import Coregex._

  def shouldParseExampleRegex(expected: Coregex, pattern: Pattern, rng: RNG)(implicit loc: Location): Unit =
    test(s"should parse example regex: ${pattern.pattern()}") {
      val actual    = Coregex.from(pattern)
      val generated = actual.generate(rng)

      assertEquals(actual, expected)
      assert(
        clue(pattern).matcher(clue(generated)).matches(),
        s"${pattern.pattern()} should match generated: $generated"
      )
    }

  def shouldParseQuotedRegex(pattern: Pattern, rng: RNG)(implicit loc: Location): Unit =
    test(s"should parse quoted regex: ${pattern.pattern()}") {
      val expected  = Pattern.compile(Pattern.quote(pattern.pattern()))
      val actual    = Coregex.from(expected)
      val generated = actual.generate(rng)
      assert(
        clue(expected).matcher(clue(generated)).matches(),
        s"${expected.pattern()} should match generated: $generated"
      )
    }

  def shouldParseLiteralRegex(pattern: Pattern, rng: RNG)(implicit loc: Location): Unit =
    test(s"should parse literal regex: ${pattern.pattern()}") {
      val expected  = Pattern.compile(pattern.pattern(), Pattern.LITERAL)
      val actual    = Coregex.from(expected)
      val generated = actual.generate(rng)
      assert(
        clue(expected).matcher(clue(generated)).matches(),
        s"${expected.pattern()} should match generated: $generated"
      )
    }

  def shouldThrowUnsupported(pattern: Pattern)(implicit loc: Location): Unit =
    test(s"should throw unsupported: ${pattern.pattern()}") {
      try {
        Coregex.from(pattern)
        fail(s"should throw error for: ${pattern.pattern()}")
      } catch {
        case _: UnsupportedOperationException =>
        case NonFatal(ex) => fail(s"should throw UnsupportedOperationException for: ${pattern.pattern()}", ex)
      }
    }

  for {
    (expected, pattern) <-
      List(
        new Concat(
          new Quantified(set()(_.range('0', '9').range('a', 'f')), 8, 8),
          "-",
          new Quantified(set()(_.range('0', '9').range('a', 'f')), 4, 4),
          "-",
          set()(_.range('0', '5')),
          new Quantified(set()(_.range('0', '9').range('a', 'f')), 3, 3),
          "-",
          set()(_.set('0', '8', '9', 'a', 'b')),
          new Quantified(set()(_.range('0', '9').range('a', 'f')), 3, 3),
          "-",
          new Quantified(set()(_.range('0', '9').range('a', 'f')), 12, 12)
        ) ->
          Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}"),
        new Concat(
          new Group(
            1,
            new Union(
              new Concat("http", new Quantified("s", 0, 1)),
              "ftp",
              "file"
            )
          ),
          "://",
          new Quantified(
            set() {
              _.single('-')
                .range('a', 'z')
                .range('A', 'Z')
                .range('0', '9')
                .set('+', '&', '@', '#', '/', '%', '?', '=', '~', '_', '|', '!', ':', ',', '.', ';')
            },
            0
          ),
          set() {
            _.single('-')
              .range('a', 'z')
              .range('A', 'Z')
              .range('0', '9')
              .set('+', '&', '@', '#', '/', '%', '=', '~', '_', '|')
          }
        ) ->
          Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"),
        new Concat(
          new Quantified(set()(_.range('0', '9')), 4, 4),
          "-",
          set()(_.set('0', '1')),
          set()(_.range('0', '9')),
          "-",
          set()(_.range('0', '3')),
          set()(_.range('0', '9')),
          "T",
          set()(_.range('0', '2')),
          set()(_.range('0', '9')),
          ":",
          set()(_.range('0', '5')),
          set()(_.range('0', '9')),
          ":",
          set()(_.range('0', '5')),
          set()(_.range('0', '9')),
          new Group(
            1,
            new Union(
              new Concat(
                set()(_.set('+', '-')),
                set()(_.range('0', '2')),
                set()(_.range('0', '9')),
                ":",
                set()(_.range('0', '5')),
                set()(_.range('0', '9'))
              ),
              "Z"
            )
          )
        ) ->
          Pattern.compile("\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)"),
        new Concat(
          new Quantified(
            new Group(
              1,
              new Concat(
                new Group(
                  2,
                  new Union(
                    new Concat(
                      "25",
                      set()(_.range('0', '5'))
                    ),
                    new Concat(
                      new Quantified(
                        new Group(
                          3,
                          new Union(
                            new Concat(
                              "2",
                              set()(_.range('0', '4'))
                            ),
                            new Concat(
                              new Quantified(
                                "1",
                                0,
                                1
                              ),
                              set()(_.range('0', '9'))
                            )
                          )
                        ),
                        0,
                        1
                      ),
                      set()(_.range('0', '9'))
                    )
                  )
                ),
                set()(_.single('.'))
              )
            ),
            3,
            3
          ),
          new Group(
            4,
            new Union(
              new Concat(
                "25",
                set()(_.range('0', '5'))
              ),
              new Concat(
                new Quantified(
                  new Group(
                    5,
                    new Union(
                      new Concat(
                        "2",
                        set()(_.range('0', '4'))
                      ),
                      new Concat(
                        new Quantified(
                          "1",
                          0,
                          1
                        ),
                        set()(_.range('0', '9'))
                      )
                    )
                  ),
                  0,
                  1
                ),
                set()(_.range('0', '9'))
              )
            )
          )
        ) -> Pattern.compile(
          "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"
        ),
        new Group(
          1,
          new Union(
            new Concat(
              new Quantified(
                new Group(
                  2,
                  new Concat(
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    ),
                    ":"
                  )
                ),
                7,
                7
              ),
              new Quantified(
                set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                1,
                4
              )
            ),
            new Concat(
              new Quantified(
                new Group(
                  3,
                  new Concat(
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    ),
                    ":"
                  )
                ),
                1,
                7
              ),
              ":"
            ),
            new Concat(
              new Quantified(
                new Group(
                  4,
                  new Concat(
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    ),
                    ":"
                  )
                ),
                1,
                6
              ),
              ":",
              new Quantified(
                set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                1,
                4
              )
            ),
            new Concat(
              new Quantified(
                new Group(
                  5,
                  new Concat(
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    ),
                    ":"
                  )
                ),
                1,
                5
              ),
              new Quantified(
                new Group(
                  6,
                  new Concat(
                    ":",
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    )
                  )
                ),
                1,
                2
              )
            ),
            new Concat(
              new Quantified(
                new Group(
                  7,
                  new Concat(
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    ),
                    ":"
                  )
                ),
                1,
                4
              ),
              new Quantified(
                new Group(
                  8,
                  new Concat(
                    ":",
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    )
                  )
                ),
                1,
                3
              )
            ),
            new Concat(
              new Quantified(
                new Group(
                  9,
                  new Concat(
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    ),
                    ":"
                  )
                ),
                1,
                3
              ),
              new Quantified(
                new Group(
                  10,
                  new Concat(
                    ":",
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    )
                  )
                ),
                1,
                4
              )
            ),
            new Concat(
              new Quantified(
                new Group(
                  11,
                  new Concat(
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    ),
                    ":"
                  )
                ),
                1,
                2
              ),
              new Quantified(
                new Group(
                  12,
                  new Concat(
                    ":",
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    )
                  )
                ),
                1,
                5
              )
            ),
            new Concat(
              new Quantified(
                set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                1,
                4
              ),
              ":",
              new Group(
                13,
                new Quantified(
                  new Group(
                    14,
                    new Concat(
                      ":",
                      new Quantified(
                        set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                        1,
                        4
                      )
                    )
                  ),
                  1,
                  6
                )
              )
            ),
            new Concat(
              ":",
              new Group(
                15,
                new Union(
                  new Quantified(
                    new Group(
                      16,
                      new Concat(
                        ":",
                        new Quantified(
                          set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                          1,
                          4
                        )
                      )
                    ),
                    1,
                    7
                  ),
                  ":"
                )
              )
            ),
            new Concat(
              "fe80:",
              new Quantified(
                new Group(
                  17,
                  new Concat(
                    ":",
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      0,
                      4
                    )
                  )
                ),
                0,
                4
              ),
              "%",
              new Quantified(
                set()(_.range('0', '9').range('a', 'z').range('A', 'Z')),
                1
              )
            ),
            new Concat(
              "::",
              new Quantified(
                new Group(
                  18,
                  new Concat(
                    "ffff",
                    new Quantified(
                      new Group(
                        19,
                        new Concat(
                          ":",
                          new Quantified(
                            "0",
                            1,
                            4
                          )
                        )
                      ),
                      0,
                      1
                    ),
                    ":"
                  )
                ),
                0,
                1
              ),
              new Quantified(
                new Group(
                  20,
                  new Concat(
                    new Group(
                      21,
                      new Union(
                        new Concat(
                          "25",
                          set()(_.range('0', '5'))
                        ),
                        new Concat(
                          new Quantified(
                            new Group(
                              22,
                              new Union(
                                new Concat(
                                  "2",
                                  set()(_.range('0', '4'))
                                ),
                                new Concat(
                                  new Quantified(
                                    "1",
                                    0,
                                    1
                                  ),
                                  set()(_.range('0', '9'))
                                )
                              )
                            ),
                            0,
                            1
                          ),
                          set()(_.range('0', '9'))
                        )
                      )
                    ),
                    set()(_.single('.'))
                  )
                ),
                3,
                3
              ),
              new Group(
                23,
                new Union(
                  new Concat(
                    "25",
                    set()(_.range('0', '5'))
                  ),
                  new Concat(
                    new Quantified(
                      new Group(
                        24,
                        new Union(
                          new Concat(
                            "2",
                            set()(_.range('0', '4'))
                          ),
                          new Concat(
                            new Quantified(
                              "1",
                              0,
                              1
                            ),
                            set()(_.range('0', '9'))
                          )
                        )
                      ),
                      0,
                      1
                    ),
                    set()(_.range('0', '9'))
                  )
                )
              )
            ),
            new Concat(
              new Quantified(
                new Group(
                  25,
                  new Concat(
                    new Quantified(
                      set()(_.range('0', '9').range('a', 'f').range('A', 'F')),
                      1,
                      4
                    ),
                    ":"
                  )
                ),
                1,
                4
              ),
              ":",
              new Quantified(
                new Group(
                  26,
                  new Concat(
                    new Group(
                      27,
                      new Union(
                        new Concat(
                          "25",
                          set()(_.range('0', '5'))
                        ),
                        new Concat(
                          new Quantified(
                            new Group(
                              28,
                              new Union(
                                new Concat(
                                  "2",
                                  set()(_.range('0', '4'))
                                ),
                                new Concat(
                                  new Quantified(
                                    "1",
                                    0,
                                    1
                                  ),
                                  set()(_.range('0', '9'))
                                )
                              )
                            ),
                            0,
                            1
                          ),
                          set()(_.range('0', '9'))
                        )
                      )
                    ),
                    set()(_.single('.'))
                  )
                ),
                3,
                3
              ),
              new Group(
                29,
                new Union(
                  new Concat(
                    "25",
                    set()(_.range('0', '5'))
                  ),
                  new Concat(
                    new Quantified(
                      new Group(
                        30,
                        new Union(
                          new Concat(
                            "2",
                            set()(_.range('0', '4'))
                          ),
                          new Concat(
                            new Quantified(
                              "1",
                              0,
                              1
                            ),
                            set()(_.range('0', '9'))
                          )
                        )
                      ),
                      0,
                      1
                    ),
                    set()(_.range('0', '9'))
                  )
                )
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
//(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])[.]){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])[.]){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))
//(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|[0-9a-fA-F]{1,4}:{1,6}:[0-9a-fA-F]{1,4}|[0-9a-fA-F]{1,4}:{1,5}:[0-9a-fA-F]{1,4}{1,2}|[0-9a-fA-F]{1,4}:{1,4}:[0-9a-fA-F]{1,4}{1,3}|[0-9a-fA-F]{1,4}:{1,3}:[0-9a-fA-F]{1,4}{1,4}|[0-9a-fA-F]{1,4}:{1,2}:[0-9a-fA-F]{1,4}{1,5}|[0-9a-fA-F]{1,4}::[0-9a-fA-F]{1,4}{1,6}|::[0-9a-fA-F]{1,4}{1,7}|:|fe80::[0-9a-fA-F]{0,4}{0,4}%[0-9a-zA-Z]+|::ffff:0{1,4}?:?25[0-5]|2[0-4]|1?[0-9]?[0-9][.]{3}25[0-5]|2[0-4]|1?[0-9]?[0-9]|[0-9a-fA-F]{1,4}:{1,4}:25[0-5]|2[0-4]|1?[0-9]?[0-9][.]{3}25[0-5]|2[0-4]|1?[0-9]?[0-9])
        new Concat(
          new Group(
            new Union(
              new Concat(
                new Quantified(
                  set() {
                    _.range('a', 'z')
                      .range('0', '9')
                      .set('!', '#', '$', '%', '&', '\'', '*', '+', '/', '=', '?', '^', '_', '`', '{', '|', '}', '~',
                        '-')
                  },
                  1
                ),
                new Quantified(
                  new Group(
                    new Concat(
                      set()(_.single('.')),
                      new Quantified(
                        set() {
                          _.range('a', 'z')
                            .range('0', '9')
                            .set('!', '#', '$', '%', '&', '\'', '*', '+', '/', '=', '?', '^', '_', '`', '{', '|', '}',
                              '~', '-')
                        },
                        1
                      )
                    )
                  ),
                  0
                )
              ),
              new Concat(
                "\"",
                new Quantified(
                  new Group(
                    new Union(
                      set() {
                        _.range(0x01, 0x08)
                          .set(0x0b, 0x0c)
                          .range(0x0e, 0x1f)
                          .single(0x21)
                          .range(0x23, 0x5b)
                          .range(0x5e, 0x7f)
                      },
                      new Concat(
                        set()(_.single('\\')),
                        set()(_.range(0x01, 0x09).set(0x0b, 0x0c).range(0x0e, 0x7f))
                      )
                    )
                  ),
                  0
                ),
                "\""
              )
            )
          ),
          "@",
          new Group(
            new Union(
              new Concat(
                new Quantified(
                  new Group(
                    new Concat(
                      set()(_.range('a', 'z').range('0', '9')),
                      new Quantified(
                        new Group(
                          new Concat(
                            new Quantified(
                              set()(_.range('a', 'z').range('0', '9').single('-')),
                              0
                            ),
                            set()(_.range('a', 'z').range('0', '9'))
                          )
                        ),
                        0,
                        1
                      ),
                      set()(_.single('.'))
                    )
                  ),
                  1
                ),
                set()(_.range('a', 'z').range('0', '9')),
                new Quantified(
                  new Group(
                    new Concat(
                      new Quantified(
                        set()(_.range('a', 'z').range('0', '9').single('-')),
                        0
                      ),
                      set()(_.range('a', 'z').range('0', '9'))
                    )
                  ),
                  0,
                  1
                )
              ),
              new Concat(
                set()(_.single('[')),
                new Quantified(
                  new Group(
                    new Concat(
                      new Group(
                        1,
                        new Union(
                          new Concat(
                            "2",
                            new Group(
                              2,
                              new Union(
                                new Concat(
                                  "5",
                                  set()(_.range('0', '5'))
                                ),
                                new Concat(
                                  set()(_.range('0', '4')),
                                  set()(_.range('0', '9'))
                                )
                              )
                            )
                          ),
                          new Concat(
                            "1",
                            set()(_.range('0', '9')),
                            set()(_.range('0', '9'))
                          ),
                          new Concat(
                            new Quantified(
                              set()(_.range('1', '9')),
                              0,
                              1
                            ),
                            set()(_.range('0', '9'))
                          )
                        )
                      ),
                      set()(_.single('.'))
                    )
                  ),
                  3,
                  3
                ),
                new Group(
                  new Union(
                    new Group(
                      3,
                      new Union(
                        new Concat(
                          "2",
                          new Group(
                            4,
                            new Union(
                              new Concat(
                                "5",
                                set()(_.range('0', '5'))
                              ),
                              new Concat(
                                set()(_.range('0', '4')),
                                set()(_.range('0', '9'))
                              )
                            )
                          )
                        ),
                        new Concat(
                          "1",
                          set()(_.range('0', '9')),
                          set()(_.range('0', '9'))
                        ),
                        new Concat(
                          new Quantified(
                            set()(_.range('1', '9')),
                            0,
                            1
                          ),
                          set()(_.range('0', '9'))
                        )
                      )
                    ),
                    new Concat(
                      new Quantified(
                        set()(_.range('a', 'z').range('0', '9').single('-')),
                        0
                      ),
                      set()(_.range('a', 'z').range('0', '9')),
                      ":",
                      new Quantified(
                        new Group(
                          new Union(
                            set() {
                              _.range(0x01, 0x08)
                                .set(0x0b, 0x0c)
                                .range(0x0e, 0x1f)
                                .range(0x21, 0x5a)
                                .range(0x53, 0x7f)
                            },
                            new Concat(
                              set()(_.single('\\')),
                              set()(_.range(0x01, 0x09).set(0x0b, 0x0c).range(0x0e, 0x7f))
                            )
                          )
                        ),
                        1
                      )
                    )
                  )
                ),
                "]"
              )
            )
          )
        ) -> Pattern.compile(
          "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5e-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])"
        ),
        new Concat(
          "arn:",
          new Group(
            1,
            "partition",
            new Quantified(
              set()(_.range('0', '9').range('a', 'z').range('A', 'Z').single('_')),
              1
            )
          ),
          ":",
          new Group(
            2,
            "service",
            new Quantified(
              set()(_.range('0', '9').range('a', 'z').range('A', 'Z').single('_')),
              1
            )
          ),
          ":",
          new Group(
            3,
            "region",
            new Quantified(
              set() {
                _.set(set()(_.range('0', '9').range('a', 'z').range('A', 'Z').single('_')).set())
                  .single('-')
              },
              1
            )
          ),
          ":",
          new Group(
            4,
            "accountID",
            new Quantified(
              set()(_.range('0', '9')),
              12,
              12
            )
          ),
          ":",
          new Quantified(
            new Group(
              5,
              "ignore",
              new Concat(
                new Group(
                  6,
                  "resourceType",
                  new Quantified(
                    set() {
                      _.set(set()(_.range('0', '9').range('a', 'z').range('A', 'Z').single('_')).set())
                        .single('-')
                    },
                    1
                  )
                ),
                set()(_.single(':').set(set()(_.single('/')).set()))
              )
            ),
            0,
            1
          ),
          new Group(
            7,
            "resource",
            new Quantified(
              set() {
                _.set(set()(_.range('0', '9').range('a', 'z').range('A', 'Z').single('_')).set())
                  .set('.', '-')
              },
              1
            )
          )
        ) -> Pattern.compile(
          "^arn:(?<partition>\\w+):(?<service>\\w+):(?<region>[\\w-]+):(?<accountID>\\d{12}):(?<ignore>(?<resourceType>[\\w-]+)[:\\/])?(?<resource>[\\w.-]+)$"
        ),
        new Quantified(
          new Group(
            1,
            new Concat(
              new Quantified(
                set(Pattern.CASE_INSENSITIVE)(_.range('a', 'z')),
                1
              ),
              "-",
              set()(_.range('A', 'Z'))
            )
          ),
          3,
          6
        )       -> Pattern.compile("((?i)[a-z]+(?-i)-[A-Z]){3,6}"),
        empty() -> Pattern.compile("^(?:||)$")
      )
    rng <- List(new RandomRNG())
  } {
    shouldParseExampleRegex(expected, pattern, rng)
    shouldParseQuotedRegex(pattern, rng)
    shouldParseLiteralRegex(pattern, rng)
  }

  for {
    pattern <- List(
      Pattern.compile("(?!.{255,}).+"),
      Pattern.compile("(?=[a-z]+).+"),
      Pattern.compile(".+(?<=[a-z]+)"),
      Pattern.compile(".+(?<!.{255,})")
    )
  } {
    shouldThrowUnsupported(pattern)
  }
}
