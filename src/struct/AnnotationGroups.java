package struct;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.swing.JMenuItem;

import annotations.Annotation;
import fm.FaceMatchImageMatchForm;
import ilb.ImageHandler;

/**
 * A table sorting MetaImage's by their annotation's category
 * 
 * @author bonifantmc
 */
public class AnnotationGroups implements Group {
	/** the category name for unannotated images */
	public static final String UNANNOTATED = "Unannotated";
	/** the category name for annotated images, without tags */
	public static final String UNTAGGED = "Untagged";
	/***/
	private final ImageHandler h;

	/** a map of all */
	private final Map<String, NLMSThumbnails> map;

	/**
	 * @param handler
	 *            image source for the table
	 */
	public AnnotationGroups(ImageHandler handler) {
		this.h = handler;
		Collator c = Collator.getInstance(Locale.US);
		c.setStrength(Collator.TERTIARY);
		map = new TreeMap<>(c);
	}

	/**
	 * (Re)Create the table based on the Handler's list, adding each Image
	 * paired with its annotation to that annotation's tag's list.
	 * 
	 */
	@Override
	public void build() {

		long t = System.currentTimeMillis();

		clear();

		for (MetaImage m : h.getMasterList())
			if (m.getAnnotations().size() == 0)
				addThumbnail(UNANNOTATED, new ImageAnnotationPair(m, null));
			else
				for (Annotation a : m.getAnnotations())
					if (a.getCategory().toString().equals(UNTAGGED))
						addThumbnail(UNTAGGED, new ImageAnnotationPair(m, a));
					else
						addThumbnail(a.getCategory().toString(), new ImageAnnotationPair(m, a));

		System.out.println("annotation group build done: " + (System.currentTimeMillis() - t) + "ms");
		h.firePropertyChange(getProperty(), null);

	}

	/**
	 * Rebuild the table, so that each image is categorized by the first
	 * prefixLength characters of its name. This recategorizes/tags all
	 * annotations an image has
	 * 
	 * @param prefix
	 *            the length of the prefixes to make
	 */
	@Override
	public void prefixSort(int prefix) {
		if (prefix < 0)
			return;

		for (MetaImage i : h.getMasterList())
			if (i.getAnnotations().size() > 0) {
				String id;
				if (prefix > i.getName().length())
					id = i.getName().substring(0, i.getName().length());
				else
					id = i.getName().substring(0, prefix);

				if (i.getAnnotations().size() == 1) {
					i.getAnnotations().get(0).getCategory().setIDString(id);
				} else {
					int cnt = 0;
					for (Annotation a : i.getAnnotations()) {
						a.getCategory().setIDString(id + "." + cnt);
						cnt++;
					}

				}
			}

		build();
		h.setMasterListChanged(true);
		h.firePropertyChange(Property.imageGroups, null);

	}

	/**
	 * Rebuild the table from a given list of images, and a given regular
	 * expression, those with annotations are grouped according to the regular
	 * expression (those that don't match it are put in "Failed Pattern Match",
	 * those that are unannotated remain in unannotated). If an image has more
	 * than one Annotation all annotations will be given the same new tag.
	 * 
	 * @param regex
	 *            a regular expression
	 */
	@Override
	public void regexSort(String regex) {
		// adjust all MetaImage's annotations's categories based on the given
		// regex and the MetaImage's name.
		Pattern p = Pattern.compile(regex);

		for (MetaImage i : h.getMasterList())
			if (i.getAnnotations().size() > 0) {
				String prefix = i.getGroupsString(p);

				if (i.getAnnotations().size() > 1) {
					int cnt = 0;
					for (Annotation a : i.getAnnotations()) {
						a.setCategory(prefix + "." + cnt);
						cnt++;
					}
				} else
					i.getAnnotations().get(0).setCategory(prefix);
			}
		// rebuild the set
		build();
		h.setMasterListChanged(true);
		h.firePropertyChange(Property.imageGroups, null);
	}

	/**
	 * rebuild the table, categorizing and tagging images based on whether they
	 * match a given glob expression.
	 * 
	 * @param glob
	 *            a glob pattern with an escaped group to match the files
	 *            against
	 */
	@Override
	public void globSort(String glob) {
		regexSort(Globs.toRegexPattern(glob));
	}

	/**
	 * 
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("{\n");
		for (NLMSThumbnails set : this.map.values())
			s.append(set.toString() + "\n");
		return s.toString() + "}";
	}

	/**
	 * * Fancy get that adds the key and creates a new list, if object doesn't
	 * exist in map
	 * 
	 * @param key
	 *            the key value being looked up
	 * @return the list the key points to
	 */
	public NLMSThumbnails get(String key) {
		NLMSThumbnails ret = this.map.get(key);
		if (ret == null)
			this.map.put(key, new NLMSThumbnails(key));
		return this.map.get(key);
	}

	/**
	 * Remove all lists that have size 0
	 */
	public void cleanUp() {
		Iterator<String> i = map.keySet().iterator();
		while (i.hasNext())
			if (map.get(i).size() == 0)
				map.remove(i);
	}

	/**
	 * Checks if a MetaImage is mapped or not
	 * 
	 * @param i
	 *            the MetaImage to check
	 * @return true if the MetaImage is somewhere in the map
	 */
	public boolean mapsImage(MetaImage i) {
		for (NLMSThumbnails set : this.map.values())
			for (Thumbnail img : set)
				if (i == img.getImage())
					return true;
		return false;
	}

	/**
	 * rebuild the table with indices as labels form 0 to n-1 the number of
	 * lists - 1
	 */
	public void buildReindex() {

		build();
		HashMap<String, NLMSThumbnails> temp = new HashMap<>();
		Integer j = 0;
		for (NLMSThumbnails s : map.values()) {
			NLMSThumbnails t = new NLMSThumbnails(j);
			for (ImageAnnotationPair i : s) {
				for (Annotation a : i.getAnnotations()) {
					a.setCategory(Integer.toString(j));
					t.add(i);
				}
			}
			temp.put(Integer.toString(j++), t);
		}
		map.clear();
		map.putAll(temp);
	}

	@Override
	public void clear() {
		this.map.clear();
	}

	/**
	 * Interpret result of a match from FaceMatch ImageMatcher. Parses out a
	 * master image/annotation pair and for each image it matches stores the
	 * distance/probability of a match to that image's distances map
	 * 
	 * @param result
	 *            the result returned from a FaceMatch ImageMatcher query.
	 */
	public void parseImageMatcherResult(String result) {

		String[] rets = result.split("\n");
		// the pair for each image to have their distance calculated against
		// stored. Extracted from first line of output. If it can't be parsed,
		// return without further parsing.
		ImageAnnotationPair p;
		try {
			p = parsePair(rets[0]);
		} catch (NoSuchElementException e) {
			return;
		}
		if (p == null)
			return;
		for (int i = 1; i < rets.length; i++) {
			try {
				String[] parts = rets[i].split("\t");
				Double d = Double.parseDouble(parts[0]);
				String name = parts[1].split(":")[0];
				name = name.replace(h.getDirectory().toString() + File.separator, "");
				MetaImage m = h.getMasterList().getByName(name);
				m.distances.put(p, d);
			} catch (Exception e) {
				if (e instanceof NumberFormatException)
					continue;
				e.printStackTrace();
			}
		}
		h.setMasterListChanged(true);
		h.firePropertyChange(Property.imageGroups, null);

	}

	/**
	 * @param string
	 *            the string to parse
	 * @return a MetaImage Annotation pair extracted from the given string.
	 * 
	 */
	private ImageAnnotationPair parsePair(String string) {
		// parse name out
		String name = string.split("\\s")[0].replace(this.h.getDirectory().getAbsolutePath() + "\\", "");
		MetaImage m = this.h.getMasterList().getByName(name);
		if (m == null)
			return null;
		// parse annotation out
		String an = string.replace(this.h.getDirectory() + "\\" + name, "").replaceAll("found\\s\\d*", "").trim();
		Annotation a = m.getAnnotationByString(an);
		return new ImageAnnotationPair(m, a);
	}

	/**
	 * put each image in the group of the image/annotation pair it best matches
	 */
	public void buildFromRankings() {

		// find each image's best match
		for (MetaImage i : this.h.getMasterList()) {
			for (int j = 0; j < i.getAnnotations().size(); j++) {

				// the best match for image i, has the lowest value in i's
				// distance's map
				double min = Double.MAX_VALUE;
				ImageAnnotationPair bestMatch = null;
				for (java.util.Map.Entry<ImageAnnotationPair, Double> e : i.distances.entrySet()) {
					double v = e.getValue();
					if (v == 0)// ignore self match
						continue;
					if (v < min)
						bestMatch = e.getKey();
				}

				// no matches
				if (bestMatch == null)
					continue;
				// set the category of i to it's best match
				i.getAnnotations().get(j).setCategory(bestMatch.x.getName() + "-" + bestMatch.index);
			}
		}
		build();
		this.h.setMasterListChanged(true);

	}

	/**
	 * Get the arraylistmodelsets for both keys, and add the second
	 * arraylistmodel set to the first one, removing the second set from the
	 * map.
	 * 
	 * @param key1
	 *            the first set to combine
	 * @param key2
	 *            the second set to combine
	 */
	@SuppressWarnings("unused")
	private synchronized void combineSets(String key1, String key2) {
		if (key1 == null || key2 == null)
			return;
		NLMSThumbnails addToList = get(key1);

		NLMSThumbnails removedList = this.map.remove(key2);
		if (removedList == null)
			return;

		for (Thumbnail i : removedList)
			Annotation.renameTag(key2, key1, i.getAnnotations());

		this.map.get(key1).addAll(removedList);
		this.map.get(key1).setMasterPair();
		// TODO check is working
	}

	/**
	 * removes all occurrences of the given MetaImage
	 * 
	 * @param i
	 *            the MetaImage to remove
	 */
	@Override
	public void removeMetaImage(MetaImage i) {
		for (NLMSThumbnails s : this.getLists())
			s.removeMetaImage(i);
	}

	@Override
	public Property getProperty() {
		return Property.annotationGroups;
	}

	@Override
	public Collection<NLMSThumbnails> getLists() {
		return this.map.values();
	}

	/**
	 * remove the given key's value, rename the tags in it, then add it back in
	 * under the new key.
	 * 
	 * @param oldName
	 *            the old key
	 * @param newName
	 *            the new key
	 */
	public void changeKey(String oldName, String newName) {
		NLMSThumbnails list = this.map.remove(oldName);
		for (Thumbnail i : list)
			Annotation.renameTag(oldName, newName, i.getAnnotations());
		this.map.put(newName, list);

	}

	@Override
	public ArrayList<JMenuItem> getMenuItems() {
		ArrayList<JMenuItem> items = new ArrayList<JMenuItem>();
		items.add(new ImageMatcherMenuItem());
		return items;
	}

	@Override
	public void move(Thumbnail m, String newKey) {
		ImageAnnotationPair p = (ImageAnnotationPair) m;
		get(newKey);
		this.map.get(p.y.getCategory().toString()).remove(p);
		p.y.getCategory().setIDString(newKey);
		this.map.get(newKey).add(p);
		h.firePropertyChange(Property.annotationGroups, null);
		h.firePropertyChange(Property.displayArea, null);
	}

	@Override
	public void nearDupSort(String nearDupOutput) {
		// void out old grouping
		for (MetaImage i : this.h.getMasterList())
			for (Annotation a : i.getAnnotations())
				a.getCategory().setIDString(null);

		// convert string result to adjacency lists of metaimages
		ArrayList<ArrayList<MetaImage>> groups = new ArrayList<>();

		for (String group : nearDupOutput.split("\n")) {
			ArrayList<MetaImage> toAdd = new ArrayList<>();
			for (String memeber : group.split("\t")) {
				MetaImage image = this.h.getMasterList().getByName(memeber);
				if (image != null)
					toAdd.add(image);
			}

			addAndassignID(groups, toAdd);
		}
		build();

		h.setMasterListChanged(true);
		h.firePropertyChange(Property.imageGroups, null);
	}

	/**
	 * add the given group to the end of the list and assign it it's index as
	 * it's group ID if any images in the group already have an ID, unify the
	 * old group with the new one, giving the old images the newer id
	 * 
	 * @param groups
	 *            pre-existing groups
	 * @param toAdd
	 *            the new group
	 */
	private void addAndassignID(ArrayList<ArrayList<MetaImage>> groups, ArrayList<MetaImage> toAdd) {
		int id = groups.size();
		String ID = Integer.toString(id);
		groups.add(toAdd);
		ArrayList<MetaImage> alsoAdd = new ArrayList<>();
		for (MetaImage image : toAdd) {
			for (Annotation a : image.getAnnotations()) {
				if (a.getCategory().toString() != null) {
					int oldID = Integer.parseInt(a.getCategory().toString());
					ArrayList<MetaImage> oldList = groups.get(oldID);
					alsoAdd.addAll(oldList);
					oldList.clear();
				}

				a.getCategory().setIDString(ID);
			}
		}
		for (MetaImage image : alsoAdd)
			for (Annotation a : image.getAnnotations())
				a.getCategory().setIDString(ID);
	}

	/**
	 * Class for launching ImageMatcher Queries
	 * 
	 * @author bonifantmc
	 *
	 */
	@SuppressWarnings("serial")
	private class ImageMatcherMenuItem extends JMenuItem {
		/**
		 * 
		 */
		public ImageMatcherMenuItem() {
			super("ImageMatcher");
			this.setToolTipText("Sort Annotations into groups based on the image in each annotation boundary.");
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					FaceMatchImageMatchForm.getFaceMatchParams(AnnotationGroups.this.h, true);

				}
			});

		}

	}

	/**
	 * 
	 * @param pair
	 *            the pair to remove from this AnnotaitonGroup's map.
	 */
	public void removePair(ImageAnnotationPair pair) {
		NLMSThumbnails a = this.map.get(pair.y.getCategory().toString());
		if (a == null)
			return;
	}

	/**
	 * Add a thumbnail to an NLMSThumbnails in the map keyed to s, if no
	 * NLMSThumbnails exists at s, add one to the map
	 * 
	 * @param s
	 *            the key to the NLMSThumbnails to add t to
	 * @param t
	 *            the thumbnail to add to the NLMSThumbails
	 */
	public void addThumbnail(String s, ImageAnnotationPair t) {
		if (!map.containsKey(s))
			map.put(s, new NLMSThumbnails(s));
		map.get(s).add(t);
	}

}