package redis.concurrency;

import java.util.Calendar;

public class TimeoutTracker {
	private long lastFailedTime;

	public void triggerFailedTime() {
		this.lastFailedTime = Calendar.getInstance().getTimeInMillis();
	}

	public boolean shouldRetry() {
		// do not try again if it's timeout less than 10 seconds ago
		return lastFailedTime > 0 && Calendar.getInstance().getTimeInMillis() - lastFailedTime < 10 * 1000;
	}
}
