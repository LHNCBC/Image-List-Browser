package image.editing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.text.BadLocationException;

import annotations.Annotation;
import annotations.Annotation.AnnotationChangeListener;
import annotations.Attribute;
import annotations.Category;
import annotations.Feature;
import generic.components.FixedHeightJScrollPane;
import generic.components.GridOfSquaresLayout;
import struct.AnnotationGroups;
import struct.ImageAnnotationPair;
import struct.MetaImage;
import struct.MetaImage.MetaImageChangeListener;
import struct.Thumbnail;

/**
 * Used for selecting attributes for an image
 * 
 * @author bonifantmc
 */
@SuppressWarnings("serial")
public class AttributeMenu extends JPanel implements MetaImageChangeListener {

	/** One tab per annotation in the menu */
	private JTabbedPane tabs = new JTabbedPane();

	/** selects which feature type is being annotated */
	JComboBox<Feature> featureType = new JComboBox<>(Feature.values());

	/** the MetaImage being annotated */
	private MetaImage image;

	/** the EditImage this belongs to */
	private EditImage editImage;

	/**
	 * @param editImage
	 *            the frame/image this menu is editing
	 */
	AttributeMenu(EditImage editImage) {
		// set important fields
		this.editImage = editImage;
		this.changeImage(editImage.getMetaImage());

		//
		this.featureType.setSelectedIndex(0);
		this.featureType.addActionListener(new FeatureTypeListener());

		this.getTabs().addChangeListener(new TabChangeListener());

		// close and cancel buttons should take up the last 2/5s of the
		// display's bottom row.
		JPanel bottomRow = new JPanel();
		bottomRow.setLayout(new GridLayout(1, 5));
		bottomRow.add(Box.createHorizontalGlue());
		bottomRow.add(Box.createHorizontalGlue());
		bottomRow.add(Box.createHorizontalGlue());
		bottomRow.add(new JButton(editImage.cancel));
		bottomRow.add(new JButton(editImage.close));

		JPanel bar = new JPanel();

		bar.setLayout(new GridBagLayout());// bar, BoxLayout.X_AXIS));
		featureType.setPrototypeDisplayValue(Feature.Animal);

		bar.add(featureType, new GBC(0, 0).setFill(GridBagConstraints.BOTH).setWeight(0, 1));
		bar.add(new JButton(editImage.clear), new GBC(1, 0).setFill(GridBagConstraints.BOTH).setWeight(0, 1));
		if (fm.FaceMatchJavaInterface.loaded)
			bar.add(new JButton(editImage.detect), new GBC(2, 0).setFill(GridBagConstraints.BOTH).setWeight(0, 1));
		bar.add(new JButton(editImage.previous), new GBC(3, 0).setFill(GridBagConstraints.BOTH).setWeight(0, 1));
		bar.add(new JButton(editImage.next), new GBC(4, 0).setFill(GridBagConstraints.BOTH).setWeight(0, 1));

		JPanel plusMinus = new JPanel();
		plusMinus.setLayout(new GridOfSquaresLayout(1, 2));

		JButton plus = new JButton(editImage.sizeUp);
		JButton minus = new JButton(editImage.sizeDown);
		// without the 0 insets the insets will squash the space available in
		// the GridOfSquaresLayout and turn them into ellipses (odd since the
		// ellipses are longer, but nonetheless true)
		Insets zeroes = new Insets(0, 0, 0, 0);
		plus.setMargin(zeroes);
		minus.setMargin(zeroes);
		plusMinus.add(minus);
		plusMinus.add(plus);

		bar.add(plusMinus, new GBC(5, 0).setFill(GridBagConstraints.BOTH).setWeight(1, 1));
		bar.add(Box.createHorizontalGlue(), new GBC(6, 0).setFill(GridBagConstraints.BOTH).setWeight(1, 1));

		this.setLayout(new BorderLayout());
		this.add(getTabs(), BorderLayout.CENTER);
		this.add(bar, BorderLayout.NORTH);
		this.add(bottomRow, BorderLayout.SOUTH);

	}

	/** list of active text fields */
	private List<JTextField> textFields = new ArrayList<>();

	/**
	 * Tabs to be added to the 'tabs' member of the AttributeMenu
	 * 
	 * @author bonifantmc
	 */
	public class AnnotationTab extends JPanel implements AnnotationChangeListener {

		/** the Annotation being edited */
		Annotation annotation;

		/** the toString of the Annotation */
		JLabel title = new JLabel();
		/**
		 * JScrollpane that wraps the title label, so as the title grows, it
		 * doesn't push down the rest of the buttons
		 */
		FixedHeightJScrollPane titleScroll = new FixedHeightJScrollPane(title, FixedHeightJScrollPane.DEFAULT);

		/***/
		JTextField id = new JTextField();
		/***/
		JTextField breed = new JTextField();

		/*********************************************************
		 * The Roll/Yaw/Pitch fields, and thier increment/decrement buttons (up
		 * increment, down decrement)
		 *********************************************************/
		/***/
		JFormattedTextField roll = new JFormattedTextField(NumberFormat.getNumberInstance());
		/***/
		BasicArrowButton rollUP = new BasicArrowButton(BasicArrowButton.NORTH);
		/***/
		BasicArrowButton rollDown = new BasicArrowButton(BasicArrowButton.SOUTH);
		/***/
		JFormattedTextField yaw = new JFormattedTextField(NumberFormat.getNumberInstance());
		/***/
		BasicArrowButton yawUP = new BasicArrowButton(BasicArrowButton.NORTH);
		/***/
		BasicArrowButton yawDown = new BasicArrowButton(BasicArrowButton.SOUTH);
		/***/
		JFormattedTextField pitch = new JFormattedTextField(NumberFormat.getNumberInstance());
		/***/
		BasicArrowButton pitchUP = new BasicArrowButton(BasicArrowButton.NORTH);
		/***/
		BasicArrowButton pitchDown = new BasicArrowButton(BasicArrowButton.SOUTH);

		/***/
		JTabbedPane pane;
		/***/
		int i;

		/** true if document listener should listen */
		public boolean pitchedit = true;

		/** true if document listener should listen */
		public boolean yawedit = true;

		/** true if document listener should listen */
		public boolean rolledit = true;

		/** check box, if checked the FaceMask should stay painted */
		JCheckBox faceMaskOn = new JCheckBox("FaceMask");

		/**
		 * the Feature being drawn currently
		 * 
		 * @param a
		 *            the annotation this tab edits
		 * @param pane
		 *            the pane this tab is added to
		 * @param i
		 *            indicates which annotation in the image this is
		 */
		AnnotationTab(Annotation a, JTabbedPane pane, int i) {

			this.annotation = a;
			this.pane = pane;
			this.i = i;
			this.annotation.addAnnotationListener(this);
			this.pane.addTab("Note " + this.i, null, this, "Edit the " + this.i + "th Annotation");
			build();

		}

		@Override
		public void onAnnotationChange() {
			this.title.setText(titleFormat(this.annotation.toString()));
			editImage.getFaceMask().setFace(this.annotation);
			if (this.faceMaskOn.isSelected()|| roll.hasFocus()||yaw.hasFocus()||pitch.hasFocus())
				makeFaceMaskVisible(true);
			else
				makeFaceMaskVisible(false);
		}

		/** build the menu */
		private void build() {
			this.removeAll();
			this.setLayout(new GridBagLayout());

			this.title.setText(titleFormat(this.annotation.toString()));
			add(titleScroll, new GBC(1, 0).setSpan(15, 3));
			title.setOpaque(true);
			title.setBackground(Color.white);
			titleScroll.setBackground(Color.white);
			titleScroll.setBorder(BorderFactory.createMatteBorder(2, 5, 2, 2, Color.WHITE));

			// GENDER
			add(new AttributeMenu.AttributeCheckBoxGroup(this.annotation.getGender(), this.annotation),
					new GBC(1, 4).setAnchor(GridBagConstraints.PAGE_START).setSpan(3, 1));
			// AGE
			add(new AttributeMenu.AttributeCheckBoxGroup(this.annotation.getAge(), this.annotation),
					new GBC(4, 4).setAnchor(GridBagConstraints.PAGE_START).setSpan(3, 1));
			// SKIN TONE
			add(new AttributeMenu.AttributeCheckBoxGroup(this.annotation.getSkin(), this.annotation),
					new GBC(7, 4).setAnchor(GridBagConstraints.PAGE_START).setSpan(3, 1));
			// OCCLUSIONS
			add(new AttributeMenu.AttributeCheckBoxGroup(this.annotation.getOcclusions(), this.annotation),
					new GBC(10, 4).setAnchor(GridBagConstraints.PAGE_START).setSpan(3, 4));
			// WOUNDS
			add(new AttributeMenu.AttributeCheckBoxGroup(this.annotation.getWound(), this.annotation),
					new GBC(13, 4).setAnchor(GridBagConstraints.PAGE_START).setSpan(3, 4));
			// ANIMAL SPECIES
			if (this.annotation.getId().isAnimal())
				add(new AttributeMenu.AttributeCheckBoxGroup(this.annotation.getKind(), this.annotation),
						new GBC(16, 4).setAnchor(GridBagConstraints.PAGE_START).setSpan(3, 4));

			// ID
			add(new JLabel("ID:"), new GBC(1, 5).setAnchor(GridBagConstraints.LINE_END));
			add(this.id, new GBC(2, 5).setAnchor(GridBagConstraints.LINE_START).setSpan(9, 1));
			this.id.setColumns(20);

			// ANIMAL BREED
			if (this.annotation.getId().isAnimal()) {
				add(new JLabel("Breed: "), new GBC(1, 6).setAnchor(GridBagConstraints.LINE_END).setSpan(1, 1));
				add(this.breed, new GBC(2, 6).setAnchor(GridBagConstraints.LINE_START).setSpan(9, 1));
				this.breed.setColumns(20);

			}

			// ROLL
			add(new JLabel("Roll: "), new GBC(1, 7).setAnchor(GridBagConstraints.LINE_END));
			add(this.roll, new GBC(2, 7).setAnchor(GridBagConstraints.LINE_START).setSpan(2, 1));
			this.roll.setColumns(4);

			// PITCH
			add(new JLabel("Pitch: "), new GBC(4, 7).setAnchor(GridBagConstraints.LINE_END));
			add(this.pitch, new GBC(5, 7).setAnchor(GridBagConstraints.LINE_START).setSpan(2, 1));
			this.pitch.setColumns(4);

			// YAW
			add(new JLabel("Yaw: "), new GBC(7, 7).setAnchor(GridBagConstraints.LINE_END));
			add(this.yaw, new GBC(8, 7).setAnchor(GridBagConstraints.LINE_START).setSpan(2, 1));
			this.yaw.setColumns(4);

			// adjust scale of face mask
			add(editImage.getFaceMask().adjustFaceScale(), new GBC(1, 8).setAnchor(GridBagConstraints.LINE_END));
			add(editImage.getFaceMask().adjustFaceTransparency(), new GBC(2, 8).setAnchor(GridBagConstraints.LINE_END));

			faceMaskOn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (faceMaskOn.isSelected()) {
						editImage.getFaceMask().setFace(annotation);

						makeFaceMaskVisible(true);
					} else {
						editImage.getFaceMask().setFace(annotation);
						makeFaceMaskVisible(false);
					}
				}
			});
			add(faceMaskOn, new GBC(3, 8).setAnchor(GridBagConstraints.LINE_END));

			this.yaw.setText(Float.toString(this.annotation.getYaw()));
			this.roll.setText(Float.toString(this.annotation.getRoll()));
			this.pitch.setText(Float.toString(this.annotation.getPitch()));
			this.id.setText(this.annotation.getCategory().toString());
			this.breed.setText(this.annotation.getBreed().toString());

			this.yaw.getDocument().addDocumentListener(new CategoryDocumentListener(ListeningFor.Yaw, this.annotation));
			this.pitch.getDocument()
					.addDocumentListener(new CategoryDocumentListener(ListeningFor.Pitch, this.annotation));
			this.roll.getDocument()
					.addDocumentListener(new CategoryDocumentListener(ListeningFor.Roll, this.annotation));

			RollYawPitchFocusListener rypListener = new RollYawPitchFocusListener();
			this.yaw.addFocusListener(rypListener);
			this.pitch.addFocusListener(rypListener);
			this.roll.addFocusListener(rypListener);
			RollYawPitchWheelListener rypWheelListener = new RollYawPitchWheelListener();

			editImage.addMouseWheelListener(rypWheelListener);

			this.id.getDocument().addDocumentListener(new CategoryDocumentListener(ListeningFor.Id, this.annotation));
			this.breed.getDocument()
					.addDocumentListener(new CategoryDocumentListener(ListeningFor.Breed, this.annotation));
		}

		/**
		 * @author bonifantmc
		 *
		 */
		private class RollYawPitchWheelListener implements MouseWheelListener {
			@Override
			public void mouseWheelMoved(MouseWheelEvent arg0) {
				float increment = (float) arg0.getPreciseWheelRotation();

				if (yaw.hasFocus()) {

					float toSet = betterParseFloat(yaw.getText()) + increment;
					annotation.setYaw(toSet);
					yaw.setText(Float.toString(toSet));
				} else if (roll.hasFocus()) {
					float toSet = betterParseFloat(roll.getText()) + increment;
					annotation.setRoll(toSet);
					roll.setText(Float.toString(toSet));
				} else if (pitch.hasFocus()) {

					float toSet = betterParseFloat(pitch.getText()) + increment;
					annotation.setPitch(toSet);
					pitch.setText(Float.toString(toSet));
				}
			}
		}

		/**
		 * @author bonifantmc
		 *
		 */
		private class CategoryDocumentListener implements DocumentListener {

			/** flag representing which field to update */
			ListeningFor lf;
			/** the annotation being edited */
			Annotation a;

			/**
			 * @param lf
			 *            what field of the annotation is being listened for
			 * @param a
			 *            the annotation this listener is editing
			 */
			CategoryDocumentListener(ListeningFor lf, Annotation a) {
				this.lf = lf;
				this.a = a;

			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				update(arg0);

			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {

				update(arg0);
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {

				update(arg0);

			}

			/**
			 * update the annotation
			 * 
			 * @param dE
			 *            the event requiring the field to update
			 */
			void update(DocumentEvent dE) {
				try {
					String update = dE.getDocument().getText(0, dE.getDocument().getLength());
					switch (this.lf) {
					case Breed:
						if (update.trim().length() == 0)
							update = Category.BREED_UNKNOWN.toString();
						this.a.setBreed(update);
						break;
					case Id:
						if (update.trim().length() == 0)
							update = Category.UNTAGGED.toString();
						String oldCategory = AnnotationTab.this.annotation.getCategory().toString();
						String name = AttributeMenu.this.editImage.getMetaImage().getName();
						AnnotationGroups ag = AttributeMenu.this.editImage.getHandler().getAnnotationGroups();
						Thumbnail oldPair = ag.get(oldCategory).getByName(name);
						if (oldPair == null) {
							Thumbnail newPair = new ImageAnnotationPair(editImage.getMetaImage(),
									((AnnotationTab) getTabs().getSelectedComponent()).annotation);
							ag.move(newPair, update);
						} else
							ag.move(ag.get(oldCategory).getByName(name), update);

						break;

					case Pitch:
						if (!pitchedit)
							return;
						this.a.setPitch(betterParseFloat(update));
						editImage.getFaceMask().setFace(AnnotationTab.this.annotation);
						break;

					case Roll:
						if (!rolledit)
							return;
						this.a.setRoll(betterParseFloat(update));
						editImage.getFaceMask().setFace(AnnotationTab.this.annotation);
						break;

					case Yaw:
						if (!yawedit)
							return;
						this.a.setYaw(betterParseFloat(update));
						editImage.getFaceMask().setFace(AnnotationTab.this.annotation);
						break;

					default:
						break;
					}
				} catch (BadLocationException e) {
					e.printStackTrace();
				}

				this.a.fireListener();
				editImage.getHandler().setMasterListChanged(true);
				// AttributeMenu.this.editImage.pack();
			}
		}

		/**
		 * Listens for when the RollYawPitch fields gain and lose focus
		 * 
		 * @author bonifantmc
		 *
		 */
		private class RollYawPitchFocusListener implements FocusListener {
			/**
			 * On gain of focus, move the FaceMask in front of the ImageLabel
			 * (display it)
			 * 
			 * @param arg0
			 *            the trigger event
			 */
			@Override
			public void focusGained(FocusEvent arg0) {
				if (faceMaskOn.isSelected() || roll.hasFocus() || yaw.hasFocus() || pitch.hasFocus()) {
					editImage.getFaceMask().updateFace(annotation);
					makeFaceMaskVisible(true);
				}
			}

			/**
			 * On loss of focus, move the FaceMask behind the ImageLabel (hide
			 * it)
			 * 
			 * @param arg0
			 *            the triggering event
			 */
			@Override
			public void focusLost(FocusEvent arg0) {
				if (!faceMaskOn.isSelected() && !roll.hasFocus() && !yaw.hasFocus() && !pitch.hasFocus()) {
					makeFaceMaskVisible(false);
				}
			}
		}

	}

	/**
	 * A more robust parser for floats than Float.parseFloat(String) (eg, can
	 * include commas)
	 * 
	 * @param f
	 *            the string to parse
	 * @return a float
	 */
	float betterParseFloat(String f) {
		f = f.trim().replace(",", "");
		if (f.length() == 0)
			return 0.0f;
		float ret;
		try {
			ret = Float.parseFloat(f);
		} catch (NumberFormatException e) {
			ret = 0f;
		}
		return ret;

	}

	/** what the document listener is listening for */

	private static enum ListeningFor {
		/***/
		Roll,
		/***/
		Pitch,
		/***/
		Yaw,
		/***/
		Id,
		/***/
		Breed
	};

	/**
	 * 
	 * @param title
	 *            the title to format
	 * @return formatted version of the title, with html.
	 */
	String titleFormat(String title) {
		title = "<html><p style=\"width:300\">" + title + "</p></html>";
		return title;
	}

	@Override
	public void onChange() {
		for (Annotation a : this.image.getAnnotations())
			a.setSelected(false);
		this.getTabs().removeAll();
		if (this.editImage != null)
			for (int i = 0; i < this.editImage.getMetaImage().getAnnotations().size(); i++)
				new AnnotationTab(this.editImage.getMetaImage().getAnnotations().get(i), this.getTabs(), i + 1);

		if (this.image.getAnnotations().size() > 0) {
			this.image.getAnnotations().get(0).setSelected(true);
			this.getTabs().setSelectedIndex(0);
		}
		this.editImage.pack();
	}

	/**
	 * @author bonifantmc
	 *
	 */
	private class AttributeCheckBoxGroup extends JPanel {
		/**
		 * valid sets for the states that the AttributeCheckBox can be in.
		 */
		Attribute a;
		/** the annotation the attribute belongs to */
		Annotation annot;
		/***/
		private final ArrayList<JToggleButton> boxes = new ArrayList<>();

		/**
		 * @param a
		 *            the Attribute the box interfaces with
		 * @param annote
		 *            the annotation the Attribute belongs to
		 */
		public AttributeCheckBoxGroup(Attribute a, Annotation annote) {
			this.a = a;
			this.annot = annote;
			buildDisplay();
			for (String s : a.values())
				for (JToggleButton b : this.boxes)
					if (b.getText().equals(s))
						b.setSelected(true);
		}

		/***/
		private void buildDisplay() {
			this.removeAll();
			this.boxes.clear();

			this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			this.add(new JLabel(this.a.getRules().toString()));
			if (a.getRules().getValidLength() == 1)
				for (String s : this.a.getRules().values()) {
					JRadioButton b = new JRadioButton(s);
					this.boxes.add(b);
					this.add(b);
				}
			else
				for (String s : this.a.getRules().values()) {
					JCheckBox b = new JCheckBox(s);
					this.boxes.add(b);
					this.add(b);
				}
			for (JToggleButton b : this.boxes)
				b.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						String[] states = getSelectedStates();
						if (AttributeCheckBoxGroup.this.a.getRules().isValidSelection(states)) {
							AttributeCheckBoxGroup.this.a.setState(states);

						} else if (b.isSelected()) {
							for (JToggleButton box : AttributeCheckBoxGroup.this.boxes)
								box.setSelected(false);
							b.setSelected(true);
							AttributeCheckBoxGroup.this.a.setState(b.getText());
						} else if (states.length == 0) {
							for (JToggleButton box : AttributeCheckBoxGroup.this.boxes)
								if (box.getText().equals(Attribute.UNMARKED))
									box.setSelected(true);
							AttributeCheckBoxGroup.this.a.setState(Attribute.UNMARKED);
						}
						boolean noneChecked = true;
						for (JToggleButton box : AttributeCheckBoxGroup.this.boxes) {
							noneChecked &= !box.isSelected();
						}
						if (noneChecked) {
							AttributeCheckBoxGroup.this.a.setState(Attribute.UNMARKED);
							for (JToggleButton box : boxes)
								if (box.getText().equals(Attribute.UNMARKED))
									box.setSelected(true);

						}
						editImage.getHandler().setMasterListChanged(true);
						AttributeCheckBoxGroup.this.annot.fireListener();
					}
				});
		}

		/**
		 * @return find the selected boxes, and return a list of the selected
		 *         states from them
		 */
		String[] getSelectedStates() {
			List<String> ret = new ArrayList<>();
			for (JToggleButton box : this.boxes)
				if (box.isSelected())
					ret.add(box.getText());
			return ret.toArray(new String[] {});
		}
	}

	/**
	 * 
	 * Removes this from the current Image's MetaImage's listeners list, and
	 * adds this to the new Image's MetaImage's listener list.
	 * 
	 * @param i
	 *            the new Image to work with (null if the this AttributeMenu is
	 *            being destroyed)
	 */
	public void changeImage(MetaImage i) {
		if (this.image != null)
			this.image.removeListener(this);

		this.image = i;
		if (this.image == null)
			return;

		for (Annotation a : this.image.getAnnotations())
			a.setSelected(false);
		this.getTabs().removeAll();
		if (this.image != null) {
			this.image.addListener(this);
			for (int j = 0; j < this.image.getAnnotations().size(); j++)
				new AnnotationTab(this.image.getAnnotations().get(j), this.getTabs(), j + 1);
			makeFaceMaskVisible(false);
			if (this.tabs.getComponentCount() > 0)
				this.setSelectedAnnotation(((AnnotationTab) this.tabs.getComponentAt(0)).annotation);
		}

	}

	/**
	 * Set the given annotation or it's ancestor/parent(if one exists) as the
	 * selected annotation, if the Annotation exists on the MetaImage
	 * 
	 * @param tempAnnotation
	 *            the annotation/descendant of the annotation to set as selected
	 */
	public void setSelectedAnnotation(Annotation tempAnnotation) {
		if (tempAnnotation != null) {
			while (tempAnnotation.getParent() != null)
				tempAnnotation = tempAnnotation.getParent();

			int ind = this.image.getAnnotations().indexOf(tempAnnotation);
			if (ind > -1) {
				this.getTabs().setSelectedIndex(ind);
				AnnotationTab at = ((AnnotationTab) this.getTabs().getSelectedComponent());
				editImage.getFaceMask().setFace(at.annotation);
				if (at.faceMaskOn.isSelected()) {
					makeFaceMaskVisible(true);
				} else {
					makeFaceMaskVisible(false);
				}
				at.add(editImage.getFaceMask().adjustFaceScale(), new GBC(1, 8).setAnchor(GridBagConstraints.LINE_END));
				at.add(editImage.getFaceMask().adjustFaceTransparency(),
						new GBC(2, 8).setAnchor(GridBagConstraints.LINE_END));
			}
		}
	}

	/**
	 * @return the selected Annotation
	 */
	public Annotation getSelectedAnnotation() {
		Component i = this.getTabs().getSelectedComponent();
		if (i instanceof AnnotationTab)
			return ((AnnotationTab) i).annotation;
		return null;

	}

	/**
	 * change the selection of featureType based on the given feature
	 * 
	 * @param f
	 *            the new feature type
	 */
	public void changeDrawingFeature(Feature f) {
		this.featureType.setSelectedItem(f);
	}

	/**
	 * A Listener for this class' featureType JComboBox.
	 */
	private class FeatureTypeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (editImage.getDrawingTool() == null)
				return;
			editImage.getDrawingTool().setDrawingFeature(
					AttributeMenu.this.featureType.getItemAt(AttributeMenu.this.featureType.getSelectedIndex()));

		}
	}

	/**
	 * 
	 * A Listener for this class' tabs Tab menu's
	 *
	 */
	private class TabChangeListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			if (AttributeMenu.this.getTabs().getSelectedComponent() != null
					&& AttributeMenu.this.getTabs().getSelectedComponent() instanceof AnnotationTab) {
				AnnotationTab at = (AnnotationTab) AttributeMenu.this.getTabs().getSelectedComponent();
				at.annotation.isDragEdgeVisible = true;
				for (Annotation a : AttributeMenu.this.image.getAnnotations())
					a.setSelected(false);
				AttributeMenu.this.image.getAnnotations().get(AttributeMenu.this.getTabs().getSelectedIndex())
						.setSelected(true);
				editImage.repaint();
				textFields.clear();
				textFields.add(at.breed);
				textFields.add(at.id);
				textFields.add(at.roll);
				textFields.add(at.yaw);
				textFields.add(at.pitch);
				at.add(editImage.getFaceMask().adjustFaceScale(), new GBC(1, 8).setAnchor(GridBagConstraints.LINE_END));
				at.add(editImage.getFaceMask().adjustFaceTransparency(),
						new GBC(2, 8).setAnchor(GridBagConstraints.LINE_END));

				editImage.getFaceMask().setFace(at.annotation);
				if (at.faceMaskOn.isSelected()) {
					makeFaceMaskVisible(true);
				} else {
					makeFaceMaskVisible(false);
				}
			}
		}
	}

	/**
	 * @return true if the user is typing in a text field (so keyboard actions
	 *         know to halt while user is typing).
	 * 
	 * 
	 */
	public boolean userTypingInTextField() {
		for (JTextField textField : textFields)
			if (textField.hasFocus())
				return true;
		return false;

	}

	/** @return the AnnotationTabs */
	public JTabbedPane getTabs() {
		return tabs;
	}

	/**
	 * Bring the FaceMask to the front (or hide it)
	 * 
	 * @param visible
	 *            true to bring to front/make visible, false to hide
	 */
	private void makeFaceMaskVisible(boolean visible) {
		JLayeredPane jlp = editImage.getJLP();
		ImageLabel il = editImage.getImageLabel();
		FaceMask fmask = editImage.getFaceMask();
		jlp.setLayer(il, visible ? 0 : 1);
		jlp.setLayer(fmask, visible ? 1 : 0);
		repaint();
	}

}
