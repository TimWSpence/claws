/*
 * Copyright 2021-2021 TimWSpence
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

package io.github.timwspence.claws

import cats.implicits.*
import cats.Monoid

class FingerTree[M, A] private[claws](private[claws] val tree: FingerTree.Internal.Tree[M, A], private[claws] val ev: Measured[M, A]):
  private given Measured[M, A] = ev

  def |>(x: A): FingerTree[M, A] = new FingerTree(tree.append(x), ev)

  def append(x: A): FingerTree[M, A] = |>(x)

  def prepend(x: A): FingerTree[M, A] = new FingerTree(tree.prepend(x), ev)

  def map[M2, B](f: A => B)(using M: Measured[M2, B]): FingerTree[M2, B] = new FingerTree(tree.map(f), M)

object FingerTree:

  def empty[M, A](using M: Measured[M, A]): FingerTree[M, A] =
    new FingerTree(Internal.Tree.Empty(), M)

  object Internal:
    enum Tree[M, A]:
      import Node.*
      import Digit.*

      case Empty() extends Tree[M, A]
      case Single(x: A) extends Tree[M, A]
      case Deep(l: Digit[A], t: Tree[M, Node[M, A]], r: Digit[A], measure: M) extends Tree[M, A]

      def measure(using M: Measured[M, A]): M = this match
        case Empty() => M.ev.empty
        case Single(x) => x.measure
        case Deep(_, _, _, m) => m

      def map[M2, B](f: A => B)(using Measured[M2, B]): Tree[M2, B] = this match
        case Empty() => Empty()
        case Single(x) => Single(f(x))
        case Deep(l, t, r, _) => Tree.deep(l.map(f), t.map(_.map(f)), r.map(f))

      def prepend(a: A)(using Measured[M, A]): Tree[M, A] = this match
         case Empty() => Single(a)
         case Single(x) => Tree.deep(One(a), Empty(), One(x))
         case Deep(Four(w, x, y, z), t, r, _) => Tree.deep(Two(a, w), t.prepend(Node.node3(x, y, z)), r)
         case Deep(l, t, r, _) => Tree.deep(l.cons(a), t, r)

      def append(a: A)(using Measured[M, A]): Tree[M, A] = this match
         case Empty() => Single(a)
         case Single(x) => Tree.deep(One(x), Empty(), One(a))
         case Deep(l, t, Four(w, x, y, z), _) => Tree.deep(l, t.append(Node.node3(w, x, y)), Two(z, a))
         case Deep(l, t, r, _) => Tree.deep(l, t, r.snoc(a))

    object Tree:
      def deep[M, A](l: Digit[A], t: Tree[M, Node[M, A]], r: Digit[A])(using M: Measured[M, A]): Tree[M, A] = {
        given Monoid[M] = M.ev
        Deep(l, t, r, l.measure |+| t.measure |+| r.measure)
      }

    enum Node[M, A]:
      case Node2(x: A, y: A, m: M)
      case Node3(x: A, y: A, z: A, m: M)

      def measure: M = this match
        case Node2(_, _, m) => m
        case Node3(_, _, _, m) => m

      def map[M2, B](f: A => B)(using Measured[M2, B]): Node[M2, B] = this match
        case Node2(x,y,_) => Node.node2(f(x), f(y))
        case Node3(x,y,z,_) => Node.node3(f(x), f(y), f(z))

    object Node:
      def node2[M, A](x: A, y: A)(using M: Measured[M, A]): Node[M, A] = {
        given Monoid[M] = M.ev
        Node2(x, y, x.measure |+| y.measure)
      }

      def node3[M, A](x: A, y: A, z: A)(using M: Measured[M, A]): Node[M, A] = {
        given Monoid[M] = M.ev
        Node3(x, y, z, x.measure |+| y.measure |+| z.measure)
      }

      given [M, A](using M: Measured[M, A]): Measured[M, Node[M, A]] =
        new Measured[M, Node[M, A]]:
          override val ev: Monoid[M] = M.ev

          extension (a: Node[M, A])
            def measure: M = a.measure

    enum Digit[A]:
      case One(x: A)
      case Two(x: A, y: A)
      case Three(x: A, y: A, z: A)
      case Four(w: A, x: A, y: A, z: A)

      def cons(a: A): Digit[A] = this match
        case One(x) => Two(a, x)
        case Two(x, y) => Three(a, x, y)
        case Three(x, y, z) => Four(a, x, y, z)
        case _ => throw new IllegalArgumentException("Cannot cons to a Digit.Four")

      def snoc(a: A): Digit[A] = this match
        case One(x) => Two(x, a)
        case Two(x, y) => Three(x, y, a)
        case Three(x, y, z) => Four(x, y, z, a)
        case _ => throw new IllegalArgumentException("Cannot snoc to a Digit.Four")

      def measure[M](using M: Measured[M, A]): M = {
        given Monoid[M] = M.ev
        this match
              case One(x) => x.measure
              case Two(x, y) => x.measure |+| y.measure
              case Three(x, y, z) => x.measure |+| y.measure |+| z.measure
              case Four(w, x, y, z) => w.measure |+| x.measure |+| y.measure |+| z.measure
      }

      def map[B](f: A => B): Digit[B] = this match
        case One(x) => One(f(x))
        case Two(x,y) => Two(f(x), f(y))
        case Three(x,y,z) => Three(f(x), f(y), f(z))
        case Four(w,x,y,z) => Four(f(w), f(x), f(y), f(z))

    object Digit:
      given [M, A](using M: Measured[M, A]): Measured[M, Digit[A]] =
        new Measured[M, Digit[A]]:
          override val ev: Monoid[M] = M.ev

          extension (a: Digit[A])
            def measure: M = a.measure


trait Measured[M, A]:
  val ev: Monoid[M]

  extension (a: A)
    def measure: M

object Measured:
  def apply[M, A](using M: Measured[M, A]): M.type = M

object Test:

  given Measured[Int, String] = ???

  import syntax.*

  val t: FingerTree[Int, String] = "foo" <| FingerTree.empty[Int, String]
