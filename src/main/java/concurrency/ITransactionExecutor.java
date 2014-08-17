package vn.com.web.vnds.concurrency;

public interface ITransactionExecutor {
	void execute(TransactionExecutable executable);
}
