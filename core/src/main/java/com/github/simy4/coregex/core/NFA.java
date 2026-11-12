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

package com.github.simy4.coregex.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/*
 * Non-deterministic finite automaton built from Coregex using Thompson's construction. Every
 * Coregex wires itself between given states, see Coregex#compile(NFA, int, int).
 * Fragments never add edges into `from` or out of `to`, so fragments can safely share endpoints
 * (union) without leaking paths between each other.
 *
 * Unlike Coregex itself, which is free to approximate a regex as long as everything it generates
 * matches, an NFA has to accept exactly the language of the regex. Therefore, constructs that are
 * not regular are rejected with UnsupportedOperationException.
 *
 * The alphabet is UTF-16 chars except surrogates, see Set#DOTALL.
 *
 * Language operations are decided exactly by a lazy breadth-first walk of the product automaton,
 * so whenever a witness exists, the shortest one is returned.
 */
final class NFA {

  private static final int START = 0;
  private static final int ACCEPT = 1;

  static NFA of(Coregex coregex) {
    NFA nfa = new NFA();
    coregex.compileAtBoundary(nfa, START, ACCEPT);
    return nfa;
  }

  private final List<List<Integer>> epsilons = new ArrayList<>();
  private final List<List<Edge>> edges = new ArrayList<>();

  private NFA() {
    state(); // START
    state(); // ACCEPT
  }

  // region construction
  int state() {
    epsilons.add(new ArrayList<>());
    edges.add(new ArrayList<>());
    return epsilons.size() - 1;
  }

  void epsilon(int from, int to) {
    if (from != to) {
      epsilons.get(from).add(to);
    }
  }

  /*
   * Label is shared, not copied: automaton never mutates it.
   */
  void edge(int from, BitSet label, int to) {
    edges.get(from).add(new Edge(label, to));
  }

  void chain(List<Coregex> chunks, int from, int to) {
    int current = from;
    for (int i = 0; i < chunks.size(); i++) {
      int next = chunks.size() - 1 == i ? to : state();
      chunks.get(i).compile(this, current, next);
      current = next;
    }
    if (chunks.isEmpty()) {
      epsilon(from, to);
    }
  }

  // endregion

  boolean matches(CharSequence input) {
    BitSet current = initial();
    for (int i = 0; i < input.length() && !current.isEmpty(); i++) {
      current = step(current, input.charAt(i));
    }
    return current.get(ACCEPT);
  }

  /*
   * Shortest string accepted by this but not the other automaton.
   */
  Optional<String> difference(NFA other) {
    return search(other, false);
  }

  /*
   * Shortest string accepted by both this and the other automaton.
   */
  Optional<String> intersection(NFA other) {
    return search(other, true);
  }

  /*
   * Breadth-first search over pairs of (single state of this NFA, set of states of other NFA).
   * Only the other side is (lazily) determinized, as only it needs to be complemented.
   *
   * otherAccepts: whether other has to accept (intersection) or reject (difference) the witness.
   *
   * Pairs are pruned with antichains: for difference, (state, others) is subsumed by an already
   * visited (state, subset of others), as every word rejected from the bigger set is rejected from
   * the smaller one too. Symmetrically for intersection with supersets. Being breadth-first, the
   * subsuming pair is never deeper, so the first witness found is still the shortest one.
   */
  private Optional<String> search(NFA other, boolean otherAccepts) {
    char[] alphabet = alphabet(other);
    Frontier frontier = new Frontier(edges.size(), otherAccepts);
    frontier.offer(initial(), other.initial(), null, '\0');
    for (Node node = frontier.poll(); null != node; node = frontier.poll()) {
      if (ACCEPT == node.state && otherAccepts == node.otherStates.get(ACCEPT)) {
        return Optional.of(node.witness());
      }
      for (char ch : alphabet) {
        BitSet next = step(singleton(node.state), ch);
        if (next.isEmpty()) {
          continue;
        }
        BitSet nextOther = other.step(node.otherStates, ch);
        if (otherAccepts && nextOther.isEmpty()) {
          continue; // other can never accept from here on
        }
        frontier.offer(next, nextOther, node, ch);
      }
    }
    return Optional.empty();
  }

  /*
   * Splits all characters on edge labels of both automatons into equivalence classes (minterms):
   * characters that are indistinguishable by every edge. Each class is then represented by its
   * simplest character, so that witnesses are readable.
   */
  private char[] alphabet(NFA other) {
    java.util.Set<BitSet> labels = new LinkedHashSet<>();
    collectLabels(labels);
    other.collectLabels(labels);
    BitSet universe = new BitSet();
    for (BitSet label : labels) {
      universe.or(label);
    }
    List<BitSet> blocks = new ArrayList<>();
    if (!universe.isEmpty()) {
      blocks.add(universe);
    }
    for (BitSet label : labels) {
      List<BitSet> refined = new ArrayList<>(blocks.size() * 2);
      for (BitSet block : blocks) {
        BitSet in = (BitSet) block.clone();
        in.and(label);
        if (in.isEmpty() || in.equals(block)) {
          refined.add(block);
        } else {
          BitSet out = (BitSet) block.clone();
          out.andNot(label);
          refined.add(in);
          refined.add(out);
        }
      }
      blocks = refined;
    }
    char[] alphabet = new char[blocks.size()];
    for (int i = 0; i < alphabet.length; i++) {
      alphabet[i] = simplest(blocks.get(i));
    }
    Arrays.sort(alphabet);
    return alphabet;
  }

  /*
   * Simplest of given chars, in the same order of preference as Set#shrink(): digits, lowercase and
   * uppercase letters, then printable ASCII, then anything else.
   */
  private static char simplest(BitSet chars) {
    for (char[] range : new char[][] {{'0', '9'}, {'a', 'z'}, {'A', 'Z'}, {' ', '~'}}) {
      int ch = chars.nextSetBit(range[0]);
      if (0 <= ch && ch <= range[1]) {
        return (char) ch;
      }
    }
    return (char) chars.nextSetBit(0);
  }

  private void collectLabels(java.util.Set<BitSet> labels) {
    for (List<Edge> stateEdges : edges) {
      for (Edge edge : stateEdges) {
        labels.add(edge.label);
      }
    }
  }

  private BitSet initial() {
    return closure(singleton(START));
  }

  private BitSet step(BitSet states, char ch) {
    BitSet next = new BitSet(edges.size());
    for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
      for (Edge edge : edges.get(state)) {
        if (edge.label.get(ch)) {
          next.set(edge.target);
        }
      }
    }
    return closure(next);
  }

  private BitSet closure(BitSet states) {
    Deque<Integer> stack = new ArrayDeque<>();
    for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
      stack.push(state);
    }
    while (!stack.isEmpty()) {
      for (int target : epsilons.get(stack.pop())) {
        if (!states.get(target)) {
          states.set(target);
          stack.push(target);
        }
      }
    }
    return states;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int state = 0; state < edges.size(); state++) {
      sb.append(state);
      if (START == state) {
        sb.append(" (start)");
      } else if (ACCEPT == state) {
        sb.append(" (accept)");
      }
      for (Edge edge : edges.get(state)) {
        sb.append(" --");
        appendLabel(sb, edge.label);
        sb.append("--> ").append(edge.target);
      }
      for (int target : epsilons.get(state)) {
        sb.append(" --ε--> ").append(target);
      }
      sb.append('\n');
    }
    return sb.toString();
  }

  private static void appendLabel(StringBuilder sb, BitSet label) {
    BitSet negated = new BitSet();
    negated.set(Character.MIN_VALUE, Character.MAX_VALUE + 1);
    negated.clear(Character.MIN_SURROGATE, Character.MAX_SURROGATE + 1);
    negated.andNot(label);
    boolean negate = ranges(negated) < ranges(label);
    BitSet chars = negate ? negated : label;
    sb.append(negate ? "[^" : "[");
    for (int start = chars.nextSetBit(0); start >= 0; ) {
      int end = chars.nextClearBit(start) - 1;
      sb.append((char) start);
      if (start < end) {
        sb.append(start + 1 < end ? "-" : "");
        sb.append((char) end);
      }
      start = chars.nextSetBit(end + 1);
    }
    sb.append(']');
  }

  private static int ranges(BitSet chars) {
    int ranges = 0;
    for (int ch = chars.nextSetBit(0); ch >= 0; ch = chars.nextSetBit(chars.nextClearBit(ch))) {
      ranges++;
    }
    return ranges;
  }

  private static BitSet singleton(int state) {
    BitSet set = new BitSet(state + 1);
    set.set(state);
    return set;
  }

  private static final class Edge {
    private final BitSet label;
    private final int target;

    private Edge(BitSet label, int target) {
      this.label = label;
      this.target = target;
    }
  }

  /*
   * Breadth-first queue of pairs, deduplicated with an antichain: per state of this NFA, a set of
   * pairwise incomparable sets of states of the other NFA, see #search(NFA, boolean).
   */
  private static final class Frontier {
    private final Deque<Node> queue = new ArrayDeque<>();
    private final List<List<BitSet>> visited;
    private final boolean supersets;

    private Frontier(int states, boolean supersets) {
      this.visited = new ArrayList<>(states);
      for (int i = 0; i < states; i++) {
        visited.add(new ArrayList<>());
      }
      this.supersets = supersets;
    }

    /*
     * Enqueues (state, others) for every given state, unless subsumed by an already visited pair.
     */
    private void offer(BitSet states, BitSet others, Node parent, char ch) {
      for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
        List<BitSet> antichain = visited.get(state);
        if (antichain.stream().noneMatch(seen -> subsumes(seen, others))) {
          antichain.removeIf(seen -> subsumes(others, seen));
          antichain.add(others);
          queue.add(new Node(state, others, parent, ch));
        }
      }
    }

    private Node poll() {
      return queue.poll();
    }

    /*
     * Difference needs the other NFA to reject, so smaller sets subsume. Intersection needs it to
     * accept, so bigger sets do.
     */
    private boolean subsumes(BitSet subsuming, BitSet subsumed) {
      return supersets ? isSubset(subsumed, subsuming) : isSubset(subsuming, subsumed);
    }

    private static boolean isSubset(BitSet sub, BitSet sup) {
      BitSet difference = (BitSet) sub.clone();
      difference.andNot(sup);
      return difference.isEmpty();
    }
  }

  private static final class Node {
    private final int state;
    private final BitSet otherStates;
    private final Node parent;
    private final char ch;

    private Node(int state, BitSet otherStates, Node parent, char ch) {
      this.state = state;
      this.otherStates = otherStates;
      this.parent = parent;
      this.ch = ch;
    }

    private String witness() {
      StringBuilder sb = new StringBuilder();
      for (Node node = this; null != node.parent; node = node.parent) {
        sb.append(node.ch);
      }
      return sb.reverse().toString();
    }
  }
}
