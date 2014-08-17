package redis.concurrency;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.JedisPool;

public class ITRedisLockServer {
	private RedisLockServer server;

	private RedisLockServer createRedisServer() {
		RedisEnvironment env = new RedisEnvironment();
		JedisPool pool = new JedisPool(env.getHost(), env.getPort());
		return new RedisLockServer(pool);
	}
	
	@Before
	public void setUp(){
		server = createRedisServer();
	}
	
	@Test
	public void serverIsAlive(){
		Assert.assertTrue(server.isAlive());
	}
	
	@Test
	public void serverIsDead(){
		JedisPool pool = new JedisPool("dead-server", 1234);
		Assert.assertFalse(new RedisLockServer(pool).isAlive());
	}

	@Test
	public void canAcquireLockTheFirstTime(){
		Assert.assertTrue(server.acquireLock("foo", "bar"));
		server.releaseLock("foo", "bar");
	}
	
	@Test
	public void lockCantBeAquireAfterLockDistributed() throws InterruptedException{
		DistributedLock lock = new DistributedLock(server, "foo");
		lock.tryLock();
		Assert.assertFalse(lock.tryLock(50, TimeUnit.MILLISECONDS));
		lock.unlock();
	}
	
	@Test
	public void lockCantBeAcquireAfterLock() throws InterruptedException{
		server.acquireLock("foo", "bar");
		Assert.assertFalse(server.acquireLock("foo", "bar"));
		Assert.assertFalse(server.acquireLock("foo", "bar2")); // not possible with diff password too
		server.releaseLock("foo", "bar");
	}

	@Test
	public void cantReleaseLockWithWrongPassword(){
		server.acquireLock("foo3", "bar");
		server.releaseLock("foo3", "bar2");
		Assert.assertFalse(server.acquireLock("foo3", "bar"));
	}
	
	@Test
	public void cantAcquireLockWithWrongPassword(){
		server.acquireLock("foo3", "bar");
		server.acquireLock("foo3", "bar2");
		Assert.assertFalse(server.acquireLock("foo3", "bar"));
	}
	
	@Test
	public void canAcquireLockAfterRelease(){
		server.acquireLock("foo4", "bar");
		server.releaseLock("foo4", "bar");
		Assert.assertTrue(server.acquireLock("foo4", "bar"));
	}
}
