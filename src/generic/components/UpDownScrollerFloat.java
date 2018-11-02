package generic.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import struct.AdjutsableFloat;

/**
 * 
 * An up and down arrow, clicking up increments the AdjustableFloat associated
 * with this scroller, the down decrements.
 * 
 * @author bonifantmc
 *
 */
public class UpDownScrollerFloat extends JPanel {
	/**
	 * the value to adjust
	 */
	private AdjutsableFloat av;
	/**
	 * the value to increment/decrement av by.
	 */
	private float increment;
	/**
	 * value updates the long a button is pressed causing the increment to jump
	 * at accelerated rates (every ten times the update is called, the increment
	 * will increase by a factor)
	 */
	private float aux;

	/**
	 * @param av
	 *            the value to adjust with the scroller
	 * @param increment
	 *            the value to increment/decrement by
	 */
	public UpDownScrollerFloat(AdjutsableFloat av, float increment) {
		this.av = av;
		this.increment = increment;
		this.aux = 1f;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		ActionListener decreaseScale = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				av.adjust(-increment*aux);
				aux+=0.1f;
			}
		};

		ActionListener increaseScale = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				av.adjust(increment*aux);
				aux+=0.1;
			}
		};

		BasicArrowButton increase = new BasicArrowButton(SwingConstants.NORTH);
		increase.addActionListener(increaseScale);
		BasicArrowButton decrease = new BasicArrowButton(SwingConstants.SOUTH);
		decrease.addActionListener(decreaseScale);
		final ButtonModel incModel = increase.getModel();
		final ButtonModel decModel = decrease.getModel();
		add(increase);
		add(decrease);

		int delay = 100;
		Timer inc = new Timer(delay, increaseScale);
		Timer dec = new Timer(delay, decreaseScale);

		increase.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (incModel.isPressed() && !inc.isRunning()) {
					inc.start();
					aux=1;
				} else if (!incModel.isPressed() && inc.isRunning()) {
					inc.stop();
					aux=1;
				}

			}
		});

		decrease.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (decModel.isPressed() && !dec.isRunning()) {
					dec.start();
					aux=1;
				} else if (!decModel.isPressed() && dec.isRunning()) {
					dec.stop();
					aux=1;
				}
			}
		});

	}

}
