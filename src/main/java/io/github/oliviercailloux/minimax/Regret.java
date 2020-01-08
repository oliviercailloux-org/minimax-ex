package io.github.oliviercailloux.minimax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.google.common.graph.Graph;

import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.jlp.elements.SumTerms;
import io.github.oliviercailloux.jlp.elements.SumTermsBuilder;
import io.github.oliviercailloux.minimax.elicitation.ConstraintsOnWeights;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class Regret {

	private static double MMR;
	private static Alternative xOpt;
	private static Alternative yAdv;
	private static HashMap<Alternative, Alternative> worstAdv = new HashMap<>();

	public static List<Alternative> getMMRAlternatives(PrefKnowledge knowledge) {
		List<Alternative> alt = knowledge.getAlternatives().asList();
		ListIterator<Alternative> i = alt.listIterator();
		List<Alternative> minAlt = new LinkedList<>();
		minAlt.add(i.next());
		double minMR = getMR(minAlt.get(0), knowledge);
		double MR;
		while (i.hasNext()) {
			Alternative x = i.next();
			worstAdv.put(x, null);
			MR = getMR(x, knowledge);
			if (MR == minMR) {
				minAlt.add(x);
			}
			if (MR < minMR) {
				minMR = MR;
				minAlt.clear();
				minAlt.add(x);
			}
		}
		MMR = minMR;

		xOpt = minAlt.get(0);
		yAdv = worstAdv.get(minAlt.get(0));

		return minAlt;
	}

	public static double getMR(Alternative x, PrefKnowledge knowledge) {
		List<Alternative> alt = knowledge.getAlternatives().asList();
		ListIterator<Alternative> i = alt.listIterator();
		double maxPMR = Double.MIN_VALUE;
		double PMR;
		while (i.hasNext()) {
			Alternative y = i.next();
			if (!x.equals(y)) {
				PMR = getPMR(x, y, knowledge);
				if (PMR > maxPMR) {
					maxPMR = PMR;
					worstAdv.put(x, y);
				}
			}
		}
		return maxPMR;
	}

	public static double getPMR(Alternative x, Alternative y, PrefKnowledge knowledge) {
		int nbAlt = knowledge.getAlternatives().size();
		int[] xrank = new int[nbAlt + 1];
		int[] yrank = new int[nbAlt + 1];
		int[] r;
		for (Voter v : knowledge.getProfile().keySet()) {
			r = getWorstRanks(x, y, knowledge.getProfile().get(v));
			xrank[r[0]]++;
			yrank[r[1]]++;
		}
		ConstraintsOnWeights cow = knowledge.getConstraintsOnWeights();
		SumTermsBuilder sb = SumTerms.builder();
		for (int i = 1; i <= nbAlt; i++) {
			sb.add(cow.getTerm(yrank[i] - xrank[i], i));
		}
		SumTerms objective = sb.build();
		return cow.maximize(objective);
	}

	static int[] getWorstRanks(Alternative x, Alternative y, VoterPartialPreference voterPartialPreference) {
		int rankx = 0;
		int ranky = 0;
		Graph<Alternative> trans = voterPartialPreference.asTransitiveGraph();
		/**
		 * Case1 x >^p y : place as much alternatives as possible above x W1: worst than
		 * x (in >^p). W3: better than y. A: the whole set of alternatives. Then the
		 * better ones are B: A \ W1. The middle ones are M: W1 intersection W3.
		 **/
		if (trans.hasEdgeConnecting(x, y)) {
			HashSet<Alternative> A = new HashSet<>(voterPartialPreference.asGraph().nodes());
			A.remove(x);
			A.remove(y);

			HashSet<Alternative> W1 = new HashSet<>(trans.successors(x));
			W1.remove(y);
			HashSet<Alternative> W3 = new HashSet<>(trans.predecessors(y));
			W3.remove(x);
			HashSet<Alternative> B = new HashSet<>(A);
			B.removeAll(W1);
			rankx = B.size() + 1;

			HashSet<Alternative> M = new HashSet<>(W1);
			M.retainAll(W3);
			ranky = rankx + M.size() + 1;
		} else {
			/**
			 * Case2 y >^p x: place as much alternatives as possible between x and y Case3 x
			 * ?^p y: consider y >^p x W1: better than y. W2: worst than x. A: the whole set
			 * of alternatives. Then the better ones are W1. The middle ones are M: A \ W1 \
			 * W2. So the rank of x is |A \ W2|+1.
			 **/
			ranky = trans.predecessors(y).size() + 1;

			rankx = voterPartialPreference.asGraph().nodes().size() - trans.successors(x).size();
		}
		int[] r = { rankx, ranky };
		return r;
	}

	public static double getMMR() {
		return MMR;
	}

	public static Alternative getxOpt() {
		return xOpt;
	}

	public static Alternative getyAdv() {
		return yAdv;
	}
		
}
