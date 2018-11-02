package struct;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny wrapper to hold a float, to cheat Java's pass-by-value nature.
 * Additionally has listeners to fire when things change
 * 
 * @author bonifantmc
 */
public class AdjutsableFloat {
	/**
	 * the value to adjust
	 */
	private float f;
	/** the max value to adjust to */
	private float max = Float.MAX_VALUE;
	/** the min value to adjust to */
	private float min = Float.MIN_VALUE;

	/**
	 * @param init
	 *            the initial value
	 */
	public AdjutsableFloat(float init) {
		this.f = init;
	}

	/**
	 * @param init
	 *            the initial value
	 * @param min
	 *            the minimum value
	 * @param max
	 *            the maximum value
	 */
	public AdjutsableFloat(float init, float min, float max) {
		this.f = init;
		this.min = min;
		this.max = max;
	}

	/** listeners interested in this AdjustableFloat */
	private List<FloatAdjustListener> listeners = new ArrayList<>();

	/**
	 * @param fal
	 *            the listener to add
	 */
	public void addListener(FloatAdjustListener fal) {
		if (!listeners.contains(fal))
			listeners.add(fal);
	}

	/**
	 * @param fal
	 *            the listener to remove
	 */
	public void removeListener(FloatAdjustListener fal) {
		while (listeners.contains(fal))
			listeners.remove(fal);
	}

	/**
	 * @param adjustBy
	 *            add adjustBy to the float.
	 */
	public void adjust(float adjustBy) {
		f += adjustBy;
		if (f < min)
			f = min;
		if (f > max)
			f = max;
		fireListeners();
	}

	/**
	 * @param toSet
	 *            the new float value
	 */
	public void set(float toSet) {
		this.f = toSet;
		if (f < min)
			f = min;
		if (f > max)
			f = max;
		fireListeners();
	}

	/** fire the listeners */
	private void fireListeners() {
		for (FloatAdjustListener fal : listeners)
			fal.onAdjust();
	}

	/**
	 * @return the float
	 */
	public float getFloat() {
		return f;
	}

	/**
	 * @author bonifantmc
	 *
	 */
	public static interface FloatAdjustListener {
		/** do whatever needs doing when the float changes */
		public void onAdjust();
	}

}
