package redis.concurrency;


public interface TransactionExecutable {
	/*
	 * Execute if all condition valid
	 */
	public void execute();

	/**
	 * Execute if condition invalid
	 */
	public void fail();
}
