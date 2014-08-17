package redis.concurrency;

import java.util.concurrent.TimeUnit;

/**
 * Both 2 lock layers must be acquired in order to acquire this lock
 * @author kidphys
 *
 */
public class TwoLayerLock implements TransactionLock {
	private TransactionLock innerLock;
	private TransactionLock outerLock;

	public TwoLayerLock(TransactionLock innerLock, TransactionLock outerLock){
		this.innerLock = innerLock;
		this.outerLock = outerLock;
	}
	
	public Boolean tryLock(long duration, TimeUnit unit)
			throws InterruptedException {
		return outerLock.tryLock(duration, unit) && innerLock.tryLock(duration, unit);
	}

	public Boolean tryLock() {
		return outerLock.tryLock() && innerLock.tryLock();
	}

	public void unlock() {
		outerLock.unlock();
		innerLock.unlock();
	}

}
