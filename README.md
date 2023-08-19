# coregex
[![Build Status](https://github.com/SimY4/coregex/workflows/Build%20and%20Test/badge.svg)](https://github.com/SimY4/coregex/actions?query=workflow%3A"Build+and+Test")
[![codecov](https://codecov.io/gh/SimY4/coregex/branch/main/graph/badge.svg)](https://codecov.io/gh/SimY4/coregex)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

[![Maven Central](https://img.shields.io/maven-central/v/com.github.simy4.coregex/coregex-core.svg)](https://search.maven.org/search?q=g:com.github.simy4.coregex)
[![Javadocs](http://www.javadoc.io/badge/com.github.simy4.coregex/coregex-core.svg)](http://www.javadoc.io/doc/com.github.simy4.coregex/coregex-core)

A handy utility for generating strings that match given regular expression criteria.

# Supported generators

- [Jqwik](https://jqwik.net/) 
- [JUnit Quickcheck](https://pholser.github.io/junit-quickcheck)
- [Kotest](https://kotest.io/)
- [scalacheck](https://scalacheck.org/)
- [vavr-test](https://github.com/vavr-io/vavr-test)

# Usage
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
  property("my property") = forAll { (str: String Matching "[a-zA-Z]{3}") =>
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