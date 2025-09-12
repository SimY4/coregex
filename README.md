# coregex
[![Build Status](https://github.com/SimY4/coregex/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/SimY4/coregex/actions?query=workflow%3A"Build+and+Test")
[![codecov](https://codecov.io/gh/SimY4/coregex/branch/main/graph/badge.svg)](https://codecov.io/gh/SimY4/coregex)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

[![Maven Central](https://img.shields.io/maven-central/v/com.github.simy4.coregex/coregex-core.svg)](https://search.maven.org/search?q=g:com.github.simy4.coregex)
[![Javadocs](http://www.javadoc.io/badge/com.github.simy4.coregex/coregex-core.svg)](http://www.javadoc.io/doc/com.github.simy4.coregex/coregex-core)

A handy utility for generating strings that match given regular expression criteria.

# Supported generators

- [functionaljava-quickcheck](https://github.com/functionaljava/functionaljava) 
- [Jqwik](https://jqwik.net/) 
- [JUnit Quickcheck](https://pholser.github.io/junit-quickcheck)
- [Kotest](https://kotest.io/)
- [scalacheck](https://scalacheck.org/)
- [vavr-test](https://github.com/vavr-io/vavr-test)

# Usage

You can use this library directly by compiling an instance of coregex from any given regular expression pattern:

```java
var pattern = Pattern.compile("[a-zA-Z]{3}");

var coregex = Coregex.from(pattern);
```

Having an instance of coregex, you can ask it to generate a string matching original regular expression. At any given time
this should be true:

```java
var seed = ThreadLocalRandom.current().nextLong();

assert pattern.matcher(coregex.generate(seed)).matches();
```

Given the library is primarily intended to be used in property based testing, it comes with a set of integrations for popular
property based testing frameworks.

## Functionaljava Quickcheck
Include the following dependency into your project:

```groovy
testImplementation "com.github.simy4.coregex:coregex-functionaljava-quickcheck"
```

Use the provided `CoregexArbirary` class to generate a string that would match the regular expression predicate:

```java
@RunWith(PropertyTestRunner.class)
public class MyTest {
  private static final Pattern PATTERN = Pattern.compile("[a-zA-Z]{3}");

  public Property myProperty() {
    return property(CoregexArbitrary.gen(PATTERN), CoregexArbitrary.shrink(PATTERN), str -> prop(3 == str.length()));
  }
}
```

## Jqwik
Include the following dependency into your project:

```groovy
testImplementation "com.github.simy4.coregex:coregex-jqwik"
```

Use the provided `Regex` annotation to generate a string that would match the regular expression predicate:

```java
class MyTest {
  @Property
  void myProperty(@ForAll @Regex("[a-zA-Z]{3}") String str) {
    assertThat(str).hasLength(3);
  }
}
```

## JUnit Quickcheck
Include the following dependency into your project:

```groovy
testImplementation "com.github.simy4.coregex:coregex-junit-quickcheck"
```

Use the provided `Regex` annotation to generate a string that would match the regular expression predicate:

```java
@RunWith(JUnitQuickcheck.class)
public class MyTest {
  @Property
  public void myProperty(@Regex("[a-zA-Z]{3}") String str) {
    assertThat(str).hasLength(3);
  }
}
```

## hedgehog-scala
Include the following dependency into your project:

```scala
libraryDependencies ++= Seq("com.github.simy4.coregex" %% "coregex-hedgehog" % Test)
```

Use the provided `CoregexGen` class to generate a string that would match the regular expression predicate:

```scala
object MySpec extends Properties {
  def tests: List[Test] = List(
    property("my property", myProperty),
  )

  def myProperty: Property = for {
    uuid <- CoregexGen.fromRegex("[a-zA-Z]{3}".r).forAll
  } yield Result.assert(str.length ==== 3)
}
```

## Kotest
Include the following dependency into your project:

```groovy
testImplementation "com.github.simy4.coregex:coregex-kotest"
```

Use the provided `CoregexArbirary` class to generate a string that would match the regular expression predicate:

```kotlin
class MyTest : DescribeSpec({
  describe("my property") {
    it("should hold") {
      checkAll(CoregexArbitrary.of("[a-zA-Z]{3}")) { str ->
        str.length shouldBe 3
      }
    }
  }
})
```

## scalacheck
Include the following dependency into your project:

```scala
libraryDependencies ++= Seq("com.github.simy4.coregex" %% "coregex-scalacheck" % Test)
```

Use the provided `CoregexInstances` trait to constrain string arbitraries:

```scala
object MySpecification extends Properties("MySpecification") with CoregexInstances {
  property("my property") = forAll { (str: StringMatching["[a-zA-Z]{3}"]) =>
    3 == str.length  
  }
}
```

## vavr test
Include the following dependency into your project:

```groovy
testImplementation "com.github.simy4.coregex:coregex-vavr-test"
```

Use the provided `CoregexArbirary` class to generate a string that would match the regular expression predicate:

```java
class MyTest {
  @Test
  void myProperty() {
    Property.def("my property")
        .forAll(CoregexArbitrary.of("[a-zA-Z]{3}"))
        .suchThat(str -> 3 == str.length())
        .check();
  }
}
```

## ZIO test
Include the following dependency into your project:

```groovy
testImplementation "com.github.simy4.coregex:coregex-zio-test"
```

Use the provided `CoregexGen` class to generate a string that would match the regular expression predicate:

```scala
object MySpec extends ZIOSpecDefault {
  def spec = suite("my spec")(
    test("my property") {
      check(CoregexGen.from(Pattern.compile("[a-zA-Z]{3}"))) { str => 
        assertTrue(str.length == 3)
      }
    }
  )
}
```
