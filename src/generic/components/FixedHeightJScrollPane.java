package generic.components;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * A JScrollPane with fixed height, only the constructor and setHeight(int) can
 * change the height
 * 
 * @author bonifantmc
 */
@SuppressWarnings("serial")
public class FixedHeightJScrollPane extends JScrollPane {
	/** The Default height of the pane */
	public static final int DEFAULT = 60;
	/** the fixed height of this pane */
	private int height;

	/**
	 * Make a label with the given fixed height
	 * 
	 * @param l
	 *            the component this pane holds
	 * @param i
	 *            the fixed height of this label
	 */
	public FixedHeightJScrollPane(JComponent l, int i) {
		super(l, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		height = i;
		this.setBorder(null);
	}

	/**
	 * @param i
	 *            the new height
	 */
	public void setHeight(int i) {
		this.height = i;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension ret = super.getPreferredSize();
		ret.height = this.height;
		return ret;
	}

	@Override
	public Dimension getMaximumSize() {
		Dimension ret = super.getMaximumSize();
		ret.height = height;
		return ret;
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension ret = super.getMinimumSize();
		ret.height = height;
		return ret;
	}
}
