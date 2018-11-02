package ilb;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import annotations.Annotation;
import annotations.AnnotationDifference;
import struct.MetaImage;
import struct.Property;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;

/**
 * The View Menu controls how many images appear in a row, the size of
 * thumbnails, and if the users is switching between GRID, LIST, or GROUPING
 * Mode.
 * 
 * @author bonifantmc
 * 
 */
@SuppressWarnings("serial")
public class ViewMenu extends JMenu implements PropertyChangeListener {

	/** Element for switching to GRID Mode */
	private final Action multiView = new MultiView();
	/** Element for switching to LIST Mode */
	private final Action singleView = new SingleView();
	/** Element for switching to GROUPING_IMAGES Mode */
	private final Action groupImageView = new GroupImageView();
	/** Element for switching to GROUPING_ANNOTATIONS Mode */
	private final Action groupAnnotationView = new GroupAnnotationView();
	/** The slider used for adjusting thumb nail size under the view menu */
	private final SliderMenuItem thumbSizeSlider;

	/***/
	private final JMenu extraLists;

	/**
	 * The ImageHandler whose mode is switched and thumbnail size is controlled
	 * by this menu
	 */
	private final ImageHandler handler;

	/**
	 * Builds the View menu's display
	 * 
	 * @param h
	 *            the Handler this menu will effect
	 */
	ViewMenu(ImageHandler h) {
		super("View");
		this.handler = h;
		extraLists = new ExtraListMenu();
		Vector<Property> v = new Vector<>();
		for (Property p : Property.values())
			v.add(p);
		getHandler().addPropertyChangeListener(this, Property.thumbnailSize, Property.mode);
		setToolTipText("Adjust how images are displayed.");
		add(getSingleView());
		add(getMultiView());
		add(getGroupAnnotationView());
		add(getGroupImageView());
		addSeparator();
		JMenu subMenu = new JMenu("Thumbnail Size");
		this.thumbSizeSlider = new SliderMenuItem();
		subMenu.add(getThumbSizeSlider());
		add(subMenu);
		getThumbSizeSlider().setValue(getHandler().getThumbnailSize());
		add(extraLists);
	}

	/**
	 * Switches ImageHandler to LIST Mode
	 * 
	 * @author bonifantmc
	 */
	private class SingleView extends AbstractAction {

		/** Set display name and tool-tip */
		SingleView() {
			putValue(NAME, "List View");
			putValue(SHORT_DESCRIPTION,
					"View only one thumbnail per row with rich information about images displayed.");
		}

		/** Switch to ListMode */
		@Override
		public void actionPerformed(ActionEvent e) {
			getHandler().setMode(Mode.LIST);

		}
	}

	/**
	 * Switches ImageHandler to GRID Mode
	 * 
	 * @author bonifantmc
	 */
	private class MultiView extends AbstractAction {

		/** Set display name and tool-tip */
		MultiView() {
			putValue(NAME, "Grid View");
			putValue(SHORT_DESCRIPTION, "View multiple thumbnails per row, but with less information displayed.");
		}

		/** Switch to GRID Mode */
		@Override
		public void actionPerformed(ActionEvent e) {
			getHandler().setMode(Mode.GRID);
		}
	}

	/**
	 * Switches ImageHandler to GROUPING Mode.
	 */
	private class GroupAnnotationView extends AbstractAction {
		/**
		 * Set the name to appear in the View menu and the tooltip to go with
		 * it.
		 */
		public GroupAnnotationView() {
			putValue(NAME, "Annotation Grouping");
			putValue(SHORT_DESCRIPTION,
					"Organize each image by grouping it with like images. Each row is a list of similar images");
		}

		/** Switch to GROUPING Mode, try to call FaceMatch's nearDup method */
		@Override
		public void actionPerformed(ActionEvent arg0) {
			getHandler().setMode(Mode.GROUPING_ANNOTATIONS);
		}
	}

	/**
	 * Switches ImageHandler to GROUPING Mode.
	 */
	private class GroupImageView extends AbstractAction {
		/**
		 * Set the name to appear in the View menu and the tooltip to go with
		 * it.
		 */
		public GroupImageView() {
			putValue(NAME, "Image Grouping");
			putValue(SHORT_DESCRIPTION,
					"Organize each image by grouping it with like images. Each row is a list of similar images");
		}

		/** Switch to GROUPING Mode, try to call FaceMatch's nearDup method */
		@Override
		public void actionPerformed(ActionEvent arg0) {
			getHandler().setMode(Mode.GROUPING_IMAGES);
		}
	}

	/**
	 * A Slider that ranges from IMAGEHANDLER.MIN_THUMBNAIL_SIZE to
	 * IMAGEHANDER.MAX_THUMBNAIL_SIZE, allows user to adjust the size of the
	 * ImageHandler's thumb
	 * 
	 * @author bonifantmc
	 * 
	 */
	class SliderMenuItem extends JSlider implements MenuElement, ChangeListener {
		/**
		 * Set the min and max values, arrange labels and tick spacings.Set
		 * listeners.
		 */
		@SuppressWarnings("boxing")
		SliderMenuItem() {
			super();
			int min = ImageHandler.MIN_THUMBNAIL_SIZE, max = ImageHandler.MAX_THUMBNAIL_SIZE;
			setMinimum(min);
			setMaximum(max);
			setValue(getHandler().getThumbnailSize());
			setTitleBorder();
			Hashtable<Integer, JLabel> labels = new Hashtable<>();
			labels.put(min + (max - min) * 1 / 4, new JLabel("Small"));
			labels.put(min + (max - min) * 2 / 4, new JLabel("Medium"));
			labels.put(min + (max - min) * 3 / 4, new JLabel("Large"));
			labels.put(max, new JLabel("Extra Large"));
			setLabelTable(labels);
			setPaintLabels(true);
			setOrientation(SwingConstants.VERTICAL);
			setMajorTickSpacing(20);
			setMinorTickSpacing(2);
			setSnapToTicks(true);
			addChangeListener(this);
			setPaintTicks(true);
		}

		/**
		 * This slider is its own component for the MenuElement
		 */
		@Override
		public Component getComponent() {
			return this;
		}

		/**
		 * The Slider has no sub elements, returns a 0 length array.
		 */
		@Override
		public MenuElement[] getSubElements() {
			return new MenuElement[0];
		}

		/**
		 * Does nothing
		 */
		@Override
		public void menuSelectionChanged(boolean arg0) {// do nothing
		}

		/**
		 * Pass event to JSlider for handling. Allows up/down arrow keys to move
		 * slider when focused on slider.
		 */
		@Override
		public void processKeyEvent(KeyEvent arg0, MenuElement[] arg1, MenuSelectionManager arg2) {
			super.processKeyEvent(arg0);
		}

		/**
		 * Pass event to JSlider defaults for handling. This allows
		 * getValueIsAdjusting() and drag events to register when using the
		 * slider.
		 */
		@Override
		public void processMouseEvent(MouseEvent arg0, MenuElement[] arg1, MenuSelectionManager arg2) {
			super.processMouseEvent(arg0);
			super.processMouseMotionEvent(arg0);
		}

		/**
		 * reset ImageHandler's thumbnail size
		 */
		@Override
		public void stateChanged(ChangeEvent arg0) {
			getHandler().setThumbnailSize(getValue());
		}

		/**
		 * Creates a border with the pixel size of the thumbnails in its title
		 * section for the slider
		 */
		@SuppressWarnings("boxing")
		void setTitleBorder() {
			setBorder(new CompoundBorder(new TitledBorder(String.format("%d pixels", getValue())),
					new EmptyBorder(10, 10, 10, 10)));
		}
	}

	/**
	 * 
	 * @author bonifantmc
	 *
	 */
	public class ExtraListMenu extends JMenu {

		/**
		 * Set the name to appear in the View menu and the tooltip to go with
		 * it.
		 */
		public ExtraListMenu() {
			super("Manage Extra Lists");
			this.setToolTipText("Add and remove extra lists from the list view.");
			JMenu subMenu = new JMenu("Adjust Overlap Req");
			subMenu.setToolTipText("Adjust the area of overlap required for a true-positive/match");
			subMenu.add(new AnnotationOverlapMenuItem());
			add(subMenu);
			add(new AddList());
			add(new RemoveAll());

			for (File f : handler.getOptionalListFiles())
				add(new RemoveFile(f));
		}

		/**
		 * @author bonifantmc
		 *
		 */
		private class RemoveFile extends JMenuItem {

			/**
			 * @param f
			 *            the file to remove
			 */
			RemoveFile(File f) {
				super("Remove " + f.getName());
				this.setToolTipText("Removes " + f.getName() + " from ILB");
				this.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						handler.removeFileFromOptionalListfiles(f);
						ExtraListMenu.this.remove(RemoveFile.this);
						handler.setMode(Mode.LIST);
					}
				});
			}
		}

		/**
		 * @author bonifantmc
		 *
		 */
		public class AddList extends JMenuItem {
			/**
			 * let the user browse for a new list to add for comparison. Also
			 * add the remove list button to the menu if the user selects a list
			 */
			AddList() {
				super("Add List");
				this.setToolTipText("Add a list for comparison");
				this.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						JFileChooser chooser = FileMenu.browser(FileMenu.VALID_TEXT_EXTENSIONS,
								JFileChooser.FILES_AND_DIRECTORIES, handler.getListFileDirecotry(),
								FileMenu.textListFilter, "Select an extra list of annotations", "Couldn't load list");
						if (chooser.showOpenDialog(handler.getImageDisplay()) == JFileChooser.APPROVE_OPTION) {
							File newList = chooser.getSelectedFile();
							if (handler.getOptionalListFiles().contains(newList))
								return;
							else {
								handler.getOptionalListFiles().add(newList);
								int idx = handler.getOptionalListFiles().indexOf(newList);
								ListReader.loadExtraList(handler, newList, idx, AddList.this);
								handler.setMode(Mode.LIST);
								ExtraListMenu.this.add(new RemoveFile(newList));
							}
						}
					}
				});
			}
		}

		/**
		 * @author bonifantmc
		 *
		 */
		private class RemoveAll extends JMenuItem {
			/**
			 * 
			 */
			RemoveAll() {
				super("Remove all extra lists");
				this.setToolTipText("Add a list for comparison");
				this.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						handler.getOptionalListFiles().clear();
						for (MetaImage image : handler.getMasterList())
							image.getAlternativeAnnotations().clear();
						List<RemoveFile> rfs = new ArrayList<>();
						for (Component item : ExtraListMenu.this.getMenuComponents()) {
							if (item instanceof RemoveFile)
								rfs.add((RemoveFile) item);
						}
						for (RemoveFile item : rfs)
							ExtraListMenu.this.remove(item);
						handler.setMode(Mode.LIST);
					}
				});
			}
		}

		/**
		 * A Slider that ranges from 0 to 1, allows user to adjust the needed
		 * area of overlap for two annotations to be considered equivalent
		 * 
		 * @author bonifantmc
		 * 
		 */
		class AnnotationOverlapMenuItem extends JSlider implements MenuElement, ChangeListener {
			/**
			 * the number the slider is based on, eg whatever the max/'1.0'
			 * really is in the system, divide all values in the slider by this
			 * to get the real number
			 */
			float base = 100f;

			/**
			 * Set the min and max values, arrange labels and tick spacings.Set
			 * listeners.
			 */
			@SuppressWarnings("boxing")
			AnnotationOverlapMenuItem() {
				super();
				int min = 0, max = 100;
				setMinimum(min);
				setMaximum(max);
				setValue((int) (Annotation.min_diff * base));
				setTitleBorder();
				Hashtable<Integer, JLabel> labels = new Hashtable<>();
				labels.put(0, new JLabel("0.0"));
				labels.put((int) (base / 4), new JLabel("0.25"));
				labels.put((int) (base / 2), new JLabel("0.5"));
				labels.put((int) (base * 3 / 4), new JLabel("0.75"));
				labels.put((int) base, new JLabel("1.0"));
				setLabelTable(labels);
				setPaintLabels(true);
				setOrientation(SwingConstants.VERTICAL);
				setMajorTickSpacing(20);
				setMinorTickSpacing(2);
				setSnapToTicks(true);
				addChangeListener(this);
				setPaintTicks(true);
			}

			/**
			 * This slider is its own component for the MenuElement
			 */
			@Override
			public Component getComponent() {
				return this;
			}

			/**
			 * The Slider has no sub elements, returns a 0 length array.
			 */
			@Override
			public MenuElement[] getSubElements() {
				return new MenuElement[0];
			}

			/**
			 * Does nothing
			 */
			@Override
			public void menuSelectionChanged(boolean arg0) {// do nothing
			}

			/**
			 * Pass event to JSlider for handling. Allows up/down arrow keys to
			 * move slider when focused on slider.
			 */
			@Override
			public void processKeyEvent(KeyEvent arg0, MenuElement[] arg1, MenuSelectionManager arg2) {
				super.processKeyEvent(arg0);
			}

			/**
			 * Pass event to JSlider defaults for handling. This allows
			 * getValueIsAdjusting() and drag events to register when using the
			 * slider.
			 */
			@Override
			public void processMouseEvent(MouseEvent arg0, MenuElement[] arg1, MenuSelectionManager arg2) {
				super.processMouseEvent(arg0);
				super.processMouseMotionEvent(arg0);
			}

			/**
			 * reset ImageHandler's thumbnail size
			 */
			@Override
			public void stateChanged(ChangeEvent arg0) {
				Annotation.min_diff = getValue() / base;
				for (MetaImage i : getHandler().getMasterList())
					for (AnnotationDifference ad : i.getDifferences().values())
						ad.calculateIntersection();
				getHandler().setMode(Mode.LIST);
				setTitleBorder();
			}

			/**
			 * Creates a border with the pixel size of the thumbnails in its
			 * title section for the slider
			 */
			@SuppressWarnings("boxing")
			void setTitleBorder() {
				setBorder(new CompoundBorder(
						new TitledBorder(String.format("%3f%% Difference ", getValue() / base)),
						new EmptyBorder(10, 10, 10, 10)));
			}
		}
	}

	/**
	 * Adjust display as ImageHandler changes
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@SuppressWarnings("boxing")
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		switch (evt.getProperty()) {
		case mode: {
			Mode m = (Mode) evt.getNewValue();
			switch (m) {
			case GROUPING_IMAGES:
				getGroupAnnotationView().setEnabled(true);
				getGroupImageView().setEnabled(false);
				getSingleView().setEnabled(true);
				getMultiView().setEnabled(true);
				break;
			case GROUPING_ANNOTATIONS:
				getGroupAnnotationView().setEnabled(false);
				getGroupImageView().setEnabled(true);
				getSingleView().setEnabled(true);
				getMultiView().setEnabled(true);
				break;
			case LIST:
				getGroupAnnotationView().setEnabled(true);
				getGroupImageView().setEnabled(true);
				getSingleView().setEnabled(false);
				getMultiView().setEnabled(true);
				break;

			case GRID:

			default:
				getGroupAnnotationView().setEnabled(true);
				getGroupImageView().setEnabled(true);
				getSingleView().setEnabled(true);
				getMultiView().setEnabled(false);
			}
			break;
		}
		case thumbnailSize:
			getThumbSizeSlider().setTitleBorder();
			getThumbSizeSlider().setValue((int) evt.getNewValue());
			break;
		default:
			break;
		}
	}

	/** @return the Action responsible for switching to LIST Mode */
	private Action getSingleView() {
		return this.singleView;
	}

	/** @return the Action responsible for switching to GRID Mode */
	private Action getMultiView() {
		return this.multiView;
	}

	/** @return the Action responsible for switching to GROUPING Mode */
	private Action getGroupImageView() {
		return this.groupImageView;
	}

	/** @return the Action responsible for switching to GROUPING Mode */
	private Action getGroupAnnotationView() {
		return this.groupAnnotationView;
	}

	/**
	 * @return the object reponsible for adjusting the ImageHandler's thumbnail
	 *         size
	 */
	SliderMenuItem getThumbSizeSlider() {
		return this.thumbSizeSlider;
	}

	/**
	 * @return the ImageHandler this menu works with, adjust thumbnail size and
	 *         changing modes for
	 */
	ImageHandler getHandler() {
		return this.handler;
	}

}
