/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

//package sun.nio.fs;
package struct;

import java.util.regex.PatternSyntaxException;

/**
 * 
 * @edit Chase Bonifant, This is originally a class implemented for java, this
 *       altered class is made to incorporate group matching into globs via use
 *       of parenthesis as in regular expressions
 */
final public class Globs {

	/**
	 * This class is intended as a purely static one, it should never be
	 * instantiated as an object.
	 */
	private Globs() {
	}

	/** reserved characters for regular expressions */
	private static final String regexMetaChars = ".^$+{[]|()";
	/** reserved characters for glob patterns */
	private static final String globMetaChars = "\\*?[{";

	/**
	 * 
	 * @param c
	 *            any character
	 * @return true if the character is reserved as part of regular expression
	 *         syntax
	 */
	private static boolean isRegexMeta(char c) {
		return regexMetaChars.indexOf(c) != -1;
	}

	/**
	 * 
	 * @param c
	 *            any character
	 * @return true if the character reserved as part of glob pattern syntax
	 */
	private static boolean isGlobMeta(char c) {
		return globMetaChars.indexOf(c) != -1;
	}

	/** end of line character return value */
	private static char EOL = 0; // TBD

	/**
	 * 
	 * @param glob
	 *            a glob pattern
	 * @param i
	 *            the index the character in the glob pattern to get
	 * @return the character at index i in the glob
	 */
	private static char next(String glob, int i) {
		if (i < glob.length()) {
			return glob.charAt(i);
		}
		return EOL;
	}

	/**
	 * Creates a regex pattern from the given glob expression.
	 * 
	 * @param globPattern
	 *            a glob pattern with group matching, for more on globs see
	 *            https://en.wikipedia.org/wiki/Glob_(programming)
	 * @return the input glob pattern converted to a regex.
	 * 
	 * 
	 */
	public static String toRegexPattern(String globPattern) {
		boolean inGroup = false;
		StringBuilder regex = new StringBuilder("^");

		int i = 0;
		while (i < globPattern.length()) {
			char c = globPattern.charAt(i++);
			switch (c) {
			case '\\':
				// escape special characters
				if (i == globPattern.length()) {
					throw new PatternSyntaxException("No character to escape", globPattern, i - 1);
				}
				char next = globPattern.charAt(i++);
				if (isGlobMeta(next) || isRegexMeta(next)) {
					regex.append('\\');
				}
				regex.append(next);
				break;
			case '/':
				regex.append("(?:\\\\|/)");
				break;
			case '[':
				regex.append("[[^\\\\/]&&[");
				if (next(globPattern, i) == '^') {
					// escape the regex negation char if it appears
					regex.append("\\^");
					i++;
				} else {
					// negation
					if (next(globPattern, i) == '!') {
						regex.append('^');
						i++;
					}
					// hyphen allowed at start
					if (next(globPattern, i) == '-') {
						regex.append('-');
						i++;
					}
				}
				boolean hasRangeStart = false;
				char last = 0;
				while (i < globPattern.length()) {
					c = globPattern.charAt(i++);
					if (c == ']') {
						break;
					}
					if (c == '/' || c == '\\') {
						throw new PatternSyntaxException("Explicit 'name separator' in class", globPattern, i - 1);
					}
					// TBD: how to specify ']' in a class?
					if (c == '\\' || c == '[' || c == '&' && next(globPattern, i) == '&') {
						// escape '\', '[' or "&&" for regex class
						regex.append('\\');

					}
					regex.append(c);

					if (c == '-') {
						if (!hasRangeStart) {
							throw new PatternSyntaxException("Invalid range", globPattern, i - 1);
						}
						if ((c = next(globPattern, i++)) == EOL || c == ']') {
							break;
						}
						if (c < last) {
							throw new PatternSyntaxException("Invalid range", globPattern, i - 3);
						}
						regex.append(c);
						hasRangeStart = false;
					} else {
						hasRangeStart = true;
						last = c;
					}
				}
				if (c != ']') {
					throw new PatternSyntaxException("Missing ']", globPattern, i - 1);
				}
				regex.append("]]");
				break;
			case '{':
				if (inGroup) {
					throw new PatternSyntaxException("Cannot nest groups", globPattern, i - 1);
				}
				regex.append("(?:(?:");
				inGroup = true;
				break;
			case '}':
				if (inGroup) {
					regex.append("))");
					inGroup = false;
				} else {
					regex.append('}');
				}
				break;
			case ',':
				if (inGroup) {
					regex.append(")|(?:");
				} else {
					regex.append(',');
				}
				break;
			case '*':
				regex.append(".*?");
				break;
			case '?':
				regex.append("[^\\\\/]");

				break;

			default:
				if (isRegexMeta(c) && c != '(' && c != ')' && c != '|') {
					regex.append('\\');
				}
				regex.append(c);
			}
		}

		if (inGroup) {
			throw new PatternSyntaxException("Missing '}", globPattern, i - 1);
		}

		return regex.append('$').toString();
	}
}