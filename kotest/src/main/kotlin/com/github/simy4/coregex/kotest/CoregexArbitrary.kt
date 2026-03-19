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

import com.github.simy4.coregex.core.Coregex
import io.kotest.property.Arb
import io.kotest.property.Classifier
import io.kotest.property.RTree
import io.kotest.property.RandomSource
import io.kotest.property.Sample
import java.util.regex.Pattern
import kotlin.streams.toList

public class CoregexArbitrary(private val coregex: Coregex): Arb<String>() {

  public companion object {
    public fun of(pattern: String, flags: Int = 0): CoregexArbitrary =
      CoregexArbitrary(Pattern.compile(pattern, flags))
  }

  public constructor(pattern: Pattern): this(Coregex.from(pattern))

  public constructor(regex: Regex): this(regex.toPattern())

  override val classifier: Classifier<out String> = Classifier { string -> "'$string' matching '$coregex'" }

  override fun edgecase(rs: RandomSource): Sample<String>? = null

  override fun sample(rs: RandomSource): Sample<String> {
    val seed = rs.random.nextLong()
    val sample = coregex.generate(seed)
    return Sample(sample, RTree({ sample }, lazy(CoregexShrinker(coregex, seed))))
  }
}

internal class CoregexShrinker(private val coregex: Coregex, private val seed: Long): () -> List<RTree<String>> {
  override fun invoke(): List<RTree<String>> =
    coregex
      .shrink()
      .map { coregex ->
        val shrink = coregex.generate(seed)
        RTree( { shrink }, lazy(CoregexShrinker(coregex, seed)))
      }
      .toList()
}
