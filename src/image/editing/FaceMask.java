package image.editing;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.interactivemesh.jfx.importer.ModelImporter;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;

import annotations.Annotation;
import generic.components.UpDownScrollerFloat;
import image.editing.AttributeMenu.AnnotationTab;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import struct.AdjutsableFloat;
import struct.AdjutsableFloat.FloatAdjustListener;
import struct.MetaImage;

/**
 * A 3D model of a face that get's overlaid on annotations while editing the
 * roll/picth/yaw to better fit/find the roll/picth/yaw
 * 
 * @author bonifantmc
 *
 */
public class FaceMask extends JFXPanel {

	/** the face */
	final Group face = new Group();
	/** The EditImage this FaceMask is associated with */
	final EditImage editImage;
	/***
	 * Possible face models: >face_obj/Pasha_guard_head.obj
	 * >face_obj/Basehead.obj >face_objMale Base Head_obj.obj
	 * >face_obj/INGAME_BASE.OBJ
	 * 
	 * 
	 */
	final public String facePath = "face_obj/Basehead.obj";
	
	/** the default scale to resize the FaceMask to*/
	public static final float defaultScale = 50f;
	
	/**the default translucency of the FaceMask*/
	public static final float defaultTranslucency = 0.75f;

	/** a scale factor in adjusting the size of the FaceMask */
	public static AdjutsableFloat scale = new AdjutsableFloat(defaultScale);
	/** how translucent the FaceMask is */
	public static AdjutsableFloat translucency = new AdjutsableFloat(defaultTranslucency, 0f, 1f);

	/** up down arrows to adjust the scale of the FaceMask */
	final UpDownScrollerFloat scaleWatcher = new UpDownScrollerFloat(scale, 1f);
	/** up down arrows to adjust how translucent of the FaceMask */
	final UpDownScrollerFloat translucencyWatcher = new UpDownScrollerFloat(translucency, 0.01f);

	/**
	 * @param editImage
	 *            the editimage this FaceMask is associated with
	 */
	public FaceMask(EditImage editImage) {
		this.editImage = editImage;
		scale.addListener(new FloatAdjustListener() {
			@Override
			public void onAdjust() {
				scale(editImage.getImageLabel().scale);
			}
		});
		translucency.addListener(new FloatAdjustListener() {
			@Override
			public void onAdjust() {
				repaint();
			}
		});

	}

	/**
	 * Update the maximum boundary of the FaceMask based on the size of the
	 * ImageLabel in the EditImage
	 */
	protected void updateBounds() {
		this.face.maxWidth(editImage.getImageLabel().getWidth());
		this.face.maxHeight(editImage.getImageLabel().getHeight());
		this.scale.adjust(0.0f);
	}

	/**
	 * Initialize the FaceMask
	 */
	protected void init() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFX();
			}
		});
	}

	/** Initialize the FaceMask */
	private void initFX() {
		// This method is invoked on the JavaFX thread
		Scene scene = createScene();
		this.setScene(scene);
		updateBounds();
	}

	/**
	 * @return load the actual 3D Model of the Face for display and build the
	 *         scene
	 */
	private Scene createScene() {
		Group root = new Group();
		Scene scene = new Scene(root, Color.TRANSPARENT);
		Node[] faceParts = loadMesh(facePath);
		face.getChildren().addAll(faceParts);

		root.getChildren().add(face);

		face.setOnMouseDragged(new FaceMouseHandler());
		face.setOnScroll(new FaceScrollHandler());
		double x = editImage.getImageLabel().getWidth() / 2;
		double y = editImage.getImageLabel().getHeight() / 2;

		this.face.setTranslateX(x);
		this.face.setTranslateY(y);
		return (scene);
	}

	/**
	 * Load a face from a Wavefront .obj file, (which is a list of lots and lots
	 * of coordinates and data, with 5 types of data/coordinates:
	 * <p>
	 * v: geometric coordinates in form 'v x y z[ w]'
	 * <p>
	 * vt: texture coordinates in the form 'vt u v[ w]'
	 * <p>
	 * vb: vertex normals in the form 'vn x y z'
	 * <p>
	 * vp: parameter space verices in the form 'vp u[ v][ w]'
	 * <p>
	 * f: polygonal face element in the form 'f [numbers
	 * 
	 * @param meshFilePath
	 *            the path to the Mesh to load, must be a WaveFront .obj file.
	 * 
	 * @return the face as a Node to add to the scene
	 */
	private static Node[] loadMesh(String meshFilePath) {
		ModelImporter importer = new ObjModelImporter();
		importer.read(meshFilePath);
		Node[] obj = (Node[]) importer.getImport();
		return obj;
	}

	/**
	 * @param annotation
	 *            translate and rotate the Face & axes according to the given
	 *            annotation
	 */
	public void setFace(Annotation annotation) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				updateFace(annotation);
			}

		});

	}

	/**
	 * @param annotation
	 *            translate and rotate the Face & axes according to the given
	 *            annotation
	 * 
	 */
	public void updateFace(Annotation annotation) {

		// calculate all the math, getting the elements of the rotation matrix
		// to rotate the face and axis
		double drawScale = this.editImage.getImageLabel().scale;

		double alf = Math.toRadians(annotation.getRoll()), bet = Math.toRadians(annotation.getPitch()),
				gam = Math.toRadians(annotation.getYaw());
		double A11 = Math.cos(alf) * Math.cos(gam);
		double A12 = Math.cos(bet) * Math.sin(alf) + Math.cos(alf) * Math.sin(bet) * Math.sin(gam);
		double A13 = Math.sin(alf) * Math.sin(bet) - Math.cos(alf) * Math.cos(bet) * Math.sin(gam);
		double A21 = -Math.cos(gam) * Math.sin(alf);
		double A22 = Math.cos(alf) * Math.cos(bet) - Math.sin(alf) * Math.sin(bet) * Math.sin(gam);
		double A23 = Math.cos(alf) * Math.sin(bet) + Math.cos(bet) * Math.sin(alf) * Math.sin(gam);
		double A31 = Math.sin(gam);
		double A32 = -Math.cos(gam) * Math.sin(bet);
		double A33 = Math.cos(bet) * Math.cos(gam);

		double d = Math.acos((A11 + A22 + A33 - 1d) / 2d);
		if (d != 0d) {
			double den = 2d * Math.sin(d);
			Point3D p = new Point3D((A32 - A23) / den, (A13 - A31) / den, (A21 - A12) / den);
			this.face.setRotationAxis(p);
			this.face.setRotate(Math.toDegrees(d));
		} else
			this.face.setRotate(0);
		scale(drawScale);
	}

	/**
	 * 
	 * @param ox
	 *            origin x
	 * @param oy
	 *            origin y
	 * @param x
	 *            mouse location x
	 * @param y
	 *            mouse location y
	 * @param annotation
	 *            the annotation the yaw and pitch will be updated for (since
	 *            it's a 2d plane the roll can't be included)
	 */
	public void rotate(double ox, double oy, double x, double y, Annotation annotation) {
		double w = this.editImage.getImageLabel().getWidth(), h = this.editImage.getImageLabel().getHeight();
		double dx = x - ox;
		double dy = y - oy;

		dx /= w / 2;
		dy /= h / 2;

		dx *= 180;
		dy *= -180;

		String yaw = String.format("%.1f", dx);
		String pitch = String.format("%.1f", dy);

		annotation.setPitch(Float.parseFloat(pitch));
		annotation.setYaw(Float.parseFloat(yaw));
		AnnotationTab at = (AnnotationTab) editImage.getAttributeMenu().getTabs().getSelectedComponent();

		if (!at.yaw.getText().equals(yaw)) {
			at.yawedit = false;
			at.yaw.setText(yaw);
			at.yawedit = true;
		}
		if (!at.pitch.getText().equals(pitch)) {
			at.pitchedit = false;
			at.pitch.setText(pitch);
			at.pitchedit = true;
		}

		updateFace(annotation);

	}

	/**
	 * @param scale
	 *            value to scale by
	 */
	private void scale(double scale) {
		this.face.setScaleX(scale * this.scale.getFloat());
		this.face.setScaleY(scale * this.scale.getFloat());
		this.face.setScaleZ(scale * this.scale.getFloat());
	}

	/** @return GUI component to adjust this FaceMask's faceScale value */
	public Component adjustFaceScale() {
		return this.scaleWatcher;
	}

	/** @return GUI component to adjust this FaceMask's faceScale value */
	public Component adjustFaceTransparency() {
		return this.translucencyWatcher;
	}

	/**
	 * @author bonifantmc
	 *
	 */
	private class FaceMouseHandler implements EventHandler<MouseEvent> {

		@Override
		public void handle(MouseEvent event) {
			if (editImage.getAttributeMenu().getSelectedAnnotation() != null) {
				double x = event.getSceneX();
				double y = event.getSceneY();

				if (event.isPrimaryButtonDown()) {
					double ox, oy;
					ox = face.getTranslateX();
					oy = face.getTranslateY();
					rotate(ox, oy, x, y, editImage.getAttributeMenu().getSelectedAnnotation());
				} else if (event.isSecondaryButtonDown()) {
					face.setTranslateX(x);
					face.setTranslateY(y);
				} else if (event.isMiddleButtonDown()) {
					// TODO (Not a to-do just a note, middle down is handled by
					// the FaceScrollHandler, since the middle button is
					// typically the mouse wheel used for scrolling.
				}
			}
		}
	}

	/**
	 * The FaceScrollHandler checks for scroll movements while adjusting the
	 * FaceMask3D Object If a scroll event happens with the Control modifier
	 * pressed, the translucency will increase/decrease accordingly, otherwise
	 * the scale/size of the FaceMask will adjust accordingly growing or
	 * shrinking the mask. If shift is pressed, then adjust the annotations'
	 * roll.
	 * 
	 * @author bonifantmc
	 */
	private class FaceScrollHandler implements EventHandler<ScrollEvent> {
		/** scale increment */
		private float incS = 3f;
		/** translucency increment */
		private float incT = 0.03f;
		/** roll increment */
		private float incR = 1f;

		@Override
		public void handle(ScrollEvent event) {
			Annotation note = editImage.getAttributeMenu().getSelectedAnnotation();
			if (note != null)
				if (event.isControlDown()) {
					FaceMask.this.translucency.adjust(event.getDeltaY() > 0 ? incT : -incT);
				} else if (event.isShiftDown()) {
					AnnotationTab at = (AnnotationTab) editImage.getAttributeMenu().getTabs().getSelectedComponent();
					note.setRoll(note.getRoll() + (event.getDeltaY() > 0 ? incR : -incR));
					if (!at.roll.getText().equals(note.getRoll())) {
						at.rolledit = false;
						at.roll.setText(Float.toString(note.getRoll()));
						at.rolledit = true;
					}
				} else {
					FaceMask.this.scale.adjust(event.getDeltaY() > 0 ? incS : -incS);
				}
		}
	}

	/**
	 * @param g
	 *            the graphics to display
	 */
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, this.translucency.getFloat());
		g2d.setComposite(alpha);
		super.paintComponent(g);

	}

	/**
	 * @param metaImage
	 *            the new MetaImage to adjust the face to orient around
	 */
	public void changeImage(MetaImage metaImage) {
		if (this.editImage.getAttributeMenu().getSelectedAnnotation() != null)
			this.updateFace(this.editImage.getAttributeMenu().getSelectedAnnotation());
		updateBounds();
	}
}
