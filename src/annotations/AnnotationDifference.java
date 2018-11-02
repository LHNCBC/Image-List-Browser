package annotations;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import struct.MetaImage;
import struct.Pair;

/**
 * Compares two Annotations, based on if they overlap.
 * 
 * The x Annotation list is considered the base/ground truth that the y
 * Annotation wants to be like
 * 
 * @author bonifantmc
 *
 */
public class AnnotationDifference extends Pair<List<Annotation>, List<Annotation>> {
	/**
	 * 
	 */
	List<Double> scores = new ArrayList<>();
	/** when there's no annotation in y to match an annotation in x */
	Set<Annotation> falseNeg = new HashSet<>();
	/** when there's an annotation in y that doesn't match an annotation in x */
	Set<Annotation> falsePos = new HashSet<>();
	/** when an annotation y matches an annotation in x */
	Set<Annotation> truePos = new HashSet<>();
	/** score for summarizing how well values intersect. */
	double averageIntersectionRatio;
	/**
	 * Minimum ratio of area intersect allowed, 1+ (1-minAreaMatch) is the max
	 */
	double minAreaMatch;
	/** 1+(1-minAreaMatch) */
	double maxAreaMatch;

	/**
	 * 
	 * @param annotated
	 *            ground truth, baseline comparison annotations
	 * @param guessed
	 *            the guessed annotations to compare with the ground
	 *            truth/baseline
	 * @param minAreaMatch
	 *            the minimum area that a guess needs to match with a baseline
	 *            to be a successful match
	 * 
	 */
	public AnnotationDifference(List<Annotation> annotated, List<Annotation> guessed, double minAreaMatch) {
		super(annotated, guessed);
		this.minAreaMatch = minAreaMatch;
		this.maxAreaMatch = 1 + 1 - minAreaMatch;
		calculateIntersection();
	}

	/**
	 * 
	 * @param annotated
	 *            ground truth, baseline comparison annotations
	 * @param guessed
	 *            the guessed annotations to compare with the ground
	 *            truth/baseline
	 * 
	 */
	public AnnotationDifference(List<Annotation> annotated, List<Annotation> guessed) {
		super(annotated, guessed);
		this.minAreaMatch = Annotation.min_diff;
		this.maxAreaMatch = 1 + 1 - minAreaMatch;
		calculateIntersection();
	}

	/**
	 * evaluate the x & y lists (the baseline and guessed annotations)
	 * determining true positive count, false positive count, false negative
	 * count, and a metric for overall overlap success of the baseline with the
	 * guessed annotations dubbed "averageIntersectionRatio"
	 * 
	 */
	public void calculateIntersection() {
		// void out old true/false positive/negatives
		falseNeg.clear();// annotations from the baseline that had not matches
		falsePos.clear();// annotations from the guess that didn't match
							// annotations in the baseline
		truePos.clear();// annotations from the guess that did match the
						// baseline

		Set<Annotation> visitedMatch = new HashSet<>();// annotations that had
														// true positive matches
														// from the baseline

		// assume all guesses are false positives
		for (Annotation b : y)
			falsePos.add(b);

		averageIntersectionRatio = 0;
		for (Annotation a : x) {
			for (Annotation b : y) {
				Rectangle2D intersection = a.createIntersection(b);
				double interArea = intersection.getWidth() * intersection.getHeight();
				double score = interArea / (a.getArea() + b.getArea() - interArea);

				// check if it is a match
				if (score >= minAreaMatch && score <= maxAreaMatch) {
					falsePos.remove(b);
					truePos.add(b);
					visitedMatch.add(a);
					scores.add(score);
					averageIntersectionRatio += score;
				}
			}
			if (!visitedMatch.contains(a))
				falseNeg.add(a);
		}
		averageIntersectionRatio /= (truePos.size() + falsePos.size() + falseNeg.size());
	}

	/**
	 * @return a string listing the number of true positives, false positives,
	 *         false negatives, and the average intersection ratio, (include
	 *         html breaks for new lines).
	 */
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append("True Positives: ").append(truePos.size()).append("<br>");
		sb.append("False Positives: ").append(falsePos.size()).append("<br>");
		sb.append("False Negatives: ").append(falseNeg.size()).append("<br>");
		sb.append("Average Score: ").append(String.format("%.2f", averageIntersectionRatio)).append("<br>");
		return sb.toString();
	}

	/** @return the image's average score fo all annotations */
	public double getScore() {
		return this.averageIntersectionRatio;
	}

	/**
	 * @author bonifantmc
	 *
	 */
	public static class Stats {
		/** average score of all images */
		double score;
		/** number of false negatives in list */
		int falseNeg;
		/** number of false postives in list */
		int falsePos;
		/** number of true positives in list */
		int truePos;

		/**
		 * @param doubles
		 *            the list of doubles to sum
		 * @return the sum of the list of doubles
		 */
		private static Double sum(List<Double> doubles) {
			double sum = 0.0;
			for (double d : doubles)
				sum += d;
			return sum;
		}

		/**
		 * @param images
		 *            the list of images to get stats for
		 * @param alt
		 *            the specific list of alternative annotations to get Stats
		 *            on
		 * @return the Stats on the alt annotation list for the images list
		 */
		public static Stats calculateStats(List<MetaImage> images, List<Annotation> alt) {
			Stats stat = new Stats();
			for (MetaImage i : images) {
				AnnotationDifference ad = i.getDifferences().get(alt);
				stat.score += sum(ad.scores);
				stat.falseNeg += ad.falseNeg.size();
				stat.falsePos += ad.falsePos.size();
				stat.truePos += ad.truePos.size();
				stat.score /= (stat.falseNeg + stat.truePos + stat.falsePos);
			}
			return stat;
		}

		/**
		 * @param images
		 *            list of images to get Stats for
		 * 
		 * @return a Map of all alternative annotation lists to their Stats
		 */
		public static Map<List<Annotation>, Stats> allStats(List<MetaImage> images) {
			Map<List<Annotation>, Stats> statMap = new HashMap<>();
			for (Entry<List<Annotation>, AnnotationDifference> diff : images.get(0).getDifferences().entrySet())
				statMap.put(diff.getKey(), Stats.calculateStats(images, diff.getKey()));
			return statMap;
		}
	}

}