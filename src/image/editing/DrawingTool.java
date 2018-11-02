package image.editing;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import annotations.Annotation;
import annotations.Attribute;
import annotations.Category;
import annotations.Feature;
import struct.ArrayListModel;
import struct.ImageAnnotationPair;
import struct.MetaImage;

/**
 * 
 * 
 * Mouse(Motion)listeners for tracking Annotations as they're drawn on the
 * ImageLabel
 * 
 * @author bonifantmc
 *
 */

public class DrawingTool extends MouseAdapter {

	/** The EditImage using this adapter */
	private final EditImage editImage;

	/** The Feature about to be or being drawn */
	private Feature drawingFeature = Feature.Face;

	/** The Annotation currently being drawn */
	private Annotation tempAnnotation;

	/** Parent of the Annotation currently being drawn, or null if none exist */
	private Annotation parent;

	/**
	 * The point where a user clicked when they started a drag operation.
	 */
	private Point2D.Double anchor = new Point2D.Double(0, 0);

	/** true if editing the start/end of an annotation */
	private boolean editingExistingAnnotation = false;

	/** true if a new annotation is being made */
	private boolean makingNewAnnotation = false;

	/** true if an old annotation is being dragged to relocate it */
	private boolean draggingExistingAnnotation = false;

	/**
	 * 
	 * 
	 * @param editImage
	 *            this DrawingTools EditImage. The DrawingTool uses the
	 *            EditImage so it can track the cursors movement over the
	 *            EditImage and to refresh both it and its associated thumbnail
	 *            in the ILB
	 */
	public DrawingTool(EditImage editImage) {
		this.editImage = editImage;
	}

	/**
	 * Pressing the mouse can have three results:
	 * <p>
	 * right-clicking: display a pop-up menu (when over an image display
	 * selections for Annotation Attributes, when not display selections for
	 * Feature)
	 * <p>
	 * left-click: Begin drawing an annotation double-left-click: delete an
	 * annotation
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		getEditImage().getImageLabel().requestFocusInWindow();

		Point2D.Double pt = scalePoint(to2DPoint(e.getPoint()));
		ArrayListModel<Annotation> list = getEditImage().getMetaImage().getAnnotations();

		// SINGLE CLICK FOR DRAWING/EDITING ANNOTATIONS
		if (e.getClickCount() == 1) {
			// CHECK FOR RESIZING AN ANNOTATION
			Annotation a = Annotation.nearAnnotation(pt, 7, list);
			if (a != null) {
				setTempAnnotation(a);
				setEditingExistingAnnotation(true);
				Point2D.Double[] arr = a.getCorners();
				double dist = Integer.MAX_VALUE;
				int i = 0;
				int j = 0;
				for (Point2D.Double pA : arr) {
					double t = pA.distance(pt);
					if (t < dist) {
						dist = t;
						j = i;
					}
					i++;
				}
				// set anchor to the corner opposite where the mouse is.
				// arr ={upperRight, upperLeft, lowerLeft, lowerRight} so
				// ur=0->ll=2, ul=1->lr=3, etc.
				setAnchor(j == 0 ? arr[2] : j == 1 ? arr[3] : j == 2 ? arr[0] : arr[1]);

			} else

			// CHECK FOR MOVING AN ANNOTATION
			if (
			// 1) when clicking in a annotation of the same type you're set to
			// draw
			(((a = Annotation.getAnnotationAtPoint(pt, list)) != null) && a.getId() == this.getDrawingFeature()) ||
			// 2) when trying to draw a sub-feature in a sub-feature, provided
			// the parent feature is not a head
					(a != null && a.getId() != Feature.Head && a.getId().isSubfeature())

			) {
				setTempAnnotation(a);
				setDraggingExistingAnnotation(true);
				setAnchor(pt);
			}
			// ELSE DRAWING A NEW ANNOTATION
			else {
				setAnchor(pt);
				startDrawingAnnotation();
			}
			// MULTIPLE CLICKS FOR DELETING ANNOTATIONS
		} else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
			Annotation a = Annotation.removeAnnotationAtPoint(pt, list);
			if (a != null && a.getParent() == null) {
				ImageAnnotationPair pair = new ImageAnnotationPair(editImage.getMetaImage(), a);
				editImage.getHandler().getAnnotationGroups().removePair(pair);

				cleanMasterSet();
			} else
				System.err.println("ODD 1");
		} else
			System.err.println("ODD 2");
	}

	/**
	 * make sure delelting an annotation didn't mess up the MasterList or
	 * Groupings
	 */
	public void cleanMasterSet() {
		getEditImage().h.sort();
		getEditImage().repaint();
		getEditImage().h.setMasterListChanged(true);
	}

	/**
	 * Handles altering the annotation as it is formed by left-mouse drags (also
	 * updates the tooltip)
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
		// do nothing for right drags
		if (SwingUtilities.isRightMouseButton(e))
			return;

		Point2D.Double end = scalePoint(to2DPoint(e.getPoint()));

		// handle making a new annotation
		if (isMakingNewAnnotation() || isEditingExistingAnnotation()) {
			getTempAnnotation().setRect(getAnchor(), end,
					getTempAnnotation().getParent() == null ? getFrame() : getTempAnnotation().getParent());
		} else if (isDraggingExistingAnnotation()) {
			// calculate translation distance
			Point2D.Double dist = new Point2D.Double(end.x - getAnchor().x, end.y - getAnchor().y);
			// apply translation
			if (getTempAnnotation().getParent() == null)
				getTempAnnotation().translate(dist, getFrame());
			else
				getTempAnnotation().translate(dist, getTempAnnotation().getParent());
			// reset anchor or you'll add more than intended next drag and
			// accelerate off screen
			setAnchor(end);
		}

		// update tool tip
		String oldText = getEditImage().getImageLabel().getToolTipText();
		getEditImage().getImageLabel()
				.setToolTipText("<html><body style='width: 50px'>" + getTempAnnotation().toString());
		if (oldText != null && !oldText.equals(getEditImage().getImageLabel().getToolTipText())) {
			ToolTipManager.sharedInstance()
					.mouseMoved(new MouseEvent(getEditImage().getImageLabel(), -1, System.currentTimeMillis(), 0,
							e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(), 0, false, 0));
		}

		// refresh the annotation
		getEditImage().repaint();
	}

	/**
	 * Finalizes the annotation currently being drawn.
	 * <p>
	 * Ensures the annotation (not sub-annotations though) obey the requirements
	 * defined by Annotation.MIN_AREA & Annotation.MIN_ASPECT_RATIO. If either
	 * of these conditions is not met, the annotation is removed.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		// add to undo stack
		Annotation.removedTooSmallAnnotations(getEditImage().getMetaImage().getAnnotations());
		cleanMasterSet();
		getEditImage().getAttributeMenu().setSelectedAnnotation(this.getTempAnnotation());

		setParent(null);
		setDraggingExistingAnnotation(false);
		setEditingExistingAnnotation(false);
		setMakingNewAnnotation(false);
		for (Annotation a : getEditImage().getMetaImage().getAnnotations())
			Annotation.extendAndClip(a, getFrame());
		getEditImage().repaint();
		getEditImage().h.getImageDisplay().repaint();

		getEditImage().snap();
	}

	/**
	 * Update's this tool's EditImage's tooltip as mouse moves
	 */
	@Override
	public void mouseMoved(MouseEvent e) {
		Point2D.Double check = scalePoint(to2DPoint(e.getPoint()));
		String text = getEditImage().getImageLabel().getToolTipText();
		String newText = null;
		for (Annotation a : getEditImage().getMetaImage().getAnnotations()) {
			String ret = a.getToolTip(check);
			if (ret != null) {
				newText = "<html><body style='width: 50px'>" + ret;
				break;
			}
		}
		getEditImage().getImageLabel().setToolTipText(newText);
		if (text == null || !text.equals(getEditImage().getImageLabel().getToolTipText()))
			ToolTipManager.sharedInstance()
					.mouseMoved(new MouseEvent(getEditImage().getImageLabel(), -1, System.currentTimeMillis(), 0,
							e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(), 0, false, 0));

		Annotation.nearAnnotation(check, 7, getEditImage().getMetaImage().getAnnotations());
		getEditImage().repaint();
	}

	/**
	 * Set the new temporary annotation to draw and check if it has a parent
	 */
	void startDrawingAnnotation() {
		// reset temp with new start coord
		// only set a parent if it's a subfeature
		if (getDrawingFeature().isSubfeature()) {
			// potential parent annotatioins
			List<Annotation> arr = Annotation.getAnnotationsAtPoint(getAnchor(),
					getEditImage().getMetaImage().getAnnotations());

			// no potential parent's means no subfeatures to draw
			if (arr.size() == 0)
				return;

			// only faces/heads/profiles/skin can have eyes/ears/nose/mouth
			if (this.getDrawingFeature().isFacialFeature()) {
				// set parent as first valid annotation found.
				for (Annotation a : arr)
					if (a.getId() == Feature.Face || a.getId() == Feature.Head || a.getId() == Feature.Profile
							|| a.getId() == Feature.Skin) {
						this.setParent(a);
						break;
					}
			} else

			// animal features can only be sub-d from the animal feature
			if (this.getDrawingFeature().isAnimalFeature()) {
				for (Annotation a : arr)
					if (a.getId().isAnimal()) {
						setParent(a);
						break;
					}

			} else
				System.err.println("Odd error, face features were covered and animal features, that's all there is");

			// if no valid parent can't draw a sub-feature return early
			if (getParent() == null) {
				System.err.println("Invalid parent: feature type: " + getDrawingFeature());
				return;
			}
		}

		// initialize the anotation to draw
		setTempAnnotation(new Annotation(getDrawingFeature(), getAnchor().x, getAnchor().y, 0, 0, getParent(),
				Attribute.UNMARKED, Attribute.UNMARKED, Attribute.UNMARKED, Attribute.UNMARKED, Attribute.UNMARKED,
				Attribute.UNMARKED, Category.BREED_UNKNOWN.toString(), Category.UNTAGGED.toString()));

		// if the new annotation does not have a parent, add it to the
		// metaimage's annotation list.
		if (!getDrawingFeature().isSubfeature()) {
			getEditImage().getMetaImage().getAnnotations().add(getTempAnnotation());
		} else
			System.err.println("not adding to main list");

		setMakingNewAnnotation(true);

	}

	/** @return the EditImage parent of this tool */
	EditImage getEditImage() {
		return this.editImage;
	}

	/** @return the Annotation being currently drawn */
	Annotation getTempAnnotation() {
		return this.tempAnnotation;
	}

	/**
	 * @param annotation
	 *            the Annotation currently being drawn will be set to this
	 */
	void setTempAnnotation(Annotation annotation) {
		this.tempAnnotation = annotation;
	}

	/**
	 * @return the initial point of an Annotation the user started to draw from
	 */
	Point2D.Double getAnchor() {
		return this.anchor;
	}

	/**
	 * @param p
	 *            the initial point of an Annotation the user started to draw
	 */
	void setAnchor(Point2D.Double p) {
		this.anchor = p;
	}

	/**
	 * @return If the tempAnnotation is a sub-annotation this returns its
	 *         parent, otherwise it returns null
	 */
	Annotation getParent() {
		return this.parent;
	}

	/**
	 * set the parent of the tempAnnotation, indicating the current temp
	 * Annotation is a sub-Annotation
	 * 
	 * @param annotation
	 *            the parent annotation to set
	 */
	void setParent(Annotation annotation) {
		this.parent = annotation;
	}

	/** @return the type of the current tempgAnnotation */
	Feature getDrawingFeature() {
		return this.drawingFeature;
	}

	/***
	 * @param Feature
	 *            the type to set the current tempAnnotation
	 */
	void setDrawingFeature(Feature Feature) {
		this.editImage.getAttributeMenu().featureType.setSelectedItem(Feature);
		this.drawingFeature = Feature;
	}

	/**
	 * 
	 * @param pt
	 *            the plain blah point to convert to a 2D Double point
	 * @return the converted 2D Double Point
	 */
	static Point2D.Double to2DPoint(Point pt) {
		if (pt == null)
			return null;
		return new Point2D.Double(pt.x, pt.y);
	}

	/**
	 * scale a point based on this Tool's editImage's scale
	 * 
	 * @param point
	 *            the point to scale
	 * @return the scaled point
	 */
	private Point2D.Double scalePoint(Point2D point) {
		Point2D.Double p = new Point2D.Double();
		p.x = point.getX() / getEditImage().getImageLabel().scale;
		p.y = point.getY() / getEditImage().getImageLabel().scale;
		return p;
	}

	/** @return true if currently editing an existing Annotation */
	private boolean isEditingExistingAnnotation() {
		return this.editingExistingAnnotation;
	}

	/**
	 * @param editingExistingAnnotation
	 *            whether an Annotation is being edited or not
	 */
	private void setEditingExistingAnnotation(boolean editingExistingAnnotation) {
		this.editingExistingAnnotation = editingExistingAnnotation;
	}

	/** @return true if moving an existing annotation */
	private boolean isDraggingExistingAnnotation() {
		return this.draggingExistingAnnotation;
	}

	/**
	 * @param draggingExistingAnnotation
	 *            should be true if dragging current annotation, else false
	 */
	private void setDraggingExistingAnnotation(boolean draggingExistingAnnotation) {
		this.draggingExistingAnnotation = draggingExistingAnnotation;
	}

	/** @return true if drawing a new Annotation */
	private boolean isMakingNewAnnotation() {
		return this.makingNewAnnotation;
	}

	/**
	 * @param makingNewAnnotation
	 *            true if drawing a new annotation false otherwise
	 */
	private void setMakingNewAnnotation(boolean makingNewAnnotation) {
		this.makingNewAnnotation = makingNewAnnotation;
	}

	/** @return a Rectangle2D of the size of the window this tool works with */
	private Rectangle2D.Double getFrame() {
		Point2D.Double end = scalePoint(new Point2D.Double(this.getEditImage().getImageLabel().getWidth(),
				this.getEditImage().getImageLabel().getHeight()));
		return new Rectangle2D.Double(0, 0, end.x, end.y);
	}

	/**
	 * 
	 * @param i
	 *            the new MetaImage to work with
	 */
	public void changeImage(MetaImage i) {
		// TODO Auto-generated method stub

	}

	/**
	 * Changes the DrawingFeature and if the mouse is over an Annotation
	 * attempts to change the Annotations Feature as well
	 * 
	 * @param f
	 *            the new DrawingFeature
	 */
	public void changeDrawingFeature(Feature f) {
		this.setDrawingFeature(f);
		Point2D.Double p = to2DPoint(getEditImage().getImageLabel().getMousePosition());
		// mouse not in component
		if (p == null)
			return;
		// change an existing annotations type, if control is down and mouse
		// over an annotation

		p = scalePoint(p);
		// cycle to find an annotation at point p
		for (Annotation a : getEditImage().getMetaImage().getAnnotations())
			if (a.contains(p)) {
				if (f.isSubfeature()) {
					for (Annotation aa : a.getSubannotes())
						if (aa.contains(p)) {
							aa.setId(f);
							getEditImage().repaint();
							getEditImage().snap();
						}
				} else {
					a.setId(f);
					getEditImage().repaint();
					getEditImage().snap();
				}
			}

	}

}
