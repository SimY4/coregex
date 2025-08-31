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

package com.github.simy4.coregex.core;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Data representation of regex language.
 *
 * <p><em>Effectively sealed.</em>
 *
 * @see Coregex.Concat
 * @see Coregex.Group
 * @see Coregex.Quantified
 * @see Coregex.Ref
 * @see Set
 * @see Coregex.Union
 * @author Alex Simkin
 * @since 0.1.0
 */
public abstract class Coregex implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Lazy<Coregex> EMPTY =
      new Lazy<>(() -> Set.builder().build().quantify(0, 0, Quantified.Type.GREEDY));

  /**
   * Constructs {@link Coregex} from provided {@link Pattern} instance.
   *
   * @param pattern regular expression to parse.
   * @return parsed coregex instance.
   * @throws UnsupportedOperationException if provided pattern constructs are not yet supported.
   */
  public static Coregex from(Pattern pattern) {
    return CoregexParser.getInstance().parse(pattern);
  }

  /**
   * @return predefined constructor for regex that matches any character.
   * @see #any(int)
   */
  public static Coregex any() {
    return any(0);
  }

  /**
   * @param flags regex flags
   * @return predefined constructor for regex that matches any character.
   * @see #any()
   */
  public static Coregex any(int flags) {
    if (0 != (flags & Pattern.DOTALL)) {
      return Set.DOTALL.get();
    } else if (0 != (flags & Pattern.UNIX_LINES)) {
      return Set.UNIX_LINES.get();
    } else {
      return Set.ALL.get();
    }
  }

  /**
   * @return predefined constructor for empty regex.
   */
  public static Coregex empty() {
    return EMPTY.get();
  }

  /**
   * @param literal literal
   * @param flags regex flags
   * @return predefined constructor for literal regex.
   */
  public static Coregex literal(String literal, int flags) {
    if (literal.isEmpty()) {
      return empty();
    }
    Set first = Set.builder(flags).single(literal.charAt(0)).build();
    if (1 == literal.length()) {
      return first;
    }
    Set[] rest = new Set[literal.length() - 1];
    for (int i = 1; i < literal.length(); i++) {
      rest[i - 1] = Set.builder(flags).single(literal.charAt(i)).build();
    }
    return new Concat(first, rest);
  }

  public static Coregex wordBoundary(int flags, boolean positive) {
    Set word =
        Set.builder(flags).range('a', 'z').range('A', 'Z').range('0', '9').single('_').build();
    Set notWord = Set.builder(flags).union(word).negate().build();
    Group.Type lookbehind = positive ? Group.Type.LOOKBEHIND : Group.Type.NEGATIVE_LOOKBEHIND;
    Group.Type lookahead = positive ? Group.Type.LOOKAHEAD : Group.Type.NEGATIVE_LOOKAHEAD;
    return new Union(
        new Concat(new Group(lookbehind, empty()), new Group(lookahead, word)),
        new Concat(new Group(lookbehind, word), new Group(lookahead, empty())),
        new Concat(new Group(lookbehind, word), new Group(lookahead, notWord)),
        new Concat(new Group(lookbehind, notWord), new Group(lookahead, word)));
  }

  Coregex() {}

  abstract void generate(Context ctx) throws RewindException;

  abstract String[] match(String input, Context ctx);

  /**
   * Converts this coregex into ones that produce "smaller" values.
   *
   * @return smaller coregexes or empty if this coregex is already the smallest possible.
   */
  public abstract Stream<? extends Coregex> shrink();

  /**
   * Samples one random string that matches this regex.
   *
   * @param seed random seed to use for sampling
   * @return sampled string
   */
  public final String generate(long seed) {
    Random rng = new Random(seed);
    int attempt = 0;
    do {
      try (Context ctx = new Context(rng)) {
        generate(ctx);
        return ctx.toString(true);
      } catch (RewindException ignored) {
      }
    } while (++attempt < Context.maxAttempts);
    throw new IllegalStateException(
        "Cannot generate string for " + this + " after " + Context.maxAttempts + " attempts");
  }

  final boolean matches(String input, Context ctx) {
    String[] matches = match(input, ctx);
    for (String match : matches) {
      if (match.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Quantify this regex.
   *
   * @param min min number of times this regex should be repeated
   * @param max max number of times this regex should be repeated. {@code -1} means no limit.
   * @param type quantifier type.
   * @return quantified regex
   * @see Quantified
   * @throws IllegalArgumentException if min is greater than max or if min is negative or if called
   *     on already quantified regex
   * @see Quantified.Type
   */
  public final Coregex quantify(int min, int max, Quantified.Type type) {
    return 1 == min && 1 == max ? this : new Quantified(this, min, max, type);
  }

  /** Sequential concatenation of regexes. */
  public static final class Concat extends Coregex {

    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    /**
     * @param first first regex
     * @param rest rest of regexes
     */
    public Concat(Coregex first, Coregex... rest) {
      this.first = requireNonNull(first, "first");
      this.rest = Arrays.copyOf(rest, rest.length);
    }

    @Override
    void generate(Context ctx) {
      first.generate(ctx);
      for (Coregex coregex : rest) {
        coregex.generate(ctx);
      }
    }

    @Override
    String[] match(String input, Context ctx) {
      Collection<String> match = Arrays.asList(first.match(input, ctx));
      for (int i = 0; i < rest.length && !match.isEmpty(); i++) {
        Collection<String> nextMatch = new HashSet<>(match.size());
        Coregex coregex = rest[i];
        for (String tail : match) {
          nextMatch.addAll(Arrays.asList(coregex.match(tail, ctx)));
        }
        match = nextMatch;
      }
      return match.toArray(new String[0]);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<Concat> shrink() {
      // shrinking until every concatenated piece can shrink.
      return Stream.concat(
          first.shrink().map(chunk -> new Concat(chunk, rest)),
          IntStream.range(0, rest.length)
              .boxed()
              .flatMap(
                  i ->
                      rest[i]
                          .shrink()
                          .map(
                              chunk -> {
                                Coregex[] rest = Arrays.copyOf(this.rest, this.rest.length);
                                rest[i] = chunk;
                                return new Concat(first, rest);
                              })));
    }

    /**
     * @return underlying regexes in order of concatenation.
     */
    public List<Coregex> concat() {
      List<Coregex> concat = new ArrayList<>(rest.length + 1);
      concat.add(first);
      concat.addAll(Arrays.asList(rest));
      return concat;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Concat concat = (Concat) o;
      return first.equals(concat.first) && Arrays.equals(rest, concat.rest);
    }

    @Override
    public int hashCode() {
      int result = first.hashCode();
      result = 31 * result + Arrays.hashCode(rest);
      return result;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(first.toString());
      for (Coregex coregex : rest) {
        sb.append(coregex.toString());
      }
      return sb.toString();
    }
  }

  static final class Context implements Appendable, AutoCloseable {
    static final int maxAttempts = 100;

    private final Context parent;
    private final int index;
    private final String name;
    private final StringBuilder buffer = new StringBuilder();
    private final Map<Serializable, String> groups;

    private Predicate<Context> lookbehind = null;

    final Random rng;

    Context(Random rng) {
      this.parent = null;
      this.index = 0;
      this.name = null;
      this.rng = rng;
      this.groups = new HashMap<>();
    }

    Context(Context parent, String name) {
      this.parent = parent;
      this.index = parent.index + 1;
      this.name = name;
      this.rng = parent.rng;
      this.groups = parent.groups;
    }

    @Override
    public Context append(char c) {
      buffer.append(c);
      return this;
    }

    @Override
    public Context append(CharSequence csq) {
      buffer.append(csq);
      return this;
    }

    @Override
    public Context append(CharSequence csq, int start, int end) {
      buffer.append(csq, start, end);
      return this;
    }

    int index() {
      return parent.index();
    }

    int length(boolean full) {
      if (!full || null == parent) {
        return buffer.length();
      }
      return parent.length(true) + buffer.length();
    }

    String ref(Serializable ref) {
      return groups.get(ref);
    }

    void finalizeWithLookbehind(Predicate<Context> lookbehind) {
      Predicate<Context> oldLookbehind = this.lookbehind;
      this.lookbehind = null == oldLookbehind ? lookbehind : oldLookbehind.and(lookbehind);
    }

    @Override
    public void close() throws RewindException {
      Context parent = this.parent;
      if (null == parent) {
        if (null != lookbehind && !lookbehind.test(this)) {
          throw new RewindException();
        }
      } else {
        parent.append(this.buffer);
        if (0 < index) {
          parent.groups.put(index, toString(false));
        }
        if (null != name) {
          parent.groups.put(name, toString(false));
        }
        parent.finalizeWithLookbehind(lookbehind);
      }
    }

    String toString(boolean full) {
      if (!full || null == parent) {
        return buffer.toString();
      }
      return parent.toString(true) + buffer;
    }
  }

  /** Regex group. */
  public static final class Group extends Coregex {

    private static final long serialVersionUID = 2L;

    private final Type type;
    private final String name;
    private final Coregex group;

    /**
     * Unnamed capturing group.
     *
     * @param group group body
     * @see Group(Type, Coregex)
     * @see Group(Type, String, Coregex)
     */
    public Group(Coregex group) {
      this(Type.CAPTURING, null, requireNonNull(group, "group"));
    }

    /**
     * Unnamed group.
     *
     * @param type group type
     * @param group group body
     * @see Group(Coregex)
     * @see Group(Type, String, Coregex)
     */
    public Group(Type type, Coregex group) {
      this(requireNonNull(type, "type"), null, requireNonNull(group, "group"));
    }

    /**
     * Named group.
     *
     * @param name group name
     * @param group group body
     * @see Group(Coregex)
     * @see Group(Type, Coregex)
     */
    public Group(String name, Coregex group) {
      this(Type.NAMED, requireNonNull(name, "name"), requireNonNull(group, "group"));
    }

    private Group(Type type, String name, Coregex group) {
      this.type = type;
      this.name = name;
      this.group = group;
    }

    @Override
    @SuppressWarnings("fallthrough")
    void generate(Context ctx) throws RewindException {
      boolean positive = true;
      switch (type) {
        case NON_CAPTURING:
        case ATOMIC:
          group.generate(ctx);
          break;
        case NEGATIVE_LOOKBEHIND:
          positive = false;
        // fall through
        case LOOKBEHIND:
          if (positive == group.matches(ctx.toString(true), ctx)) {
            break;
          }
          throw new RewindException();
        case NEGATIVE_LOOKAHEAD:
          positive = false;
        // fall through
        case LOOKAHEAD:
          int length = ctx.length(true);
          Coregex fullLookbehind =
              new Concat(any().quantify(length, length, Quantified.Type.GREEDY), group);
          final boolean pos = positive;
          ctx.finalizeWithLookbehind(
              root -> pos == fullLookbehind.matches(root.toString(true), root));
          break;
        default:
          try (Context childCtx = new Context(ctx, name)) {
            group.generate(childCtx);
          }
      }
    }

    @Override
    String[] match(String input, Context ctx) {
      switch (type) {
        case NON_CAPTURING:
        case ATOMIC:
          return group.match(input, ctx);
        case LOOKAHEAD:
        case LOOKBEHIND:
        case NEGATIVE_LOOKAHEAD:
        case NEGATIVE_LOOKBEHIND:
          return new String[] {input};
        default:
          try (Context childCtx = new Context(ctx, name)) {
            return group.match(input, childCtx);
          }
      }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<Group> shrink() {
      switch (type) {
        case LOOKAHEAD:
        case LOOKBEHIND:
        case NEGATIVE_LOOKAHEAD:
        case NEGATIVE_LOOKBEHIND:
          return Stream.empty();
        default:
          return group.shrink().map(group -> new Group(type, name, group));
      }
    }

    /**
     * @return group type
     */
    public Type type() {
      return type;
    }

    /**
     * @return group name if group is a named group
     */
    public Optional<String> name() {
      return Optional.ofNullable(name);
    }

    /**
     * @return group body
     */
    public Coregex group() {
      return group;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Group group = (Group) o;
      return type == group.type
          && Objects.equals(this.name, group.name)
          && this.group.equals(group.group);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, name, group);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("(");
      switch (type) {
        case NON_CAPTURING:
          sb.append("?:");
          break;
        case ATOMIC:
          sb.append("?>");
          break;
        case NAMED:
          sb.append("?<").append(name).append('>');
          break;
        case LOOKAHEAD:
          sb.append("?=");
          break;
        case LOOKBEHIND:
          sb.append("?<=");
          break;
        case NEGATIVE_LOOKAHEAD:
          sb.append("?!");
          break;
        case NEGATIVE_LOOKBEHIND:
          sb.append("?<!");
          break;
        default:
          break;
      }
      return sb.append(group).append(')').toString();
    }

    /** Regex group type. * */
    public enum Type {
      NON_CAPTURING,
      CAPTURING,
      ATOMIC,
      NAMED,
      LOOKAHEAD,
      LOOKBEHIND,
      NEGATIVE_LOOKAHEAD,
      NEGATIVE_LOOKBEHIND
    }
  }

  /** Quantified regex. */
  public static final class Quantified extends Coregex {

    private static final long serialVersionUID = 1L;

    private final Coregex quantified;
    private final int min;
    private final int max;
    private final Type type;

    /**
     * Greedily quantified regex with no upper limit.
     *
     * @param quantified quantified regex
     * @param min min number of times this regex should be repeated
     * @throws IllegalArgumentException if min is negative
     * @see Quantified(Coregex, int, int)
     * @see Quantified(Coregex, int, int, Type)
     */
    public Quantified(Coregex quantified, int min) {
      this(quantified, min, -1, Type.GREEDY);
    }

    /**
     * Greedily quantified regex.
     *
     * @param quantified quantified regex
     * @param min min number of times this regex should be repeated
     * @param max max number of times this regex should be repeated. {@code -1} means no limit.
     * @throws IllegalArgumentException if min is greater than max or if min is negative
     * @see Quantified(Coregex, int)
     * @see Quantified(Coregex, int, int, Type)
     */
    public Quantified(Coregex quantified, int min, int max) {
      this(quantified, min, max, Type.GREEDY);
    }

    /**
     * @param quantified quantified regex
     * @param min min number of times this regex should be repeated
     * @param max max number of times this regex should be repeated. {@code -1} means no limit.
     * @param type quantifier type.
     * @throws IllegalArgumentException if min is greater than max or if min is negative
     * @see Quantified(Coregex, int)
     * @see Quantified(Coregex, int, int)
     * @see Type
     */
    public Quantified(Coregex quantified, int min, int max, Type type) {
      this.quantified = requireNonNull(quantified, "quantified");
      if (min < 0 || (-1 != max && min > max)) {
        throw new IllegalArgumentException(
            "min: " + min + " and max: " + max + " has to be positive with min being <= max");
      }
      this.min = min;
      this.max = max;
      this.type = requireNonNull(type, "type");
    }

    @Override
    void generate(Context ctx) throws RewindException {
      int quantifier = 0, min = this.min, max = this.max;
      for (; quantifier < min; quantifier++) {
        quantified.generate(ctx);
      }
      for (; (-1 == max || quantifier < max) && 0 != ctx.rng.nextInt(4); quantifier++) {
        quantified.generate(ctx);
      }
    }

    @Override
    String[] match(String input, Context ctx) {
      int quantifier = 0, min = this.min, max = this.max;
      java.util.Set<String> match = new HashSet<>(Collections.singleton(input));
      for (; quantifier < min && !match.isEmpty(); quantifier++) {
        java.util.Set<String> nextMatch = new HashSet<>(match.size());
        for (String tail : match) {
          nextMatch.addAll(Arrays.asList(quantified.match(tail, ctx)));
        }
        match = nextMatch;
      }
      for (; (-1 == max || quantifier < max) && !match.isEmpty(); quantifier++) {
        java.util.Set<String> nextMatch = new HashSet<>(match.size());
        for (String tail : match) {
          nextMatch.addAll(Arrays.asList(quantified.match(tail, ctx)));
        }
        if (!match.addAll(nextMatch)) {
          break;
        }
      }
      return match.toArray(new String[0]);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<Quantified> shrink() {
      // reducing size and shrinking quantified piece.
      int min = this.min, max = this.max;
      return Stream.concat(
          IntStream.of(0, 1, 2, 3, 5, 8, 13)
              .filter(i -> -1 == max || min + i < max)
              .mapToObj(i -> new Quantified(quantified, min, min + i, type)),
          quantified.shrink().map(quantified -> new Quantified(quantified, min, max, type)));
    }

    /**
     * @return quantified regex
     */
    public Coregex quantified() {
      return quantified;
    }

    /**
     * @return min number of times this regex should be repeated
     */
    public int min() {
      return min;
    }

    /**
     * @return max number of times this regex should be repeated. {@code -1} means no limit.
     */
    public int max() {
      return max;
    }

    /**
     * @return quantifier type. Currently, doesn't affect the generation flow - only display.
     * @see Type
     */
    public Type type() {
      return type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Quantified quantified = (Quantified) o;
      return min == quantified.min
          && max == quantified.max
          && type == quantified.type
          && this.quantified.equals(quantified.quantified);
    }

    @Override
    public int hashCode() {
      return Objects.hash(quantified, min, max, type);
    }

    @Override
    @SuppressWarnings("fallthrough")
    public String toString() {
      if (0 == min && 0 == max) {
        return "";
      }
      StringBuilder string = new StringBuilder();
      string.append(quantified);
      switch (max) {
        case -1:
          switch (min) {
            case 0:
              string.append('*');
              break;
            case 1:
              string.append('+');
              break;
            default:
              string.append('{').append(min).append(",}");
              break;
          }
          break;
        case 1:
          if (min == 0) {
            string.append('?');
            break;
          }
        // fall through
        default:
          if (min == max) {
            string.append('{').append(min).append('}');
          } else {
            string.append('{').append(min).append(',').append(max).append('}');
          }
          break;
      }
      switch (type) {
        case RELUCTANT:
          string.append('?');
          break;
        case POSSESSIVE:
          string.append('+');
          break;
        default:
          break;
      }
      return string.toString();
    }

    /** Quantifier type. */
    public enum Type {
      GREEDY,
      RELUCTANT,
      POSSESSIVE,
    }
  }

  /** Backreference to a group. */
  public static final class Ref extends Coregex {

    private static final long serialVersionUID = 1L;

    private final Serializable ref;

    public Ref(String name) {
      this.ref = requireNonNull(name, "name");
    }

    public Ref(int index) {
      this.ref = index;
    }

    @Override
    void generate(Context ctx) {
      ctx.append(ctx.ref(ref));
    }

    @Override
    String[] match(String input, Context ctx) {
      String ref = ctx.ref(this.ref);
      return input.startsWith(ref) ? new String[] {input.substring(ref.length())} : new String[0];
    }

    /** {@inheritDoc} */
    @Override
    public Stream<Ref> shrink() {
      return Stream.empty();
    }

    public Serializable ref() {
      return ref;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Ref ref = (Ref) o;
      return ref.equals(ref.ref);
    }

    @Override
    public int hashCode() {
      return ref.hashCode();
    }

    @Override
    public String toString() {
      return ref instanceof String ? "\\k<" + ref + '>' : "\\" + ref;
    }
  }

  /** Unification of regexes. */
  public static final class Union extends Coregex {

    private static final long serialVersionUID = 1L;

    private final Coregex first;
    private final Coregex[] rest;

    /**
     * @param first first regex
     * @param rest rest of regexes
     */
    public Union(Coregex first, Coregex... rest) {
      this.first = requireNonNull(first, "first");
      this.rest = Arrays.copyOf(rest, rest.length);
    }

    @Override
    void generate(Context ctx) throws RewindException {
      int index = ctx.rng.nextInt(rest.length + 1);
      (index < rest.length ? rest[index] : first).generate(ctx);
    }

    @Override
    String[] match(String input, Context ctx) {
      Collection<String> match = new HashSet<>(rest.length + 1);
      match.addAll(Arrays.asList(first.match(input, ctx)));
      for (Coregex coregex : rest) {
        match.addAll(Arrays.asList(coregex.match(input, ctx)));
      }
      return match.toArray(new String[0]);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<Union> shrink() {
      // reducing and shrinking choices until every union piece can shrink or ran out of choices.
      return Stream.concat(
          IntStream.range(0, rest.length)
              .mapToObj(
                  i -> {
                    Coregex[] rest = new Coregex[this.rest.length - 1];
                    System.arraycopy(this.rest, 0, rest, 0, i);
                    System.arraycopy(this.rest, i + 1, rest, i, this.rest.length - i - 1);
                    return new Union(first, rest);
                  }),
          Stream.concat(
              first.shrink().map(chunk -> new Union(chunk, rest)),
              IntStream.range(0, rest.length)
                  .boxed()
                  .flatMap(
                      i ->
                          rest[i]
                              .shrink()
                              .map(
                                  chunk -> {
                                    Coregex[] rest = Arrays.copyOf(this.rest, this.rest.length);
                                    rest[i] = chunk;
                                    return new Union(first, rest);
                                  }))));
    }

    /**
     * @return underlying regexes forming this unification.
     */
    public List<Coregex> union() {
      List<Coregex> union = new ArrayList<>(rest.length + 1);
      union.add(first);
      union.addAll(Arrays.asList(rest));
      return union;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Union union = (Union) o;
      return first.equals(union.first) && Arrays.equals(rest, union.rest);
    }

    @Override
    public int hashCode() {
      int result = first.hashCode();
      result = 31 * result + Arrays.hashCode(rest);
      return result;
    }

    @Override
    public String toString() {
      StringJoiner joiner = new StringJoiner("|");
      joiner.add(first.toString());
      for (Coregex coregex : rest) {
        joiner.add(coregex.toString());
      }
      return joiner.toString();
    }
  }
}
