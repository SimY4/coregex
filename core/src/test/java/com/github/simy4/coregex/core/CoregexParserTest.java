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

package com.github.simy4.coregex.core;

import com.github.simy4.coregex.core.generators.CoregexGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

@RunWith(JUnitQuickcheck.class)
public class CoregexParserTest {
  @Test
  public void shouldParseUUIDRegex() {
    assertEquals(
        new Coregex.Concat(
            new Coregex.Quantified(
                new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 8, 8, true),
            new Coregex.Literal("-"),
            new Coregex.Quantified(
                new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 4, 4, true),
            new Coregex.Literal("-"),
            new Coregex.Set(Set.builder().range('0', '5').build()),
            new Coregex.Quantified(
                new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 3, 3, true),
            new Coregex.Literal("-"),
            new Coregex.Set(Set.builder().set('0', '8', '9', 'a', 'b').build()),
            new Coregex.Quantified(
                new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()), 3, 3, true),
            new Coregex.Literal("-"),
            new Coregex.Quantified(
                new Coregex.Set(Set.builder().range('0', '9').range('a', 'f').build()),
                12,
                12,
                true)),
        CoregexParser.getInstance()
            .parse(
                Pattern.compile(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}")));
  }

  @Test
  public void shouldParseURLRegex() {
    assertEquals(
        new Coregex.Concat(
            new Coregex.Literal(""),
            new Coregex.Union(
                new Coregex.Concat(
                    new Coregex.Literal("http"),
                    new Coregex.Quantified(new Coregex.Literal("s"), 0, 1, true)),
                new Coregex.Literal("ftp"),
                new Coregex.Literal("file")),
            new Coregex.Literal("://"),
            new Coregex.Quantified(
                new Coregex.Set(
                    Set.builder()
                        .single('-')
                        .range('a', 'z')
                        .range('A', 'Z')
                        .range('0', '9')
                        .set(
                            '+', '&', '@', '#', '/', '%', '?', '=', '~', '_', '|', '!', ':', ',',
                            '.', ';')
                        .build()),
                0,
                -1,
                true),
            new Coregex.Set(
                Set.builder()
                    .single('-')
                    .range('a', 'z')
                    .range('A', 'Z')
                    .range('0', '9')
                    .set('+', '&', '@', '#', '/', '%', '=', '~', '_', '|')
                    .build())),
        CoregexParser.getInstance()
            .parse(
                Pattern.compile(
                    "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")));
  }

  @Property
  @Ignore
  public void shouldParse(@From(CoregexGenerator.class) Coregex coregex) {
    String regex = coregex.toString();
    Coregex parsed = CoregexParser.getInstance().parse(Pattern.compile(regex));
    assertEquals(regex, parsed.toString());
  }
}
