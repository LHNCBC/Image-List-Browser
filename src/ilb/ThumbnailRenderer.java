package ilb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import annotations.Annotation;
import annotations.AnnotationDifference;
import image.editing.GBC;
import struct.ILBImageReader;
import struct.ImageAnnotationPair;
import struct.ImageMap;
import struct.MetaImage;
import struct.Pair;
import struct.Property;
import struct.Property.PropertyChangeEvent;
import struct.Property.PropertyChangeListener;
import struct.Thumbnail;

/**
 * A Renderer used in displaying image thumbnails in {@link ThumbnailList}s.
 * <p>
 * getListCellRendererComponent always returns this Renderer, as it is also the
 * JPanel used as a stamp to print the images it renders.
 * 
 * @author bonifantmc
 * @see ThumbnailList
 * @see ImageMap
 * @see ImageHandler
 */
@SuppressWarnings("serial")
public class ThumbnailRenderer extends JPanel implements ListCellRenderer<Thumbnail>, PropertyChangeListener {
	/**
	 * The ImageHandler that provides access to the ImageMap for finding images
	 * to display as Thumbnails, also provides PopertyChangEvents to tell this
	 * Renderer when to shift between displaying for LIST &amp; GRID/GROUPING
	 * Mode
	 */
	private final ImageHandler handler;

	/** The ImageIcon that directly holds and displays the thumbnail image */
	private final ImageIcon baseIcon = new ImageIcon();

	/**
	 * The JLabel that holds the baseIcon, and displays it's name (in
	 * GRID/GROUPING Mode) or its tooltip (in LIST Mode).
	 */
	private final JLabel baseIconHolder = new JLabel();

	/**
	 * The Panel that holds the baseIconHolder and keeps it properly centered/
	 * aligned
	 */
	private final JPanel basePanel = new JPanel();

	/** AN image to display if the defaults are missing */
	private static BufferedImage emergencyBackup = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

	/**
	 * Creates a new Renderer and prepares it for display.
	 * 
	 * @param h
	 *            handler to supply access to images
	 */
	public ThumbnailRenderer(ImageHandler h) {
		this.handler = h;
		getHandler().addPropertyChangeListener(this, Property.mode);

		setOpaque(true);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		getBaseIconHolder().setIcon(getBaseIcon());

		if (h.getMode() == null)
			h.setMode(Mode.GRID);

		getBasePanel().setLayout(new BorderLayout());
		getBasePanel().add(getBaseIconHolder(), BorderLayout.CENTER);

		switch (h.getMode()) {
		case LIST:
			prepareForLIST();
			break;
		case GRID:
		case GROUPING_IMAGES:
		case GROUPING_ANNOTATIONS:
		default:
			prepareForGRIDAndGROUP();
			break;

		}
	}

	/**
	 * Build the proper Thumbnail stamp for the given Thumbnail
	 * 
	 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList,
	 *      java.lang.Object, int, boolean, boolean)
	 */
	@Override
	public Component getListCellRendererComponent(JList<? extends Thumbnail> list, Thumbnail value, int index,
			boolean isSelected, boolean cellHasFocus) {
		switch (getHandler().getMode()) {
		case LIST:
			setToolTipText(null);
			MetaImage i = value.getImage();
			for (int j = 0; j < i.getAlternativeAnnotations().size(); j++) {
				setIcon(extraImages.get(j), i, i.getAlternativeAnnotations().get(j));
			}
			break;
		case GROUPING_IMAGES:
		case GROUPING_ANNOTATIONS:
		case GRID:
		default:
			setToolTipText(value.toolTip());
		}

		setIcon(getBaseIcon(), value, null);
		setText(value);

		if (list != null) {
			setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
			getBasePanel().setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
		}
		return this;
	}

	/***
	 * Pulls the image requested from this Renderer's ImageHandler's ImageMap,
	 * and scales it down to size before adding it to the display and draws the
	 * annotations on it.
	 * 
	 * @param icon
	 *            the icon to draw the image on
	 * 
	 * @param image
	 *            the Thumbnail/MetaImage access to the image to draw
	 * @param annotations
	 *            the annotations to draw (if none provided, use those in the
	 *            thumbnail, else don't draw any)
	 */
	private void setIcon(ImageIcon icon, Thumbnail image, List<Annotation> annotations) {
		BufferedImage toDraw = getHandler().getImages().get(image);

		if (toDraw == null)
			toDraw = emergencyBackup;

		// draw the whole image and all annotations if there's only a MetaImage
		// unpaired.
		if ((image instanceof MetaImage
				|| (image instanceof ImageAnnotationPair && ((ImageAnnotationPair) image).y == null)
				|| annotations != null)) {

			double scaleThmb = getHandler().scaleThumbSize(toDraw.getWidth(), toDraw.getHeight());
			double scaleAnnot = getHandler().scaleThumbSize(image.getImage().getWidth(), image.getImage().getHeight());

			icon.setImage(ILBImageReader.scale(toDraw, scaleThmb));

			if (toDraw != ImageMap.MISSING_IMAGE && toDraw != ImageMap.LOADING)
				if (annotations != null)
					Annotation.paintAnnotations((Graphics2D) icon.getImage().getGraphics(), annotations, scaleAnnot);
				else
					Annotation.paintAnnotations((Graphics2D) icon.getImage().getGraphics(), image.getAnnotations(),
							scaleAnnot);

			// draw a subimage of the image with only the annotation relating
			// specifically to that area.
		} else if (image instanceof Pair) {
			ImageAnnotationPair p = (ImageAnnotationPair) image;

			double scaleAnnot = ILBImageReader.scale(p.y.getWidth(), p.y.getHeight(), ImageHandler.MAX_THUMBNAIL_SIZE,
					ImageHandler.MAX_THUMBNAIL_SIZE);

			// draw annotations on the initial scaled image for simplicity
			if (toDraw != ImageMap.MISSING_IMAGE && toDraw != ImageMap.LOADING)
				Annotation.paintTrimmedAnnotation((Graphics2D) toDraw.getGraphics(), p.y, scaleAnnot);

			BufferedImage finalDraw = ILBImageReader.getScaledInstance(toDraw, getHandler().getThumbnailSize(),
					getHandler().getThumbnailSize(), RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, false);
			icon.setImage(finalDraw);

		}
	}

	/***
	 * Sets the display of metadata about the thumbnail.
	 * <p>
	 * In LIST Mode:
	 * <p>
	 * Display what would normally be the thumbnail's tooltip over to the right
	 * of the thumbnail.
	 * <p>
	 * In GRID & GROUPING Mode:
	 * <p>
	 * Display the image's name centered under its thumbnail (or as much as is
	 * easily displayed and replace the remainder of text with ellipses).
	 * 
	 * @param image
	 *            the Thumbnail that provides the needed data to build the text
	 *            to display
	 */
	private void setText(Thumbnail image) {
		switch (getHandler().getMode()) {
		case LIST:
			getBaseIconHolder().setText(image.toolTipWithoutAnnotation());
			String text;
			MetaImage img = image.getImage();
			for (int i = 0; i < img.getAlternativeAnnotations().size(); i++) {
				text = "<html>" + this.handler.getOptionalListFiles().get(i).getName();
				if (img.getAlternativeAnnotations().size() >= i) {
					if (!img.getDifferences().containsKey(img.getAlternativeAnnotations().get(i))) {
						img.getDifferences().put(img.getAlternativeAnnotations().get(i),
								new AnnotationDifference(img.getAnnotations(), img.getAlternativeAnnotations().get(i)));
					}
					AnnotationDifference ad = img.getDifferences().get(img.getAlternativeAnnotations().get(i));
					text += ".<br>" + ad.getText();
					text += "</html>";
					this.extraImageLabels.get(i).setText(text);
					if (ad.getScore() > Annotation.min_diff){
						this.extraImageLabels.get(i).setBackground(Color.GREEN);
						}
					else{
						this.extraImageLabels.get(i).setBackground(Color.RED);}
				}
			}

			return;
		case GROUPING_IMAGES:
		case GROUPING_ANNOTATIONS:
		case GRID:
		default:
			String name = image.getName();
			name = condenseString(name, getHandler().getThumbnailSize(), "...");
			getBaseIconHolder().setText(name);
			return;
		}

	}

	/**
	 * 
	 * @param text
	 *            the String to condense
	 * @param size
	 *            the length of the string to condense to
	 * @param replaceWith
	 *            the string to replace the deleted part with
	 * @return the given string reduced to fit 2 lines of the given width, with
	 *         the remainder replaced with the given replacement String.
	 */
	private String condenseString(String text, int size, String replaceWith) {
		if (getFont() != null && getFontMetrics(getFont()) != null
				&& getFontMetrics(getFont()).stringWidth(text) > size) {
			int badWidth = getFontMetrics(getFont()).stringWidth(text);
			int breaksToMake = (int) Math.ceil((double) badWidth / size);
			int spacing = text.length() / breaksToMake;
			String[] split = text.split("(?<=\\G.{" + spacing + "})");
			StringBuilder b = new StringBuilder();
			b.append("<html><center>");
			b.append(split[0]);
			b.append("<br>");
			b.append(split[1]);
			if (split.length > 2)
				b.append(replaceWith);
			b.append("</center></html>");
			return b.toString();
		}
		return text;

	}

	/**
	 * reset this for displaying LISTs or GRID/GROUPINGs
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		switch (arg0.getProperty()) {
		case mode:
			removeAll();
			switch ((Mode) arg0.getNewValue()) {
			case LIST:
				prepareForLIST();
				break;
			case GRID:
			case GROUPING_IMAGES:
			case GROUPING_ANNOTATIONS:
			default:
				prepareForGRIDAndGROUP();
				break;
			}
			break;
		default:
			break;
		}
	}

	/**
	 * When in ListMode, there may be extra lists of image annotations to view.
	 */
	List<ImageIcon> extraImages = new ArrayList<>();

	/**
	 * When in ListMode, there may be extra lists of image annotations to view,
	 * and each image should be labeled by it's list, and below it contain
	 * metadata calculated about the alternative list's annotations compared to
	 * the base list
	 */
	private List<JLabel> extraImageLabels = new ArrayList<>();

	/**
	 * Set up stamp for a LIST thumbnail (image to the left, metadata to the
	 * right
	 */
	private void prepareForLIST() {
		removeAll();
		getBasePanel().removeAll();
		getBasePanel().setLayout(new BoxLayout(getBasePanel(), BoxLayout.X_AXIS));

		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		getBasePanel().add(content);

		extraImages.clear();
		extraImageLabels.clear();

		GBC gbc = new GBC(0, 0);
		gbc.setAnchor(GridBagConstraints.WEST);
		content.add(getBaseIconHolder(), gbc);
		// in list view the base icon holder is just the baselabel,
		// the base icon is next to it instead of inside it
		getBaseIconHolder().setIcon(null);
		gbc.gridx++;
		
		String filename = handler.getListFile()==null?"":handler.getListFile().getName();
		
		JLabel baseiconLabel = new JLabel(
				"<html>" + filename + "<br> <br> <br> <br> <br> </html>", getBaseIcon(),
				SwingConstants.CENTER);
		setAlignments(baseiconLabel, SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.BOTTOM,
				SwingConstants.CENTER);

		content.add(baseiconLabel);
		for (File f : handler.getOptionalListFiles()) {
			gbc.gridx++;
			ImageIcon toAdd = new ImageIcon();
			extraImages.add(toAdd);
			JLabel newLabel = new JLabel(f.getName(), toAdd, SwingConstants.CENTER);
			newLabel.setOpaque(true);
			setAlignments(newLabel, SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.BOTTOM,
					SwingConstants.CENTER);
			content.add(newLabel, gbc);
			extraImageLabels.add(newLabel);
		}

		// slight buffer to the left of image
		add(Box.createHorizontalStrut(5));
		add(getBasePanel());
		add(Box.createHorizontalStrut(5));

		// display text to the center right of image
		setAlignments(getBaseIconHolder(), SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.CENTER,
				SwingConstants.CENTER);
	}

	/**
	 * 
	 * @param toSet
	 *            the JLabel to set fields for
	 * @param hsa
	 *            HorizontalAlignment
	 * @param htp
	 *            HorizontalTextPosition
	 * @param vtp
	 *            VerticalTextPosition
	 * @param ay
	 *            AlignmentY
	 */
	private void setAlignments(JLabel toSet, int hsa, int htp, int vtp, int ay) {
		toSet.setHorizontalAlignment(hsa);
		toSet.setHorizontalTextPosition(htp);
		toSet.setVerticalTextPosition(vtp);
		toSet.setAlignmentY(ay);
	}

	/**
	 * set up the stamp for a grid/grouping thumbnail (image cnetered with name
	 * below)
	 */
	private void prepareForGRIDAndGROUP() {
		removeAll();
		getBasePanel().removeAll();
		getBasePanel().setLayout(new BorderLayout());
		getBasePanel().add(getBaseIconHolder(), BorderLayout.CENTER);
		getBaseIconHolder().setIcon(getBaseIcon());

		// center the image
		add(Box.createHorizontalGlue());
		add(getBasePanel());
		add(Box.createHorizontalGlue());

		// center the text and put below image
		getBaseIconHolder().setHorizontalAlignment(SwingConstants.CENTER);
		getBaseIconHolder().setHorizontalTextPosition(SwingConstants.CENTER);
		getBaseIconHolder().setVerticalTextPosition(SwingConstants.BOTTOM);
		getBaseIconHolder().setAlignmentX(SwingConstants.CENTER);
		getBaseIconHolder().setAlignmentY(SwingConstants.CENTER);
	}

	/**
	 * @return the handler that supplies the image and tells when to change the
	 *         display
	 */
	private ImageHandler getHandler() {
		return this.handler;
	}

	/** @return the thumbnail */
	private ImageIcon getBaseIcon() {
		return this.baseIcon;
	}

	/**
	 * @return holder of the thumbnail and displayer of the image name and meta
	 *         data
	 */
	private JLabel getBaseIconHolder() {
		return this.baseIconHolder;
	}

	/** @return base panel for holding image */
	private JPanel getBasePanel() {
		return this.basePanel;
	}
}
