package fm;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import annotations.Annotation;
import annotations.Category;
import ilb.FileMenu;
import ilb.ImageHandler;
import ilb.Mode;
import struct.MetaImage;

/**
 * A basic form to get parameters for running facematch, particularly the
 * weights of different features
 */
@SuppressWarnings("serial")
public class FaceMatchImageMatchForm extends JFrame {
	/**
	 * input fields
	 * <p>
	 * [0] = tolerance threshold
	 * <p>
	 * [1] = HAAR weight
	 * <p>
	 * [2] = LBPH weight
	 * <p>
	 * [3] = SIFT weight
	 * <p>
	 * [4] = SURF weight
	 * <p>
	 * [5] = ORB weight
	 * <p>
	 * [6] = RSILC weight
	 * <p>
	 */
	JFormattedTextField[] input = { new JFormattedTextField(new DecimalFormat()),
			new JFormattedTextField(new DecimalFormat()), new JFormattedTextField(new DecimalFormat()),
			new JFormattedTextField(new DecimalFormat()), new JFormattedTextField(new DecimalFormat()),
			new JFormattedTextField(new DecimalFormat()), new JFormattedTextField(new DecimalFormat()) };

	/**
	 * input field labels
	 * <p>
	 * [0] = tolerance threshold
	 * <p>
	 * [1] = HAAR weight
	 * <p>
	 * [2] = LBPH weight
	 * <p>
	 * [3] = SIFT weight
	 * <p>
	 * [4] = SURF weight
	 * <p>
	 * [5] = ORB weight
	 * <p>
	 * [6] = RSILC weight
	 */
	JLabel[] label = { new JLabel("Tolerance:"), new JLabel("Haar:     "), new JLabel("LBPH:     "),
			new JLabel("SIFT:     "), new JLabel("SURF:     "), new JLabel("ORB:      "), new JLabel("RSILC:    ") };

	/**
	 * input field/label's tool tips.
	 * <p>
	 * [0] = tolerance threshold
	 * <p>
	 * [1] = HAAR weight
	 * <p>
	 * [2] = LBPH weight
	 * <p>
	 * [3] = SIFT weight
	 * <p>
	 * [4] = SURF weight
	 * <p>
	 * [5] = ORB weight
	 * <p>
	 * [6] = RSILC weight
	 */
	String[] tip = {
			"The tolerance defines how many images to return matches for against a given image,\n -1 to match all images, between 0 and 1 to set a similarity distance limit,  and more than one to match the first x images.",
			"The weight to give to the HAAR features in image matching.",
			"The weight to give to the LBPH features in image matching.",
			"The weight to give to the SIFT features in image matching.",
			"The weight to give to the SURF features in image matching.",
			"The weight to give to the ORB features in image matching.",
			"The weight to give to the RSILCR features in image matching." };

	/** Image Handler for list, path, and image needs */
	ImageHandler handler;

	/**
	 * Build the form, first asking tolerance, then for image features &
	 * weights, the for rank/dist/many (whatever those are).
	 * 
	 * @param handle
	 *            image handler
	 * @param group
	 *            true if grouping false if searching
	 */
	public FaceMatchImageMatchForm(ImageHandler handle, final boolean group) {
		this.handler = handle;
		this.setTitle("FaceMatch Parameters");

		// main panel
		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
		content.setAlignmentY(LEFT_ALIGNMENT);
		this.setLayout(new BorderLayout());
		add(content, BorderLayout.CENTER);
		add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		add(Box.createHorizontalStrut(5), BorderLayout.WEST);

		content.add(Box.createVerticalStrut(10));

		// add elements to content, skipping tolerance if its a search since,
		// you match one image to all others regardless
		for (int i = 0; i < this.input.length; i++) {
			if (!group && i == 0)
				continue;
			JPanel j = new JPanel();
			j.setLayout(new BoxLayout(j, BoxLayout.X_AXIS));
			j.add(this.label[i]);
			j.add(this.input[i]);
			j.add(Box.createHorizontalGlue());

			this.input[i].setMaximumSize(this.input[i].getPreferredSize());
			content.add(j);
			content.add(Box.createVerticalStrut(6));
			this.label[i].setToolTipText(this.tip[i]);
			this.input[i].setToolTipText(this.tip[i]);
			this.input[i].setColumns(5);
		}
		this.input[0].setValue(0.5);

		// add the different mode selections, set MANY on by default
		JPanel a = new JPanel(), b = new JPanel(), c = new JPanel();
		final JRadioButton many = new JRadioButton("MANY");
		final JRadioButton dist = new JRadioButton("DIST");
		final JRadioButton rank = new JRadioButton("RANK");
		ButtonGroup g = new ButtonGroup();
		a.add(many);
		a.setLayout(new BoxLayout(a, BoxLayout.X_AXIS));
		b.add(dist);
		b.setLayout(new BoxLayout(b, BoxLayout.X_AXIS));
		c.add(rank);
		c.setLayout(new BoxLayout(c, BoxLayout.X_AXIS));
		g.add(many);
		g.add(dist);
		g.add(rank);
		content.add(a);
		content.add(b);
		content.add(c);
		many.setSelected(true);

		// add launch button
		JButton launch = new JButton("Lauch Image Matching");
		content.add(launch);
		content.add(Box.createVerticalStrut(10));
		// add warning note that ingests will run if they don't exist first
		JLabel ingestWarning = new JLabel(
				"<html>" + "If ingest does not exist," + "<br>" + "it will be loaded first.</html>");
		content.add(ingestWarning);
		content.add(Box.createVerticalStrut(10));
		// add launch command
		launch.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				// store parsed input weights
				Double[] in = new Double[7];

				// regex for double
				String doub = "(-?\\d*\\.\\d*)|(-?\\d+)";

				// get tolerance weight
				if (group) {
					if (!FaceMatchImageMatchForm.this.input[0].getText().matches(doub)) {
						JOptionPane.showConfirmDialog(content,
								"Must have a tolerance rating: negative " + "values match all images, values greater "
										+ "than 1 match the nearest integer number "
										+ "of images, and values between 0 and 1 "
										+ "match images within that distance of " + "the image being matched.",
								"FaceMatch Requires Tolerance", JOptionPane.OK_CANCEL_OPTION);

						return;
					}
					in[0] = Double.parseDouble(FaceMatchImageMatchForm.this.input[0].getText());
				}

				// cycle through parsing feature weights, at least one must be
				// positive non-zero.
				boolean atLeastOneWeight = false;
				for (int i = 1; i < in.length; i++) {
					if (FaceMatchImageMatchForm.this.input[i].getText().matches(doub)) {
						in[i] = Double.parseDouble(FaceMatchImageMatchForm.this.input[i].getText());
						if (in[i] < 0)
							in[i] = (double) 0;
						atLeastOneWeight |= in[i] > 0;
					} else
						in[i] = (double) 0;
				}

				if (!atLeastOneWeight) {
					JOptionPane.showConfirmDialog(content, "At least one feature type must have a weight set.",
							"FaceMatch Must Weight at least one feature.", JOptionPane.OK_CANCEL_OPTION);
					return;
				}

				if (FaceMatchImageMatchForm.this.handler.getListFile() == null) {
					// force user to save
					FileMenu.getNewSaveSpot(FaceMatchImageMatchForm.this.handler);
					// user canceled
					if (FaceMatchImageMatchForm.this.handler.getListFile() == null)
						return;
					FileMenu.save(FaceMatchImageMatchForm.this.handler, false);
				}
				String filePrefix = "Files/" + (FaceMatchImageMatchForm.this.handler.getListFile().getName());
				File f = new File(filePrefix + ".ndx.out");

				// write file pointing to required indices to run facematch
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
					if (in[1] != 0)
						bw.write("HAAR\t" + filePrefix + ".HAAR.ndx.out\t" + in[1] + "\n");
					if (in[2] != 0)
						bw.write("LBPH\t" + filePrefix + ".LBPH.ndx.out\t" + in[2] + "\n");
					if (in[3] != 0)
						bw.write("SIFT\t" + filePrefix + ".SIFT.ndx.out\t" + in[3] + "\n");
					if (in[4] != 0)
						bw.write("SURF\t" + filePrefix + ".SURF.ndx.out\t" + in[4] + "\n");
					if (in[5] != 0)
						bw.write("ORB\t" + filePrefix + ".ORB.ndx.out\t" + in[5] + "\n");
					if (in[6] != 0)
						bw.write("RSILC\t" + filePrefix + ".RSILC.ndx.out\t" + in[6] + "\n");

				} catch (Exception e) {
					e.printStackTrace();
				}

				Object[] arr = {
						// NDX PATH
						f.getAbsolutePath(),
						// MODE
						(many.isSelected() ? "MANY" : (dist.isSelected() ? "DIST" : "RANK")),
						// RepoPATH
						FaceMatchImageMatchForm.this.handler.getDirectory().getAbsolutePath() + File.separator,
						// QueryList
						group ? FaceMatchImageMatchForm.this.handler.getListFile().getAbsolutePath()
								: new File("Files/query.lst").getAbsolutePath(),
						// FF Flags
						FaceFinder.getProcFlags(),
						// Threshold
						!group ? -1 : ((in[0] < 0) ? -1 : ((in[0] > 1) ? in[0].intValue() : in[0])) };

				// make sure the user saves their file, if they want to
				FaceMatchImageMatchForm.this.handler.checkedSaving();

				// make sure ingests have been created
				ArrayList<String> toAdd = new ArrayList<>();
				if (in[1] != 0 && !new File(filePrefix + ".HAAR.ndx.out").exists())
					toAdd.add("HAAR");
				if (in[2] != 0 && !new File(filePrefix + ".LBPH.ndx.out").exists())
					toAdd.add("LBPH");
				if (in[3] != 0 && !new File(filePrefix + ".SIFT.ndx.out").exists())
					toAdd.add("SIFT");
				if (in[4] != 0 && !new File(filePrefix + ".SURF.ndx.out").exists())
					toAdd.add("SURF");
				if (in[5] != 0 && !new File(filePrefix + ".ORB.ndx.out").exists())
					toAdd.add("ORB");
				if (in[6] != 0 && !new File(filePrefix + ".RSILC.ndx.out").exists())
					toAdd.add("RSILC");

				// launch ingest if not yet done.
				if (toAdd.size() != 0) {
					fm.FaceMatchWorker.ingest(FaceMatchImageMatchForm.this.handler, toAdd.toArray());
				}

				// move to grouping window if grouping images
				if (group && FaceMatchImageMatchForm.this.handler.getMode() != Mode.GROUPING_ANNOTATIONS)
					FaceMatchImageMatchForm.this.handler.setMode(Mode.GROUPING_ANNOTATIONS);

				// close form window
				FaceMatchImageMatchForm.this
						.dispatchEvent(new WindowEvent(FaceMatchImageMatchForm.this, WindowEvent.WINDOW_CLOSING));

				// clear image distance charts, and file all images as
				// unannotated
				if (group) {
					for (MetaImage i : FaceMatchImageMatchForm.this.handler.getMasterList()) {
						i.distances.clear();
						for (Annotation anote : i.getAnnotations())
							anote.setCategory(Category.UNTAGGED.toString());
					}
				}

				// launch grouping or search
				new fm.FaceMatchWorker(FaceMatchImageMatchForm.this.handler, arr,
						group ? fm.FaceMatchWorker.GROUP : fm.FaceMatchWorker.SEARCH, 0).execute();

			}
		});

	}

	/**
	 * Display a window to enter and select FaceMatch parameters and then launch
	 * FaceMatch
	 * 
	 * @param group
	 *            true if grouping images, false if search for the matches to
	 *            one image
	 * 
	 * @param h
	 *            image handler to access MetaImages and to get display area for
	 *            messages.
	 */
	public static void getFaceMatchParams(ImageHandler h, boolean group) {
		JFrame f = new FaceMatchImageMatchForm(h, group);
		f.setVisible(true);
		f.pack();
		f.setLocationRelativeTo(h.getImageDisplay());
	}

	/**
	 * dialog for launching an ingest of images for FaceMatch
	 * 
	 * @param h
	 *            ImageHandler supplies info for accessing the image list and
	 *            repository
	 */
	public static void ingestDialog(final ImageHandler h) {
		final JFrame frame = new JFrame("Ingest");
		JPanel content = new JPanel();
		final JCheckBox[] boxes = { new JCheckBox("HAAR"), new JCheckBox("LBPH"), new JCheckBox("SIFT"),
				new JCheckBox("SURF"), new JCheckBox("ORB"), new JCheckBox("RSILC") };
		for (JCheckBox b : boxes)
			content.add(b);
		JButton launch = new JButton("Injest images");
		content.add(launch);
		launch.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				ArrayList<String> arr = new ArrayList<>();
				for (JCheckBox b : boxes)
					if (b.isSelected())
						arr.add(b.getText());
				// close form window
				frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
				fm.FaceMatchWorker.ingest(h, arr.toArray());

			}
		});
		frame.add(content);
		frame.setVisible(true);
		frame.setLocationRelativeTo(h.getImageDisplay());
		frame.pack();
	}
}
