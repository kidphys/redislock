package vn.com.web.vnds.concurrency;

import java.util.concurrent.TimeUnit;

public interface ITransactionLock {
	Boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

	Boolean tryLock();

	void unlock();
}
