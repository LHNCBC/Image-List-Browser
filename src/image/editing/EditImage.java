package image.editing;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import annotations.Annotation;
import annotations.Feature;
import ilb.ImageHandler;
import image.editing.UndoRedo.AnnotationSnapshot;
import struct.MetaImage;
import struct.Property;

/**
 * Opens a specified image to its fullest size possible according to screen
 * dimensions in a separate JFrame from the ILB.
 * <p>
 * EditImage windows cannot be resized.
 * <p>
 * EditImage uses the {@link DrawingTool} as a MouseListener and KeyListener for
 * editing image Annotations.
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
public class EditImage extends JFrame {
	/** metadata about the image to display */
	private MetaImage metaImage;
	/** handles working with the images */
	ImageHandler h;

	// listeners and menus
	/**
	 * the tool for adjusting annotation by drawing on the image canvas with the
	 * mouse
	 */
	final private DrawingTool dt;
	/** the tool for adjusting annotations via a side menu */
	final private AttributeMenu am;
	/** tracks changes in the EditImage */
	final private UndoRedo ur;

	// display
	/**
	 * holds the image label and face mask, when the FaceMask is needed, it's
	 * brought to the front and overlays the annotations in the ImageLabel to
	 * aid in aligning the roll, yaw, and pitch correctly
	 */
	final private JLayeredPane jlp;

	/** the canvas the image is displayed in */
	final private ImageLabel il;
	/**
	 * A 3D Face that overlays annotations while adjusting the roll/yaw/pitch
	 * values
	 */
	final private FaceMask fmask;

	/**
	 * close the image, removing all changes (any other images edited in this
	 * window persist
	 */
	final Action cancel = new CancelClose();
	/** close the image keeping all changes (still need to save later) */
	final Action close = new OkClose();
	/** move to the next image in the list file */
	final Action next = new Next();
	/** move to the previous image in the list file */
	final Action previous = new Previous();
	/** Remove all annotations from the image */
	final Action clear = new Clear();
	/** call FaceMatch's FaceFinder on the image */
	final Action detect = new Detect();
	/** decrease max image label dimension */
	final Action sizeDown = new SizeDown();
	/** increase max image label dimension */
	final Action sizeUp = new SizeUp();
	/** action for undoing the last action the user took */
	final Action undo = new Undo();

	/**
	 * 
	 * @param i
	 *            the image to display
	 * @param h
	 *            handles interactions with the images
	 */
	public EditImage(MetaImage i, ImageHandler h) {
		super(i.getName());

		// set important variables
		this.h = h;
		this.metaImage = i;

		// instantiate components of the EditImage
		this.jlp = new JLayeredPane();
		this.fmask = new FaceMask(this);
		this.il = new ImageLabel(i, h, jlp, fmask);
		this.dt = new DrawingTool(this);
		this.am = new AttributeMenu(this);
		this.ur = new UndoRedo(this);
		this.fmask.init();

		jlp.add(fmask, new Integer(0));
		jlp.add(il, new Integer(1));

		// build display

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
		content.add(jlp);
		content.add(getAttributeMenu());
		add(content);

		// add listeners
		getImageLabel().addMouseListener(getDrawingTool());
		getImageLabel().addMouseMotionListener(getDrawingTool());

		setMinimumSize(new Dimension(200, 200));
		// display the window
		pack();
		setVisible(true);
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent arg0) {
				EditImage.this.changeImage(null);
				getHandler().getEditImages().remove(EditImage.this);

			}
		});

		/// add key command/bindings/accelerators/mnemonics
		InputMap im = content.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap actionMap = content.getActionMap();
		// close keep changes on ENTER key
		im.put(KeyStroke.getKeyStroke("ENTER"), "enterPressed");
		actionMap.put("enterPressed", close);

		// close discard changes on ESC key
		im.put(KeyStroke.getKeyStroke("ESCAPE"), "escapePressed");
		actionMap.put("escapePressed", cancel);

		// next on right arrow key
		im.put(KeyStroke.getKeyStroke("RIGHT"), "rightArrowPressed");
		actionMap.put("rightArrowPressed", next);

		// previous on left arrow key
		im.put(KeyStroke.getKeyStroke("LEFT"), "leftArrowPressed");
		actionMap.put("leftArrowPressed", previous);

		// call FaceMatch's FaceMatcher
		im.put(KeyStroke.getKeyStroke("D"), "dPressed");
		actionMap.put("dPressed", detect);

		// clear the image of annotations
		im.put(KeyStroke.getKeyStroke("C"), "cPressed");
		actionMap.put("cPressed", clear);

		// make image bigger
		im.put(KeyStroke.getKeyStroke("EQUALS"), "plusPressed");
		im.put(KeyStroke.getKeyStroke("ADD"), "plusPressed");
		actionMap.put("plusPressed", sizeUp);

		// make image smaller
		im.put(KeyStroke.getKeyStroke("MINUS"), "minusPressed");
		im.put(KeyStroke.getKeyStroke("SUBTRACT"), "minusPressed");
		actionMap.put("minusPressed", sizeDown);

		im.put(KeyStroke.getKeyStroke("control Z"), "ctrlZ");
		actionMap.put("ctrlZ", undo);

		// when the user presses a feature's key update things in the
		// DrawingTool & AttributeMenu
		for (Feature f : Feature.values()) {
			String sym = f.getIdString().toUpperCase();
			im.put(KeyStroke.getKeyStroke(sym), sym + "Pressed");
			actionMap.put(sym + "Pressed", new AbstractAction() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (am.userTypingInTextField())
						return;
					EditImage.this.getDrawingTool().changeDrawingFeature(f);
					EditImage.this.getAttributeMenu().changeDrawingFeature(f);

				}

			});
		}

		this.setResizable(false);
	}

	/**
	 * @param newState
	 *            the state to set the EditImage to
	 */
	public void setState(AnnotationSnapshot newState) {
		if (newState == null)
			return;
		// reset MetaImage Annotations
		getMetaImage().getAnnotations().clear();
		getMetaImage().getAnnotations().addAll(Annotation.cloneList(newState.annotations));

		// adjust drawing tool
		getDrawingTool().setAnchor(newState.snapStart);
		getDrawingTool()
				.setTempAnnotation(newState.snapTempAnnotation == null ? null : newState.snapTempAnnotation.clone());
		getDrawingTool().setDrawingFeature(newState.snapDrawingFeature);
		getDrawingTool().setParent(newState.snapParent);

		// adjust attribute menu
		getAttributeMenu().featureType.setSelectedItem(newState.snapDrawingFeature);

	}

	/**
	 * change this EditImage so it works with a new MetaImage
	 * 
	 * @param i
	 *            the new MetaImage to work with
	 */
	public void changeImage(MetaImage i) {
		for (Annotation a : this.getMetaImage().getAnnotations()) {
			a.setSelected(false);
			a.isDragEdgeVisible = false;
		}
		this.metaImage.setDate(System.currentTimeMillis());
		this.getHandler().firePropertyChange(Property.tally, null);

		this.metaImage = i;
		if (i != null) {
			if (getMetaImage().getAnnotations().size() > 0)
				getMetaImage().getAnnotations().get(0).setSelected(true);
			setTitle(getMetaImage().getName());
		} else
			setTitle("No MetaImage is Selected to Load And Display");

		getImageLabel().changeImage(getMetaImage());
		getDrawingTool().changeImage(getMetaImage());
		getUndoRedo().changeImage(getMetaImage());
		getAttributeMenu().changeImage(getMetaImage());
		getFaceMask().changeImage(getMetaImage());
		pack();
	}

	/** shift to the next MetaImage after the current one */
	void nextImage() {
		if (getMetaImage().getIndexCur() < getHandler().getMasterList().size() - 2)
			changeImage(getHandler().getMasterList().get(getMetaImage().getIndexCur() + 1));
	}

	/** shift to the last MetaImage before the current one */
	void lastImage() {
		if (getMetaImage().getIndexCur() > 0)
			changeImage(getHandler().getMasterList().get(getMetaImage().getIndexCur() - 1));
	}

	/** Record the current state of the EditImage */
	public void snap() {
		getUndoRedo().takeSnapshot();
	}

	/** @return the DrawingTool for this EditImage */
	DrawingTool getDrawingTool() {
		return this.dt;
	}

	/** @return the UndoReo tool that tracks the state of the EditImage */
	UndoRedo getUndoRedo() {
		return this.ur;
	}

	/** @return the ImageLabel that displays the image being Edited */
	ImageLabel getImageLabel() {
		return this.il;
	}

	/** @return The AttributeMenu for adjusting Annotation Attributes */
	AttributeMenu getAttributeMenu() {
		return this.am;
	}

	/** @return the ImageHandler for accessing images */
	ImageHandler getHandler() {
		return this.h;
	}

	/**
	 * @return the MetaImage this EditImage Displays
	 */
	public MetaImage getMetaImage() {
		return this.metaImage;
	}

	////////
	//////// ABSTRACT ACTIONS THAT CAN BE BUNDLED INTO BUTTONS mnemonics
	//////// Accelerators, etc
	///////

	/** clears all annotations for the given edit Image */
	class Clear extends AbstractAction {
		/***/
		public Clear() {
			super("Clear");
			putValue(SHORT_DESCRIPTION, "Remove all Annotations from the image.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (am.userTypingInTextField())
				return;
			getMetaImage().getAnnotations().clear();
		}
	}

	/** calls FaceMatch for detecting Faces */
	class Detect extends AbstractAction {
		/***/
		public Detect() {
			super("Detect");
			putValue(SHORT_DESCRIPTION, "Call FaceMatch to find faces.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (am.userTypingInTextField())
				return;
			fm.FaceMatchWorker.getFaces(h, getMetaImage());
		}
	}

	/** Move on to the next image in the list */
	class Next extends AbstractAction {
		/***/
		public Next() {
			super("Next");
			putValue(SHORT_DESCRIPTION, "Go to the next image in the list.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (am.userTypingInTextField())
				return;
			nextImage();
		}
	}

	/** Move back to the last image in the list */
	class Previous extends AbstractAction {
		/***/
		public Previous() {
			super("Previous");
			putValue(SHORT_DESCRIPTION, "Go back one image in the list.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (am.userTypingInTextField())
				return;
			lastImage();
		}
	}

	/** Move on to the next image in the list */
	class OkClose extends AbstractAction {
		/***/
		public OkClose() {
			super("OK");
			putValue(SHORT_DESCRIPTION, "Close the image, keeping all changes done to it.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (am.userTypingInTextField())
				return;
			dispatchEvent(new WindowEvent(EditImage.this, WindowEvent.WINDOW_CLOSING));
		}
	}

	/** Move back to the last image in the list */
	class CancelClose extends AbstractAction {
		/***/
		public CancelClose() {
			super("Cancel");
			putValue(SHORT_DESCRIPTION, "Close the image, undo all changes done to it.");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (am.userTypingInTextField())
				return;
			getUndoRedo().revert();
			dispatchEvent(new WindowEvent(EditImage.this, WindowEvent.WINDOW_CLOSING));
		}

	}

	/***
	 * Increase size of the Image displayed
	 */
	class SizeUp extends AbstractAction {
		/** prep the action's text and tooltip */
		public SizeUp() {
			super("+");
			putValue(SHORT_DESCRIPTION, "Increase the size of the displayed image");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (am.userTypingInTextField())
				return;
			getImageLabel().sizeUp();
			pack();
			fmask.updateBounds();
		}

	}

	/***
	 * Decrease size of the Image displayed
	 */
	class SizeDown extends AbstractAction {
		/** prep the action's text and tooltip */
		public SizeDown() {
			super("-");
			putValue(SHORT_DESCRIPTION, "Decrease the size of the displayed image");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (am.userTypingInTextField())
				return;
			getImageLabel().sizeDown();
			pack();
			fmask.updateBounds();

		}

	}

	/**
	 * call the undo action
	 */
	class Undo extends AbstractAction {
		/** prep the action's text and tooltip */
		public Undo() {
			super("Undo");
			putValue(SHORT_DESCRIPTION, "Undo the last action taken in changing the image annotation.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (am.userTypingInTextField())
				return;
			EditImage.this.getUndoRedo().undo();
		}
	}

	/**
	 * 
	 * @return the JLayeredPane that holds the ImageLabel and FaceMask
	 */
	public JLayeredPane getJLP() {
		return this.jlp;
	}

	/**
	 * @return this EditImage's FaceMask
	 */
	public FaceMask getFaceMask() {
		return this.fmask;
	}

}
