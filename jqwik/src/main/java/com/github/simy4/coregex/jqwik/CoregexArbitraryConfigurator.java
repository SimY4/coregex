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

package com.github.simy4.coregex.jqwik;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.configurators.ArbitraryConfiguratorBase;

import java.util.regex.Pattern;

public class CoregexArbitraryConfigurator extends ArbitraryConfiguratorBase {
  public Arbitrary<String> configure(Arbitrary<String> arbitrary, Regex regex) {
    return Arbitraries.fromGenerator(new CoregexGenerator(Pattern.compile(regex.value())));
  }
}
