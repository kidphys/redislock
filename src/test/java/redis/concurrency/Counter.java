package redis.concurrency;

public class Counter {
	private int successCount;
	private int failCount;

	public void reset() {
		successCount = 0;
		failCount = 0;
	}

	public void success() {
		successCount++;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public void fail() {
		failCount++;
	}

	public int getFailCount() {
		return failCount;
	}
}
