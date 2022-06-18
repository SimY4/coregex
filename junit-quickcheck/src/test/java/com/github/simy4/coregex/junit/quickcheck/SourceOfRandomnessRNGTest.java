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

package com.github.simy4.coregex.junit.quickcheck;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitQuickcheck.class)
public class SourceOfRandomnessRNGTest {

  @Property
  public void shouldGenerateIntInRange(int i1, int i2, long seed) {
    int start = Math.min(i1, i2);
    int end = Math.max(i1, i2);
    SourceOfRandomness sourceOfRandomness = new SourceOfRandomness(new Random(seed));
    int generated = new SourceOfRandomnessRNG(sourceOfRandomness).genInteger(start, end).getValue();
    assertTrue(start + " <= " + generated + " <= " + end, start <= generated && generated <= end);
  }

  @Property
  public void shouldGenerateIntWhenStartAndEndAreTheSame(int startAndEnd, long seed) {
    SourceOfRandomness sourceOfRandomness = new SourceOfRandomness(new Random(seed));
    int generated =
        new SourceOfRandomnessRNG(sourceOfRandomness)
            .genInteger(startAndEnd, startAndEnd)
            .getValue();
    assertEquals(startAndEnd, generated);
  }
}
