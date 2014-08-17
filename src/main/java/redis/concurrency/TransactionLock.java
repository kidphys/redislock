package redis.concurrency;

import java.util.concurrent.TimeUnit;

/**
 * Provide common interface for lock that can be use 
 * @author chau.hoang
 *
 */
public interface TransactionLock {
	Boolean tryLock(long duration, TimeUnit unit) throws InterruptedException;

	Boolean tryLock();

	void unlock();
}
