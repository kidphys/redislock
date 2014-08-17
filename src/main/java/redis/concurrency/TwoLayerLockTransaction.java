//package redis.concurrency;
//
//import java.util.concurrent.TimeUnit;
//
//public class TwoLayerLockTransaction implements SynchronizedTransaction {
//	private ITransactionLock outerLock;
//	private ITransactionLock innerLock;
//	private long waitTime;
//	private TimeUnit timeUnit;
//
//	@SuppressWarnings("unused")
//	private TwoLayerLockTransaction(){};
//
//	public TwoLayerLockTransaction(ITransactionLock outerLock, ITransactionLock innerLock, long waitTime, TimeUnit timeUnit) {
//		this.outerLock = outerLock;
//		this.innerLock = innerLock;
//		this.waitTime = waitTime;
//		this.timeUnit = timeUnit;
//	}
//
//	private void executeInner(TransactionExecutable executable) {
//		try {
//			if (innerLock.tryLock(waitTime, timeUnit)) {
//				try {
//					executable.execute();
//				} catch (RuntimeException ex) {
//					executable.fail();
//				} finally {
//					innerLock.unlock();
//				}
//			} else {
//				executable.fail();
//			}
//		} catch (InterruptedException ex) {
//			executable.fail();
//		}
//	}
//
//	public void execute(TransactionExecutable executable) {
//		try {
//			if (outerLock.tryLock(waitTime, timeUnit)) {
//				try {
//					executeInner(executable);
//				} catch (RuntimeException ex) {
//					// allow execute if inner lock fails
//					executable.execute();
//				} finally {
//					outerLock.unlock();
//				}
//			} else {
//				executable.fail();
//			}
//		} catch (InterruptedException ex) {
//			executable.fail();
//		}
//	}
//
//}
