package redis.concurrency;


public class TransactionScheduler {
	private final Counter counter;
	public TransactionScheduler(){
		counter = new Counter();
	}

	public void resetCounter(){
		counter.reset();
	}

	public int getCompletedTransaction(){
		return counter.getSuccessCount();
	}
	
	public int getFailedTransaction(){
		return counter.getFailCount();
	}

	public TransactionExecutable createBlockingTask(final long duration){
		TransactionExecutable executable = new TransactionExecutable() {

			public void fail() {
				counter.fail();
			}

			public void execute() {
				try {
					Thread.sleep(duration);
					counter.success();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};

		return executable;
	}

	public TransactionExecutable createExceptionExecutable() {
		return new TransactionExecutable() {

			public void fail() {
				counter.fail();
			}

			public void execute() {
				counter.success();
				throw new RuntimeException();
			}
		};
	}

	public Thread createGenericExecutorThread(final SynchronizedTransaction executor, final TransactionExecutable executable) {
		return new Thread(new Runnable() {
			public void run() {
				try {
					executor.execute(executable);
				} catch (RuntimeException e) {
					// do nothing
				}
			}
		});
	}

	public Thread scheduleBlockingTask(final SynchronizedTransaction executor, final int delayStart,
			final int duration) {
		return new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(delayStart);
					executor.execute(createBlockingTask(duration));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
	}
}
