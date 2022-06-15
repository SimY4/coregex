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
    regex = "\\..+";
    Coregex parsed = CoregexParser.getInstance().parse(Pattern.compile(regex));
    assertEquals(regex, parsed.toString());
  }
}