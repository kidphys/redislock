package vn.com.web.vnds.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import java.util.concurrent.locks.ReentrantLock;

public class LocalTransactionLock implements ITransactionLock{
	private Lock lock;
	
	public LocalTransactionLock(){
		this.lock = new ReentrantLock();
	}
	
	@Override
	public Boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		return lock.tryLock(time, unit);
	}

	@Override
	public Boolean tryLock() {
		return lock.tryLock();
	}

	@Override
	public void unlock() {
		lock.unlock();
	}

}
