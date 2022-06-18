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
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

@RunWith(JUnitQuickcheck.class)
public class CoregexParserTest {
  @Property
  public void shouldParse(@From(CoregexGenerator.class) Coregex coregex) {
    String regex = coregex.toString();
    Coregex parsed = CoregexParser.getInstance().parse(Pattern.compile(regex));
    assertEquals(regex, parsed.toString());
  }
}
