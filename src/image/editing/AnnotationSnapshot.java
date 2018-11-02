package image.editing;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Stack;

import annotations.Annotation;
import annotations.Feature;
import struct.MetaImage;
import struct.Property;

/**
 * 
 * A snapshot of this Tool at one instance in time.
 * 
 * @author Girish
 * 
 */

class UndoRedo {
	/** stack of previous states */
	private Stack<AnnotationSnapshot> undo = new Stack<>();
	/** stack of states undone that can be redone */
	private Stack<AnnotationSnapshot> redo = new Stack<>();
	/** the image to monitor */
	private EditImage image;

	/**
	 * @param image
	 *            the image this UndoRedo tracks
	 */
	public UndoRedo(EditImage image) {
		this.image = image;
		takeSnapshot();
	}

	/**
	 * take a snapshot of the current state of the EditImage
	 */
	public void takeSnapshot() {
		this.redo.clear();
		this.undo.push(new AnnotationSnapshot(this.image));
	}

	/**
	 * Undo the last action done
	 */
	public void undo() {
		if (this.undo.size() > 1) {
			this.redo.push(this.undo.pop());
			this.image.setState(this.undo.peek());
		} else
			this.image.setState(this.undo.peek());
	}

	/** redo the last action undone */
	public void redo() {
		if (this.redo.size() > 1) {
			AnnotationSnapshot newState = this.redo.pop();
			this.image.setState(newState);
			this.redo.push(newState);
		}

	}

	/**
	 * Tracks the state of an EditImage
	 * 
	 * @author bonifantmc
	 *
	 */
	static class AnnotationSnapshot {
		/** this snapshot's list of Annotations at one instance in time */
		final ArrayList<Annotation> annotations;
		/** the current Annotation being edited at one instance in time */
		final Annotation snapTempAnnotation;
		/** the current Feature being edited at one instance in time */
		final Feature snapDrawingFeature;
		/** the parent of tempAnnotation at one instance in time */
		final Annotation snapParent;
		/** the start point of tempAnnotation at one instance in time */
		final Point2D.Double snapStart;

		/**
		 * 
		 * @param image
		 *            the image to snapshot
		 */
		private AnnotationSnapshot(EditImage image) {

			this.annotations = Annotation.cloneList(image.getMetaImage().getAnnotations());
			this.snapTempAnnotation = image.getDrawingTool().getTempAnnotation() == null ? null
					: image.getDrawingTool().getTempAnnotation().clone();
			this.snapDrawingFeature = image.getDrawingTool().getDrawingFeature();
			this.snapParent = image.getDrawingTool().getParent();
			this.snapStart = image.getDrawingTool().getAnchor();
		}
	}

	/**
	 * Update the undo/redo stacks for a new MetaImage
	 * 
	 * @param i
	 *            the new MetaImage
	 */
	public void changeImage(MetaImage i) {
		this.undo.clear();
		this.redo.clear();
		if (i != null)
			this.takeSnapshot();
		image.getHandler().firePropertyChange(Property.annotationGroups, null);

	}

	/** call undo until the EditImage reverts to it's original state. */
	public void revert() {
		while (this.undo.size() > 1)
			undo();
		undo();
	}
}