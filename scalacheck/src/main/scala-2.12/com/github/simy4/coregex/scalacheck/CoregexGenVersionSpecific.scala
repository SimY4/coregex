package com.github.simy4.coregex
package scalacheck

import core.Coregex
import org.scalacheck.Shrink

private[scalacheck] trait CoregexGenVersionSpecific {
  import scala.compat.java8.StreamConverters._

  def shrinkFor(coregex: Coregex): Shrink[String] = {
    val shrinks = coregex.shrink().toScala[Stream]
    Shrink { larger =>
      shrinks.map(coregex => coregex.generate(larger.length.toLong)).filter(_.length < larger.length)
    }
  }
}
