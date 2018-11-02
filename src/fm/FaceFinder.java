package fm;

import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import ilb.ImageHandler;

/**
 * The FaceFinder class consists of the static enum type ProcFlag which
 * enumerates all the processing flags that can be set for FaceMatch's
 * FaceFinder. Additionally, it has helper methods for checking the flags and
 * building a 32bit int flag to pass to FaceFinder based on which flags are set
 * 
 * @author bonifantmc
 *
 */
public class FaceFinder {
	/**
	 * Enumerated type for processing flags used by the FaceFinder.
	 * <p>
	 * Each flag has an integer value that's simply one bit of an int turned on.
	 * bit-oring different flags together builds the entire processing flag.
	 * <p>
	 * Each flag additionally has a check box a tooltip and boolean associated
	 * with it. The checkbox and tooltip are for the ILB GUI to use while the
	 * boolean says if the flag is turned on or not
	 */
	public static enum ProcFlag {
		//
		// Enumerations:
		//

		/** no additional processing */
		none(0, null, null),
		/** detect faces only when no face is given */
		selective(1, null, null),
		/** face detection is on */
		detection(selective.value << 1, null, null),
		/** use pi/2 rotations for detection */
		rotation(detection.value << 1, "Rotation", "Look for faces at 90 degree rotations"),
		/** multi-way rotation; e.g. pi/6/30degree rotations */
		rotationMultiway(rotation.value << 1, "Multi-way Rotation", "Look for faces at 30 degree rotations"),
		/** detect facial features (landmarks) */
		cascade(rotationMultiway.value << 1, "Detect Landmarks", "Search for eyes, ears, nose, mouth, etc"),
		/** use OpenCV GUI for visual feedback and/or annotations */
		visual(cascade.value << 1, null, null),
		/** verbose visuals */
		verbose(visual.value << 1, null, null),
		/** histogram equalization is to correct low contrast images */
		HistEQ(verbose.value << 1, "Histogram Equalization", "correct low contrast images"),
		/** discard any given annotations */
		discard(HistEQ.value << 1, null, null),
		/** set-intersect the found face regions with the given face regions */
		intersect(discard.value << 1, null, null),
		/** skin color sampling is on */
		sampling(intersect.value << 1, null, null),
		/** remove false positives based on skin map */
		skinClrFP(sampling.value << 1, null, null),
		/** keep only those larger faces; if they have landmarks */
		keepCascaded(skinClrFP.value << 1, "Discard Featureless Faces", "Discards large faces detected that lack eyes/ears/nose/mouth"),
		/** seek face landmarks in large skin blobs */
		seekLandmarks(keepCascaded.value << 1, null, null),
		/** seek landmarks in skin blobs using color */
		seekLandmarksColor(seekLandmarks.value << 1, null, null),
		/** pop-up GUI only for images where no faces are given/found */
		visualSelective(seekLandmarksColor.value << 1, null, null),
		/** save scaled version of images */
		saveScaled(visualSelective.value << 1, null, null),
		/** save skin maps of images */
		saveSkinMap(saveScaled.value << 1, null, null),
		/** save normalized face patches */
		saveFaces(saveSkinMap.value << 1, null, null),
		/** correct legacy landmarks scaling problems */
		subScaleCorrection(saveFaces.value << 1, null, null),
		/** do not block results display with a web-cam feed */
		LiveFeed(subScaleCorrection.value << 1, null, null);

		//
		// variables of the ProcFlag class
		//

		/** the words to display as the check box text */
		private String name;
		/** the integer value representation of the flag */
		private int value;
		/** the flag represented as a check box */
		private final JCheckBox box = new JCheckBox();
		/** the state of the flag for when running FaceFiner */
		private boolean isInUse = false;

		/**
		 * 
		 * @param i
		 *            the value of the flag
		 * @param x
		 *            the text to display as the check box
		 * @param y
		 *            the text to display as the tooltip
		 */
		private ProcFlag(int i, String x, String y) {
			this.name = x;
			this.value = i;
			if (x != y && y != null) {
				this.box.setText(x);
				this.box.setToolTipText(y);
			}

			// by defualt only detection is on.
			if (this.value == 1 << 1) {
				this.box.setSelected(true);
				this.isInUse = true;
			} else {
				this.box.setSelected(false);
				this.isInUse = false;
			}
		}

		/** sync each ProcFlag's check box with how its boolean flag is set */
		private static void updateboxes() {
			for (ProcFlag f : ProcFlag.values())
				f.box.setSelected(f.isInUse);
		}

		/** sync each ProcFlag's flag with how its check box is set */
		private static void updateFlags() {
			for (ProcFlag f : ProcFlag.values())
				f.isInUse = f.box.isSelected();
		}

		/**
		 * Take a configuration file and read out the properties for setting the
		 * booleans of the ProcFlags
		 * 
		 * @param prop
		 *            the properties file to read from
		 */
		public static void getProperties(Properties prop) {//

			for (ProcFlag f : ProcFlag.values())
				if (f.name != null)
					f.isInUse = Boolean.parseBoolean((String) prop.getOrDefault(f.name, Boolean.toString(f.isInUse)));
			ProcFlag.updateboxes();
		}

		/**
		 * Take a configuration file and write out the properties for setting
		 * the booleans of FaceFinder
		 * 
		 * @param prop
		 *            the properties file to write to
		 */
		public static void setProperties(Properties prop) {
			for (ProcFlag f : ProcFlag.values())
				if (f.name != null)
					prop.setProperty(f.name, Boolean.toString(f.isInUse));

		}

	}

	/**
	 * Check all the ProcFlags and OR together the values of those that are set
	 * true.
	 * 
	 * @return current FaceFinder Processing Flags set on as an int
	 */
	public static int getProcFlags() {
		int ret = 0;
		for (ProcFlag p : ProcFlag.values())
			if (p.isInUse)
				ret |= p.value;
		return ret;
	}

	/**
	 * Launch a pop-up dialogue to get parameter choices from users. Users can
	 * only set flags that have the name variable set to something non-null.
	 * 
	 * @param h
	 *            an ImageHandler that provides the location of where to launch
	 *            the dialogue
	 */
	public static void setParameters(ImageHandler h) {

		// build pop-up window
		final JFrame popUpWindow = new JFrame("FaceFinder Parameters");
		final JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		popUpWindow.setContentPane(contentPanel);

		// user only sees check boxes for ProcFlags with non-null names
		for (ProcFlag f : ProcFlag.values()) {
			if (f.name != null)
				contentPanel.add(f.box);
		}

		// make sure check boxes are updated
		ProcFlag.updateboxes();

		// build buttons to accept or cancel changes.
		JPanel closeButtons = new JPanel();
		JButton accept = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		closeButtons.setLayout(new BoxLayout(closeButtons, BoxLayout.X_AXIS));
		closeButtons.add(javax.swing.Box.createHorizontalGlue());
		closeButtons.add(accept);
		closeButtons.add(Box.createHorizontalGlue());
		closeButtons.add(cancel);
		closeButtons.add(Box.createHorizontalGlue());

		// when ok is clicked close and update flags to match checkboxes, when
		// cancel is clicked, close and restore check boxes to match flags
		CloseWindow cw = new CloseWindow(popUpWindow);
		accept.addMouseListener(cw);
		cancel.addMouseListener(cw);
		contentPanel.add(closeButtons);

		// launch the pop-up dialogue.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				popUpWindow.pack();
				popUpWindow.setLocationRelativeTo(h.getImageDisplay());
				popUpWindow.setVisible(true);
				popUpWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			}
		});

	}

	/**
	 * If the user closes the dialogue from setParameters by clicking OK, then
	 * save the new parameters.
	 * 
	 * @author bonifantmc
	 *
	 */
	private static class CloseWindow extends MouseAdapter {
		/** the frame to close when this listener is triggered */
		JFrame frame;

		/**
		 * @param frame
		 *            the frame that will close when this adapter is triggered
		 */
		CloseWindow(JFrame frame) {
			this.frame = frame;
		}

		/**
		 * When a button is clicked, close the frame. If it was the accept
		 * button, first save the new FaceFinder parameters before closing.
		 */
		@Override
		public void mouseClicked(MouseEvent arg0) {
			// check for proper input
			if (arg0.getSource() instanceof JButton) {
				JButton j = (JButton) arg0.getSource();

				if (j.getText().equals("OK"))
					ProcFlag.updateFlags();

				// close the window
				WindowEvent we = new WindowEvent(this.frame, WindowEvent.WINDOW_CLOSING);
				Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(we);

			}

		}
	}
}