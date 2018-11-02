package ilb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;

import annotations.Annotation;
import annotations.Feature;
import fm.FaceFinder;
import struct.MetaImage;

/**
 * A ToolsMenu manages FaceMatch tools and the parameters they can be set with.
 * It allows users to open a dialog for setting FaceFinder configurations.
 * 
 * @author bonifantmc
 *
 */
@SuppressWarnings("serial")
public class ToolsMenu extends JMenu {
	/**
	 * The action launched to call FaceFinder.setParameters to set FaceFinder's
	 * configurations
	 */
	private final Action setFaceFinderParameters = new FaceFinderParameters();
	/** The action launched to call FaceFinder on all images in the list. */
	private final Action faceFindAll = new FaceFinderDetectAll();
	/** The action launched to display the Stats Menu */
	private final Action stats = new StatsMenu();

	/**
	 * The ImageHandler provides access to ILB's display area so when dialogues
	 * are launched they can be centered on ILB
	 */
	private final ImageHandler h;

	/**
	 * Build a ToolsMenu
	 * 
	 * @param h
	 *            the ImageHandler provides access to the display area of ILB so
	 *            when dialogues are opened they can be centered on the ILB
	 *            screen
	 */
	public ToolsMenu(ImageHandler h) {
		this.setText("Tools");
		this.add(this.setFaceFinderParameters);
		this.add(this.faceFindAll);
		this.add(this.stats);
		// this.add(this.setHuman);
		// this.add(this.setAnimal);
		// this.add(this.setAll);
		this.h = h;
	}

	/**
	 * An Action class, when its action is performed it launches the FaceFinder
	 * setParameter's dialogue from the FaceFinder class
	 * 
	 * @author bonifantmc
	 *
	 */
	private class FaceFinderParameters extends AbstractAction {
		/** Constructor that sets the actions name and tooltip */
		FaceFinderParameters() {
			putValue(NAME, "FaceFinder Configuration");
			putValue(SHORT_DESCRIPTION, "Configure FaceMatch's FaceFinder parameters.");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			FaceFinder.setParameters(ToolsMenu.this.h);
		}

	}

	/**
	 * Abstract Action that calls FaceFinder on all images in the list.
	 */
	private class FaceFinderDetectAll extends AbstractAction {

		/** Constructor that sets the action's name and tooltip */
		FaceFinderDetectAll() {
			putValue(NAME, "FaceFinder Detect All");
			putValue(SHORT_DESCRIPTION, "Run FaceFinder on all Images in the list.");

		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			fm.FaceMatchWorker.getFaces(h, h.getMasterList().toArray(new MetaImage[] {}));

		}

	}

	/**
	 * Menu for displaying stats about the list in ILB.
	 * 
	 * @author bonifantmc
	 *
	 */
	private class StatsMenu extends AbstractAction {
		/** Frame to display stats in */
		JFrame statFrame = new JFrame("List Statistics");
		/** content panel of frame */
		JPanel content = new JPanel();
		/** string displaying the stats */
		JLabel stats = new JLabel();
		/** button to refresh the stats */
		JButton refresh = new JButton("Refesh Stats");
		/** button to save the stats to a text file */
		JButton save = new JButton("Save to File");

		/** Constructor that sets the action's name and tooltip */
		StatsMenu() {
			putValue(NAME, "Display Statistics");
			putValue(SHORT_DESCRIPTION, "Display basic statistics about the list");

			refresh.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					setStatsLabel(stats);
				}
			});
			save.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					saveStats();

				}

			});

			content.setLayout(new BorderLayout());
			stats.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			content.add(stats, BorderLayout.CENTER);
			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
			buttons.add(refresh);
			buttons.add(save);
			content.add(buttons, BorderLayout.SOUTH);
			statFrame.setContentPane(content);

		}

		/**
		 * Save the Statistics to a txt file.
		 */
		protected void saveStats() {
			JFileChooser save = new JFileChooser();
			save.setApproveButtonText("Save");
			if (h.getListFile() != null)
				save.setCurrentDirectory(h.getListFileDirecotry());
			File file = null;
			if (save.showOpenDialog(h.getImageDisplay()) == JFileChooser.APPROVE_OPTION)
				file = save.getSelectedFile();
			if (file == null)
				return;

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
				String txt = stats.getText();
				txt = txt.replace("<p>", "");
				txt = txt.replace("</p>", "\n");
				txt = txt.replace("<html>", "");
				txt = txt.replace("</html>", "");
				txt = txt.replace(":", "");
				bw.write(txt);
				bw.flush();
				h.getProgressBar().message.setText("Stats Saved to " + file.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			setStatsLabel(stats);
			statFrame.setLocationRelativeTo(h.getImageDisplay());
			statFrame.setVisible(true);
			statFrame.pack();
		}

		/**
		 * @param stats
		 *            a JLabel, it's text will be set to reflect the results of
		 *            the evaluateList() method (Ie: set to display the stats
		 *            about ILB's current list).
		 */
		private void setStatsLabel(JLabel stats) {
			HashMap<String, Integer> map = evaluateList();
			StringBuilder sb = new StringBuilder();

			// text header
			sb.append("<html>");

			// text content

			sb.append("<p>").append("Images:\t").append(getTally(map, "Images")).append("</p>");
			sb.append("<p>").append("Annotations:\t").append(getTally(map, "Annotations")).append("</p>");
			sb.append("<p>").append("Faces:\t").append(getTally(map, "Face")).append("</p>");
			sb.append("<p>").append("Profiles:\t").append(getTally(map, "Profile")).append("</p>");
			sb.append("<p>").append("Faces + Profiles:\t").append(getTally(map, "Face") + getTally(map, "Profile"))
					.append("</p>");
			sb.append("<p></p><p>Landmarks</p>");
			sb.append("<p>").append("Eyes:\t").append(getTally(map, "Eyes")).append("</p>");
			sb.append("<p>").append("Ears:\t").append(getTally(map, "Ear")).append("</p>");
			sb.append("<p>").append("Noses:\t").append(getTally(map, "Nose")).append("</p>");
			sb.append("<p>").append("Mouths:\t").append(getTally(map, "Mouth")).append("</p>");
			sb.append("<p></p><p>Gender</p>");
			sb.append("<p>").append("Male:\t").append(getTally(map, "gendermale")).append("</p>");
			sb.append("<p>").append("Female:\t").append(getTally(map, "genderfemale")).append("</p>");
			sb.append("<p>").append("Gender Unknown:\t").append(getTally(map, "genderunknown")).append("</p>");
			sb.append("<p>").append("Gender Not Marked:\t").append(getTally(map, "genderunmarked")).append("</p>");
			sb.append("<p></p><p>Skin Tone</p>");
			sb.append("<p>").append("Dark:\t").append(getTally(map, "skindark")).append("</p>");
			sb.append("<p>").append("Light:\t").append(getTally(map, "skinlight")).append("</p>");
			sb.append("<p>").append("Skin Tone Unknown:\t").append(getTally(map, "skinunknown")).append("</p>");
			sb.append("<p>").append("Skin Tone Not Marked:\t").append(getTally(map, "skinunmarked")).append("</p>");
			sb.append("<p></p><p>Age</p>");
			sb.append("<p>").append("Adult:\t").append(getTally(map, "ageadult")).append("</p>");
			sb.append("<p>").append("Youth:\t").append(getTally(map, "ageyouth")).append("</p>");
			sb.append("<p>").append("Age Unknown:\t").append(getTally(map, "ageunknown")).append("</p>");
			sb.append("<p>").append("Age Not Marked:\t").append(getTally(map, "ageunmarked")).append("</p>");
			sb.append("<p></p><p>Wounds</p>");
			sb.append("<p>").append("Blood:\t").append(getTally(map, "woundblood")).append("</p>");
			sb.append("<p>").append("Bruise:\t").append(getTally(map, "woundbruise")).append("</p>");
			sb.append("<p>").append("Scar:\t").append(getTally(map, "woundscar")).append("</p>");
			sb.append("<p>").append("Unwounded:\t").append(getTally(map, "woundunwoundedEyes")).append("</p>");
			sb.append("<p>").append("Wounds Not Marked:\t").append(getTally(map, "woundunmarked")).append("</p>");
			sb.append("<p></p><p>Occulusions</p>");
			sb.append("<p>").append("Glasses:\t").append(getTally(map, "occlusionsglasses")).append("</p>");
			sb.append("<p>").append("Sunglasses:\t").append(getTally(map, "occlusionssunglasses")).append("</p>");
			sb.append("<p>").append("Eye Patch:\t").append(getTally(map, "occlusionspatch")).append("</p>");
			sb.append("<p>").append("Mustache:\t").append(getTally(map, "occlusionsmustache")).append("</p>");
			sb.append("<p>").append("Beard:\t").append(getTally(map, "occlusionsbeard")).append("</p>");
			sb.append("<p>").append("Unoccluded:\t").append(getTally(map, "occlusionsunoccluded")).append("</p>");
			sb.append("<p>").append("Unmarked:\t").append(getTally(map, "occlusionsunmarked")).append("</p>");
			sb.append("<p>").append("Guesses:\t").append(getTally(map, "occlusionsguess")).append("</p>");
			// text footer
			sb.append("</html>");

			// set text
			stats.setText(sb.toString());
		}

		/**
		 * @return a Map of String/Integer Pairs, the string indicates what was
		 *         tallied, and the integer is the tally. eg: an entry might be
		 *         "Males: 35" or "Faces: 64" where "Males: 35" means there were
		 *         35 males in the list, and "Faces: 64" means there were 64
		 *         faces in the list.
		 */
		private HashMap<String, Integer> evaluateList() {
			HashMap<String, Integer> map = new HashMap<>();
			// keys to track.
			for (MetaImage i : h.getMasterList()) {
				this.evaluateAnnotationList(i.getAnnotations(), map);
				addTally(map, "Images");
			}
			return map;
		}

		/**
		 * 
		 * @param lis
		 *            the list of annotations to evaluate
		 * @param map
		 *            the map of attributes being tallied
		 */
		private void evaluateAnnotationList(List<Annotation> lis, Map<String, Integer> map) {
			for (Annotation a : lis)
				evaluateAnnotation(a, map);
		}

		/**
		 * 
		 * @param a
		 *            the annotation to evaluate
		 * @param map
		 *            the map to tally in
		 */
		private void evaluateAnnotation(Annotation a, Map<String, Integer> map) {
			addTally(map, "Annotations");
			evaluateFeature(a.getId(), map);
			evaluateAttributes(a, map);
			for (Annotation sub : a.getSubannotes())
				evaluateFeature(sub.getId(), map);
		}

		/**
		 * 
		 * @param f
		 *            the feature
		 * @param map
		 *            the map where the feature's count is incremented/tallied
		 */
		private void evaluateFeature(Feature f, Map<String, Integer> map) {
			switch (f) {
			case Animal:
				addTally(map, "Animal");
				break;
			case Body:
				addTally(map, "Body");
				break;
			case Ear:
				addTally(map, "Ear");
				break;
			case Eyes:
				addTally(map, "Eyes");
				break;
			case Face:
				addTally(map, "Face");
				break;
			case Head:
				addTally(map, "Head");
				break;
			case Leg:
				addTally(map, "Leg");
				break;
			case Mouth:
				addTally(map, "Mouth");
				break;
			case Nose:
				addTally(map, "Nose");
				break;
			case Profile:
				addTally(map, "Profile");
				break;
			case Skin:
				addTally(map, "Skin");
				break;
			case Tail:
				addTally(map, "Tail");
				break;
			default:
				break;
			}
		}

		/**
		 * @param a
		 *            the annotation whose attributes are being tallied
		 * @param map
		 *            the map to update tallies in
		 * 
		 */
		private void evaluateAttributes(Annotation a, Map<String, Integer> map) {
			evaluateAttribute(a.getAge().getState(), map, "age");
			evaluateAttribute(a.getGender().getState(), map, "gender");
			evaluateAttribute(a.getSkin().getState(), map, "skin");
			evaluateAttribute(a.getWound().getState(), map, "wound");
			evaluateAttribute(a.getOcclusions().getState(), map, "occlusions");

		}

		/**
		 * @param states
		 *            a list of states to increment in the map
		 * @param map
		 *            the map to increment on
		 * @param attribute
		 *            the attribute being tallied
		 */
		private void evaluateAttribute(List<String> states, Map<String, Integer> map, String attribute) {
			for (String state : states)
				addTally(map, attribute + state);
		}

		/**
		 * Increment the tally for the given key in the given map (add the key
		 * and set the initial value to 1 if the key isn't there)
		 * 
		 * @param map
		 *            the map to increment the tally for
		 * @param s
		 *            the key increment
		 */
		private void addTally(Map<String, Integer> map, String s) {
			if (!map.containsKey(s))
				map.put(s, 1);
			else
				map.put(s, map.get(s) + 1);
		}

		/**
		 * 
		 * @param map
		 *            the map to look up the tally in
		 * @param s
		 *            the key for the tally to look up
		 * @return the tally for key s in HashMap map, or 0 if tally not in map
		 */
		private int getTally(HashMap<String, Integer> map, String s) {

			if (map.containsKey(s))
				return map.get(s);
			else
				return 0;
		}

	}

}
