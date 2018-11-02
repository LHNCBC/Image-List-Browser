package struct;

/**
 * @author bonifantmc
 *
 */
public abstract class ComparableRunnable implements Runnable, Comparable<ComparableRunnable> {
	/**
	 * ComparableRunnables are by default ordered by the time they were created,
	 * first come first serve
	 */
	long order = System.currentTimeMillis();

	@Override
	public int compareTo(ComparableRunnable arg0) {
		return Long.compare(order, arg0.order);
	}

	@Override
	abstract public void run();

}
