package vn.com.web.vnds.concurrency;

import java.util.concurrent.TimeUnit;

public class TwoLayerTransactionExecutor implements ITransactionExecutor {
	private ITransactionLock outerLock;
	private ITransactionLock innerLock;
	private long waitTime;
	private TimeUnit timeUnit;
	
	@SuppressWarnings("unused")
	private TwoLayerTransactionExecutor(){};

	public TwoLayerTransactionExecutor(ITransactionLock outerLock, ITransactionLock innerLock, long waitTime, TimeUnit timeUnit) {
		this.outerLock = outerLock;
		this.innerLock = innerLock;
		this.waitTime = waitTime;
		this.timeUnit = timeUnit;
	}

	private void executeInner(TransactionExecutable executable) {
		try {
			if (innerLock.tryLock(waitTime, timeUnit)) {
				try {
					executable.execute();
				} catch (RuntimeException ex) {
					executable.fail();
				} finally {
					innerLock.unlock();
				}
			} else {
				executable.fail();
			}
		} catch (InterruptedException ex) {
			executable.fail();
		}		 
	}

	@Override
	public void execute(TransactionExecutable executable) {
		try {
			if (outerLock.tryLock(waitTime, timeUnit)) {
				try {
					executeInner(executable);
				} catch (RuntimeException ex) {
					// allow execute if inner lock fails
					executable.execute();
				} finally {
					outerLock.unlock();
				}
			} else {
				executable.fail();
			}
		} catch (InterruptedException ex) {
			executable.fail();
		}
	}

}
