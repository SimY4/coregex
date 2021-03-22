package com.github.simy4.coregex.core;

import java.util.Map;

public interface RNG {
  Map.Entry<RNG, Integer> genInteger(int startInc, int endInc);

  Map.Entry<RNG, Long> genLong();
}
