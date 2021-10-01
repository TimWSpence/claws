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

      def prepend(x: A)(using Measured[M, A]): Tree[M, A] = this match
         case Empty() => Single(x)
         case Single(y) => Tree.deep(One(x), Empty(), One(y))
         case Deep(l, r, t, _) => ???

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
          override val ev = M.ev

          extension (a: Node[M, A])
            def measure: M = a.measure

    enum Digit[A]:
      case One(x: A)
      case Two(x: A, y: A)
      case Three(x: A, y: A, z: A)
      case Four(w: A, x: A, y: A, z: A)

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
