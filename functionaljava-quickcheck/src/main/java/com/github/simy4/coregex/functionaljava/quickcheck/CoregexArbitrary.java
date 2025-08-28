/*
 * Copyright 2021-2025 Alex Simkin
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

import com.github.simy4.coregex.core.Coregex;
import fj.P;
import fj.P2;
import fj.data.Stream;
import fj.test.Gen;
import fj.test.Shrink;
import java.util.regex.Pattern;

public final class CoregexArbitrary {

  public static P2<Gen<String>, Shrink<String>> arbitrary(Pattern pattern) {
    return P.p(gen(pattern), shrink(pattern));
  }

  public static Gen<String> gen(Pattern pattern) {
    Coregex coregex = Coregex.from(pattern);
    return Gen.gen(__ -> rand -> coregex.generate(rand.choose(Long.MIN_VALUE, Long.MAX_VALUE)));
  }

  public static Shrink<String> shrink(Pattern pattern) {
    Stream<? extends Coregex> shrinks =
        Stream.iteratorStream(
            com.github.simy4.coregex.core.Coregex.from(pattern).shrink().iterator());
    return Shrink.shrink(
        larger ->
            shrinks
                .map(coregex -> coregex.generate(larger.length()))
                .filter(string -> string.length() < larger.length()));
  }

  private CoregexArbitrary() {
    throw new UnsupportedOperationException("new");
  }
}
