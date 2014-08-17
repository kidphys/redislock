package redis.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import java.util.concurrent.locks.ReentrantLock;

public class LocalLock implements TransactionLock{
	private Lock lock;

	public LocalLock(){
		this.lock = new ReentrantLock();
	}

	public Boolean tryLock(long duration, TimeUnit unit) throws InterruptedException {
		return lock.tryLock(duration, unit);
	}

	public Boolean tryLock() {
		return lock.tryLock();
	}

	public void unlock() {
		lock.unlock();
	}

}
