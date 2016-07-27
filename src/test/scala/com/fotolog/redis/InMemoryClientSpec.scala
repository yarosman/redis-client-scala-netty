package com.fotolog.redis

import org.scalatest.{FlatSpec, Matchers}

/**
 * Will be merged with RedisClientTest when in-memory client will support
 * full set of operations.
 */
class InMemoryClientSpec extends FlatSpec with Matchers with TestClient {

  override lazy val client = createInMemoryClient()

  "A In Memory Client" should "respond to ping" in {
    client.ping() shouldBe true
  }

  it should "support get, set, exists and type operations" in {
    client.exists("foo") shouldBe false
    client.set("foo", "bar", 2592000) shouldBe true

    client.exists("foo") shouldBe true

    client.get[String]("foo").get shouldEqual "bar"
    client.keytype("foo") shouldEqual KeyType.String

    // keys test
    client.set("boo", "baz") shouldBe true
    client.set("baz", "bar") shouldBe true
    client.set("buzzword", "bar") shouldBe true

    client.keys("?oo") shouldEqual Set("foo", "boo")
    client.keys("*") shouldEqual Set("foo", "boo", "baz", "buzzword")
    client.keys("???") shouldEqual Set("foo", "boo", "baz")
    client.keys("*b*") shouldEqual Set("baz", "buzzword", "boo")

    client.del("foo") shouldEqual 1
    client.exists("foo") shouldBe false

    client.del("foo") shouldEqual 0

    // rename
    client.rename("baz", "rbaz") shouldBe true
    client.exists("baz") shouldBe false
    client.get[String]("rbaz") shouldEqual Some("bar")

    // rename nx
    client.rename("rbaz", "buzzword", true) shouldBe false
  }

  it should "support inc/dec operations" in {
    client.set("foo", 1) shouldBe true
    client.incr("foo", 10) shouldEqual 11
    client.incr("foo", -11) shouldEqual 0
    client.incr("unexistent", -5) shouldEqual -5
  }

  it should "fail to rename inexistent key" in {
    intercept[RedisException] {
      client.rename("non-existent", "newkey")
    }
  }

  it should "fail to increment inexistent key" in {
    client.set("baz", "bar") shouldBe true

    intercept[RedisException] {
      client.incr("baz")
    }

  }

  it should "support hash commands" in {

    assert(client.hset[String]("foo", "one", "another"), "Problem with creating hash")
    assert(client.hmset("bar", "baz1" -> "1", "baz2" -> "2"), "Problem with creating 2 values hash")

    client.hget[String]("foo", "one") shouldBe Some("another")
    client.hget[String]("bar", "baz1") shouldBe Some("1")

    assert(Map("baz1" -> "1", "baz2" -> "2") === client.hmget[String]("bar", "baz1", "baz2"), "Resulting map with 2 values")
    assert(Map("baz2" -> "2") === client.hmget[String]("bar", "baz2"), "Resulting map with 1 values")

    assert(7 === client.hincr("bar", "baz2", 5), "Was 2 plus 5 has to give 7")
    assert(-3 === client.hincr("bar", "baz1", -4), "Was 1 minus 4 has to give -3")

    assert(Map("baz1" -> "-3", "baz2" -> "7") === client.hmget[String]("bar", "baz1", "baz2"), "Changed map has to have values 7, -3")

    assert(client.hmset[String]("zoo-key", "foo" -> "{foo}", "baz" -> "{baz}", "vaz" -> "{vaz}", "bzr" -> "{bzr}", "wry" -> "{wry}"))

    val map = client.hmget[String]("zoo-key", "foo", "bzr", "vaz", "wry")

    for(k <- map.keys) {
      assert("{" + k + "}" == map(k).toString, "Values don't correspond to keys in result")
    }

    assert(Map("vaz" -> "{vaz}", "bzr" -> "{bzr}", "wry" -> "{wry}")=== client.hmget[String]("zoo-key", "boo", "bzr", "vaz", "wry"))
    assert(5 === client.hlen("zoo-key"), "Length of map elements should be 5")
    assert(client.hdel("zoo-key", "bzr"), "Problem with deleting")
    client.hget[String]("zoo-key", "bzr") shouldBe None

    assert(client.hexists("zoo-key", "vaz"), "Key 'vaz' should exist in map `zoo-key`")
    assert(4 === client.hlen("zoo-key"), "Length of map elements should be 2")
  }

  /*
  @Test def testKeyTtl() {
    assertTrue(client.set("foo", "bar", 5))
    assertTrue(client.ttl("foo") <= 5)

    assertTrue(client.set("baz", "foo"))

    assertEquals("Ttl if not set should equal -1", -1, client.ttl("baz"))

    assertEquals("Ttl of nonexistent entity has to be -2", -2, client.ttl("bar"))

    assertTrue(client.set("bee", "test", 100))
    assertTrue(client.persist("bee"))
    assertEquals("Ttl of persisted should equal -1", -1, client.ttl("bee"))
  }

  @Test def testSet() {
    assertEquals("Should add 2 elements and create set", 2, client.sadd("sport", "tennis", "hockey"))
    assertEquals("Should add only one element", 1, client.sadd("sport", "football"))
    assertEquals("Should not add any elements", 0, client.sadd("sport", "hockey"))

    assertTrue("Elements should be in set", client.sismember("sport", "hockey"))
    assertFalse("Elements should not be in set", client.sismember("sport", "ski"))
    assertFalse("No set – no elements", client.sismember("drink", "ski"))

    assertEquals("Resulting set has to contain all elements", Set("tennis", "hockey", "football"), client.smembers[String]("sport"))
  }

  @Test def testRedlockScript() {
    import com.fotolog.redis.primitives.Redlock._

    assertTrue(client.set("redlock:key", "redlock:value"))
    assertEquals("Should not unlock redis server with nonexistent value", Set(0), client.eval[Int](UNLOCK_SCRIPT, "redlock:key" -> "non:value"))
    assertEquals("Should unlock redis server", Set(1), client.eval[Int](UNLOCK_SCRIPT, "redlock:key" -> "redlock:value"))

  }

  @Test def testPubSub() {
    val latch = new CountDownLatch(1)
    var invoked = false

    val subscribtionRes = RedisClient("mem:test").subscribe[String]("f*", "foo", "f*", "bar") { (channel, msg) =>
      invoked = true
      latch.countDown()
    }

    assertEquals(Seq(1, 2, 2, 3), subscribtionRes)

    client.publish("fee", "message")

    latch.await(5, TimeUnit.SECONDS)

    assertTrue(invoked)

  }
  */
}
