package redis.concurrency;

import java.util.concurrent.TimeUnit;

/**
 * Best use when we don't want to care about unlock the lock after use. Just put the executable inside the transaction.
 * @author kidphys
 *
 */
public class SynchronizedTransaction {
	private long waitTime;
	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
	private TransactionLock lock;

	public SynchronizedTransaction(long waitTime, TimeUnit unit, TransactionLock lock) {
		this.lock = lock;
		this.waitTime = waitTime;
		this.timeUnit = unit;
	}

	public SynchronizedTransaction(TransactionLock lock) {
		this.lock = lock;
		this.waitTime = 1000;
	}

	public void execute(TransactionExecutable executor) {
		try {
			if (lock.tryLock(waitTime, timeUnit)) {
				executor.execute();
			} else {
				executor.fail();
			}
		} catch (InterruptedException ex) {
			executor.fail();
			Thread.currentThread().interrupt();
		}
		finally{
			lock.unlock();
		}
	}

	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}
}
