package org.ucombinator.jaam

import scala.collection.mutable
import scala.reflect.ClassTag

/* mutable generic store */
abstract class AbstractStore[K <: Addr, E, V <: AbstractDomain[E] : ClassTag](val map: mutable.Map[K, V], val bot: V) {
  var on: Boolean = false
  var print: Boolean = false
  var readAddrs = Set[K]()
  var writeAddrs = Set[K]()

  def resetReadAddrsAndWriteAddrs(): scala.Unit = {
    readAddrs = Set[K]()
    writeAddrs = Set[K]()
  }

  /////////////////////////////////

  def contains(addr: K): Boolean = map.contains(addr)

  def join(store: AbstractStore[K, E, V]): AbstractStore[K, E, V] = {
    store.map.foreach { case (k, v) => {
      this.update(k, v)
    }}
    this
  }

  def get(addr: K): Option[V] = {
    readAddrs += addr
    map.get(addr)
  }
  def getOrElseBot(addr: K): V = {
    this.get(addr) match {
      case Some(value) => value
      case None => bot
    }
  }

  def update(m: mutable.Map[K, V]): AbstractStore[K, E, V] = {
    m.foreach{ case (a, d) => this.update(a, d) }
    this
  }
  def update(addrs: Set[K], d: V): AbstractStore[K, E, V] = {
    addrs.foreach{ case a => this.update(a, d) }
    this
  }
  def update(addrs: Option[Set[K]], d: V): AbstractStore[K, E, V] = {
    addrs match {
      case Some(as) => this.update(as, d)
      case None =>
    }
    this
  }
  def update(addr: K, d: V): AbstractStore[K, E, V] = {
    val oldd: V = this.getOrElseBot(addr)
    val newd: V = oldd.join(d)
    if (on && oldd.values.size != newd.values.size) {
      writeAddrs += addr
    }
    map += (addr -> newd)
    this
  }

  def apply(addr: K): V = {
    readAddrs += addr
    map(addr)
  }
  def apply(addrs: Set[K]): V = {
    readAddrs ++= addrs
    val ds = for (a <- addrs; if map.contains(a)) yield { map(a) }
    val res = ds.fold(bot)(_ join _)
    if (res == bot) throw UndefinedAddrsException(addrs)
    res
  }

  def prettyString() : List[String] =
    (for ((a, d) <- map) yield { a + " -> " + d }).toList
}

object Store {
  val serializer = Soot.toStringSerializer[Store]
}
case class Store(override val map: mutable.Map[Addr, D])
  extends AbstractStore[Addr, Value, D](map, D(Set()))

object KontStore {
  //TODO: use which serializer? toStringSerializer or mapSerializer?
  val serializer = Soot.toStringSerializer[KontStore]
}
case class KontStore(override val map: mutable.Map[KontAddr, KontD])
  extends AbstractStore[KontAddr, Kont, KontD](map, KontD(Set()))
