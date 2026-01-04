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

package com.github.simy4.coregex.functionaljava.quickcheck;

import static fj.test.Property.prop;

import fj.Equal;
import fj.F;
import fj.Ord;
import fj.P2;
import fj.Try;
import fj.data.List;
import fj.test.Arbitrary;
import fj.test.Gen;
import fj.test.Property;
import fj.test.Shrink;
import fj.test.runner.PropertyTestRunner;
import java.net.InetAddress;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.runner.RunWith;

@RunWith(PropertyTestRunner.class)
public class CoregexArbitraryTest {
  private static final Equal<String> stringEqual = Equal.stringEqual;
  private static final Ord<Integer> intOrd = Ord.intOrd;

  private static <T> Property property(P2<Gen<T>, Shrink<T>> arbitrary, F<T, Property> property) {
    return Property.property(arbitrary._1(), arbitrary._2(), property);
  }

  public Property shouldGenerateMatchingUUIDString() {
    return property(
        CoregexArbitrary.arbitrary(
            Pattern.compile(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}")),
        uuid -> prop(stringEqual.eq(uuid, UUID.fromString(uuid).toString())));
  }

  public Property shouldGenerateMatchingIPv4String() {
    return property(
        CoregexArbitrary.arbitrary(
            Pattern.compile(
                "((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])")),
        ipv4 -> {
          String[] expected = ipv4.split("\\.");
          String[] actual =
              Try.io(() -> InetAddress.getByName(ipv4).getHostAddress().split("\\."))
                  .safe()
                  .run()
                  .success();
          return prop(expected.length == actual.length)
              .and(
                  List.arrayList(expected)
                      .zip(List.arrayList(actual))
                      .foldLeft(
                          (acc, p) ->
                              acc.and(prop(Integer.parseInt(p._1()) == Integer.parseInt(p._2()))),
                          prop(true)));
        });
  }

  public Property shouldGenerateMatchingIsoDateString() {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
    return property(
        CoregexArbitrary.arbitrary(
            Pattern.compile(
                "[12]\\d{3}-(?:0[1-9]|1[012])-(?:0[1-9]|1\\d|2[0-8])T(?:1\\d|2[0-3]):[0-5]\\d:[0-5]\\d(\\.\\d{2}[1-9])?Z")),
        iso8601Date ->
            prop(stringEqual.eq(iso8601Date, formatter.format(formatter.parse(iso8601Date)))));
  }

  public Property shouldGenerateUniqueStrings() {
    return property(
        CoregexArbitrary.arbitrary(Pattern.compile("[a-zA-Z0-9]{32,}"))
            .split(Arbitrary::arbList, Shrink::shrinkList),
        strings ->
            strings.foldLeft(
                (acc, s) ->
                    acc.and(prop(intOrd.isLessThanOrEqualTo(32, s.length())))
                        .and(prop(s.chars().allMatch(Character::isLetterOrDigit))),
                prop(true)));
  }
}
