package redis.concurrency;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

interface Imp{
	void doIt(String arg);
}

public class DistributedLockTest {
	
	private LockServer server;
	
	@Before
	public void setUp(){
		server = mock(LockServer.class);
	}

	@Test
	public void testSanity() {
		DistributedLock lock = new DistributedLock(server, "foo");
		lock.tryLock();
		verify(server).acquireLock(Mockito.anyString(), Mockito.anyString());
		lock.unlock();
		verify(server).releaseLock(Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void waitLongEnoughLockWillBeAvail() throws InterruptedException {
		when(server.acquireLock(Mockito.anyString(), Mockito.anyString())).thenReturn(false);
		DistributedLock lock = new DistributedLock(server, "foo");
		(new Timer()).schedule(new TimerTask() {
			@Override
			public void run() {
				when(server.acquireLock(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
			}
		}, 100);
		Assert.assertTrue(lock.tryLock(210, TimeUnit.MILLISECONDS));
	}	
	
	@Test
	public void eachLockShouldAcquireLockWithDifferentPassword(){
		ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
		DistributedLock lock1 = new DistributedLock(server, "foo");
		DistributedLock lock2 = new DistributedLock(server, "foo");
		lock1.tryLock();
		lock2.tryLock();
		verify(server, Mockito.times(2)).acquireLock(Mockito.anyString(), args.capture());
		List<String> values = args.getAllValues();
		Assert.assertNotEquals(values.get(0), values.get(1));
	}
	
	@Test
	public void releaseLockIfServerFault(){
		DistributedLock lock = new DistributedLock(server, "foo");
		when(server.acquireLock(Mockito.anyString(),Mockito.anyString())).thenThrow(new RuntimeException());
		Assert.assertTrue(lock.tryLock());
	}
	
	@Test
	public void shouldNotTryIfServerIsInFaultState() throws InterruptedException{
		DistributedLock lock = new DistributedLock(server, "foo");
		when(server.isAlive()).thenReturn(false);
		long now = System.currentTimeMillis();
		lock.tryLock(200, TimeUnit.MILLISECONDS);
		Assert.assertTrue(System.currentTimeMillis() - now < 20);
	}
}
