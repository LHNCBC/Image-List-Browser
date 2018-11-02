package struct;

import java.awt.Container;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * @author bonifantmc
 *
 */
public interface Group {

	/**
	 * sort groups based on file name prefixes
	 * 
	 * @param prefix
	 *            the length of the prefix
	 */
	public void prefixSort(int prefix);

	/**
	 * sort groups based on a glob (which is just a fancy subset of
	 * regexMatchsort)
	 * 
	 * @param glob
	 *            the glob to match by
	 */
	public void globSort(String glob);

	/**
	 * sort groups based on a regular expression
	 * 
	 * @param regex
	 *            the regular expression to match by
	 */
	public void regexSort(String regex);

	/**
	 * Parses the output of the NearDupImageDetector into groups
	 * 
	 * @param nearDupOutput
	 *            the output of the NearDupImageDetector
	 * 
	 */
	public void nearDupSort(String nearDupOutput);

	/** (re)build the entire group */
	public void build();

	/** void out the entire group */
	public void clear();

	/**
	 * remove all traces of the given MetaImage
	 * 
	 * @param i
	 *            the MetaImge to remove
	 */
	public void removeMetaImage(MetaImage i);

	/** @return return the property for this specific group */
	public Property getProperty();

	/**
	 * @return list of all groups
	 */
	public Collection<NLMSThumbnails> getLists();

	/** @return grouping items */
	public ArrayList<JMenuItem> getMenuItems();

	/**
	 * 
	 * @param m
	 *            the image being regrouped
	 * @param newName
	 *            the new group to place it in
	 */
	public void move(Thumbnail m, String newName);

	///////////////////////////////////////////////////////////////////////////
	// some shared getters for getting prefixes, globs, and regexs from the //
	// user /////////////////////////////////////////////////////////////////
	/////////

	/**
	 * Create a prompt asking for a string from the user.
	 * 
	 * @param prompt
	 *            the question to ask user what string we want
	 * @param c
	 *            the container to center the prompt over
	 * @return the string the user gave
	 */
	public static String getTextInput(String prompt, Container c) {
		return JOptionPane.showInputDialog(c, prompt);
	}

	/**
	 * 
	 * @param c
	 *            the container this method's prompts to the user should be
	 *            centered over
	 * @return a prefix length (or negative value if the user canceled)
	 */
	public static int getValidPrefix(Container c) {
		String size = getTextInput("Enter the prefix length: ", c);

		// canceled action
		if (size == null)
			return -1;

		// check for validity
		do {
			try {
				int prefixLength = Integer.parseInt(size);
				if (prefixLength > 0)
					return prefixLength;

			} catch (NumberFormatException e) {
				// doesn't matter continue the DO-WHILE if bad input
			}
			size = Group.getTextInput(
					"Please enter a valid prefix length (prefix must be positive integer values (ie: 1, 2, 3...): ", c);
		} while (size != null);
		// if loop ended then the user gave up
		return -1;
	}

	/**
	 * 
	 * @param c
	 *            the container this method's prompts to the user should be
	 *            centered over
	 * @return a glob pattern provided by the user(or null if the user canceled
	 */
	public static String getValidGlob(Container c) {
		String glob = getTextInput("Enter a Glob Expression: ", c);

		// canceled action
		if (glob == null)
			return null;

		// check for validity
		do {
			if (validateGlob(glob))
				return glob;
			glob = Group.getTextInput("Please enter a valid Glob Expression: ", c);
		} while (glob != null);
		// if loop ended then the user gave up
		return null;
	}

	/**
	 * Validates if a glob pattern is valid
	 * 
	 * @param glob
	 *            the glob pattern
	 * @return true if the pattern is valid
	 */
	public static boolean validateGlob(String glob) {

		return validateRegex(Globs.toRegexPattern(glob));
	}

	/**
	 * Validates if a regex pattern is valid
	 * 
	 * @param regexPattern
	 *            the regex to validate
	 * @return true if the regex is valid
	 */
	public static boolean validateRegex(String regexPattern) {
		try {
			Pattern.compile(regexPattern);
			return true;
		} catch (PatternSyntaxException e) {
			return false;
		}
	}

	/***
	 * 
	 * @param c
	 *            the container this method's prompts to the user should be
	 *            centered over
	 * @return a regex pattern provided by the user (or null if the user
	 *         canceled)
	 */
	public static String getValidRegex(Container c) {
		String regex = getTextInput("Enter a Regular Expression: ", c);

		// canceled action
		if (regex == null)
			return null;

		// check for validity
		do {
			if (validateRegex(regex))
				return regex;

			regex = Group.getTextInput("Please enter a valid Regular Expression: ", c);
		} while (regex != null);
		// if loop ended then the user gave up
		return null;
	}
}
