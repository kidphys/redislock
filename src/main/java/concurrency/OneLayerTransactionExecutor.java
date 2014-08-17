package vn.com.web.vnds.concurrency;

import java.util.concurrent.TimeUnit;

public class OneLayerTransactionExecutor implements ITransactionExecutor {
	private long waitTime;
	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
	private ITransactionLock lock;

	public static OneLayerTransactionExecutor createLocalTransactionExecutor(long waitTime, TimeUnit unit) {
		return new OneLayerTransactionExecutor(waitTime, unit, new LocalTransactionLock());
	}

	@SuppressWarnings("unused")
	private OneLayerTransactionExecutor() {
	}

	public OneLayerTransactionExecutor(long waitTime, TimeUnit unit, ITransactionLock lock) {
		this.lock = lock;
		this.waitTime = waitTime;
		this.timeUnit = unit;
	}

	public OneLayerTransactionExecutor(ITransactionLock lock) {
		this.lock = lock;
		this.waitTime = 0;
	}

	public void execute(TransactionExecutable executor) {
		try {
			if (lock.tryLock(waitTime, timeUnit)) {
				try {
					executor.execute();
				} finally {
					lock.unlock();
				}
			} else {
				executor.fail();
			}
		} catch (InterruptedException ex) {
			executor.fail();
		}
	}

	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}
}
