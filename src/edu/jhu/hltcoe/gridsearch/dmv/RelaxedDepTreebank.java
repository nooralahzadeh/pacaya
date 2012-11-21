/**
 * 
 */
package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Utilities;

public class RelaxedDepTreebank {
    private double[][] fracRoots;
    private double[][][] fracChildren;

    public RelaxedDepTreebank(DmvTrainCorpus corpus) {
        fracRoots = new double[corpus.size()][];
        fracChildren = new double[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                fracRoots[s] = null;
                fracChildren[s] = null;
            } else {
                Sentence sentence = corpus.getSentence(s);
                fracRoots[s] = new double[sentence.size()];
                fracChildren[s] = new double[sentence.size()][sentence.size()];
            }
        }
    }
    
    public double[][] getFracRoots() {
        return fracRoots;
    }

    public double[][][] getFracChildren() {
        return fracChildren;
    }

    public double getPropFracArcs() {
        int numFractional = 0;
        int numArcs = 0;
        for (int s = 0; s < fracRoots.length; s++) {
            double[] fracRoot = fracRoots[s];
            double[][] fracChild = fracChildren[s];
            
            if (fracRoot == null) {
                continue;
            }
            
            for (int child = 0; child < fracRoot.length; child++) {
                if (isFractional(fracRoot[child])) {
                    numFractional++;
                }
                numArcs++;
            }
            for (int parent = 0; parent < fracChild.length; parent++) {
                for (int child = 0; child < fracChild[parent].length; child++) {
                    if (isFractional(fracChild[parent][child])) {
                        numFractional++;
                    }
                    numArcs++;
                }
            }
        }
        if (numArcs == 0) {
            return 0.0;
        } else {
            return (double) numFractional / numArcs;
        }
    }

    private boolean isFractional(double arcWeight) {
        if (Utilities.equals(arcWeight, 0.0, 1e-9) || Utilities.equals(arcWeight, 1.0, 1e-9)) {
            return false;
        }
        return true;
    }
}