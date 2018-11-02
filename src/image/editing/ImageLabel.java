package image.editing;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JLabel;

import annotations.Annotation;
import ilb.ImageHandler;
import struct.ILBImageReader;
import struct.ImageMap;
import struct.MetaImage;

/***
 * Encapsulates the display of an Image and its Annotations
 * 
 * @author bonifantmc
 *
 */
@SuppressWarnings("serial")
public class ImageLabel extends JLabel {
	/** the original image */
	private BufferedImage image;
	/** metadata about the image to display */
	private MetaImage metaImage;
	/** absolute path to folder/directory the image is in */
	private ImageHandler h;
	/** how much the image should be scaled */
	double scale;
	/** maximum width of picture to display */
	private int maxWidth = 400;
	/** maximum height of picture to display */
	private int maxHeight = 400;

	/**
	 * whatever contianer is holding this ImageLabel (when this ImageLabel's
	 * size changes, so will it's container)
	 */
	private Container[] containers;

	/**
	 * 
	 * @param i
	 *            the metadata about the image to display
	 * @param h
	 *            provides access to the images and their directory.
	 * @param c
	 *            the container holding this ImageLabel, when this ImageLabel
	 *            resizes, so does c (if you don't want the container to resize,
	 *            pass in null)
	 * 
	 * 
	 */
	ImageLabel(MetaImage i, ImageHandler h, Container...c) {
		this.h = h;
		this.containers = c;
		changeImage(i);
	}

	/**
	 * (re)load the image for display and adjust the size of ImageLabel
	 */
	private void reSetImage() {
		// prepare for disposal if the image has been set to null
		// void out the different listeners
		if (this.metaImage == null) {
			this.image.flush();
			this.image = null;
			this.h = null;
			while (this.getKeyListeners().length > 0)
				this.removeKeyListener(this.getKeyListeners()[0]);
			while (this.getMouseListeners().length > 0)
				this.removeMouseListener(this.getMouseListeners()[0]);
			while (this.getMouseMotionListeners().length > 0)
				this.removeMouseMotionListener(this.getMouseMotionListeners()[0]);

			return;
		}

		double width = this.image.getWidth();
		double height = this.image.getHeight();
		this.scale = ILBImageReader.scaleMatch(width, height, maxWidth, maxHeight);
		width *= this.scale;
		height *= this.scale;

		ILBImageReader.scale(this.image, this.scale);

		Dimension d = new Dimension((int) width, (int) height);
		setSize(d);
		setMaximumSize(d);
		setPreferredSize(d);
		setMinimumSize(d);
		for(Container c: containers) {
			c.setSize(d);
			c.setMaximumSize(d);
			c.setPreferredSize(d);
			c.setMinimumSize(d);
		}
		if (this.metaImage.getAnnotations().size() == 0) {
			fm.FaceMatchWorker.getFaces(this.h, this.metaImage);
		}
		this.repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponents(g);

		// full icon size
		int w = getWidth();
		int h = getHeight();

		//
		double width = this.metaImage.getWidth() * this.scale;
		double height = this.metaImage.getHeight() * this.scale;

		// fill entire shape, image may not cover all the area, for instance if
		// the image is really small the space for the close, minimize, maximize
		// buttons will force the panel to be larger
		g.setColor(getBackground());
		g.fillRect(0, 0, w, h);

		// draw the image and its annotations.

		g.drawImage(this.image, 0, 0, (int) width, (int) height, this);
		Annotation.paintAnnotations((Graphics2D) g, this.metaImage.getAnnotations(), this.scale);
	}

	/**
	 * switch the image being displayed to a new one
	 * 
	 * @param i
	 *            the new image to display
	 * 
	 */
	public void changeImage(MetaImage i) {
		this.metaImage = i;
		if (metaImage != null) {
			try {
				this.image = metaImage.readImage();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (this.image == null)
				this.image = ImageMap.MISSING_IMAGE;
		}
		reSetImage();
	}

	/** increase image max size by 10 pixels */
	public void sizeUp() {
		this.maxHeight += 10;
		this.maxWidth += 10;
		if (this.maxHeight > MAX_HEIGHT)
			this.maxHeight = MAX_HEIGHT;
		if (this.maxWidth > MAX_WIDTH)
			this.maxWidth = MAX_WIDTH;

		this.reSetImage();
		this.invalidate();
	}

	/** decrease image max size by 10 pixels */
	public void sizeDown() {
		this.maxHeight -= 10;
		this.maxWidth -= 10;

		if (this.maxHeight < MIN_HEIGHT)
			this.maxHeight = MIN_HEIGHT;
		if (this.maxWidth < MIN_WIDTH)
			this.maxWidth = MIN_WIDTH;
		this.reSetImage();
		this.invalidate();
	}

	/** the image can never exceed this width */
	private static final int MAX_WIDTH = 1024;
	/** the image can never exceed this width */
	private static final int MAX_HEIGHT = 1024;
	/** the image must be at least this width or the MIN_HEIGHT */
	private static final int MIN_WIDTH = 40;
	/** the image must be at least this height or the MIN_WIDTH */
	private static final int MIN_HEIGHT = 40;

}
