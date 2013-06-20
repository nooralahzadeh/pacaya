package edu.jhu.gridsearch.dmv;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.DepTreebank;
import edu.jhu.data.SentenceCollection;
import edu.jhu.gridsearch.dmv.DmvObjective.DmvObjectivePrm;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.model.dmv.RandomDmvModelFactory;
import edu.jhu.parse.relax.LpDmvRelaxedParserTest;
import edu.jhu.train.DmvTrainCorpus;
import edu.jhu.util.Prng;

public class DmvObjectiveTest {

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testIntFeatCountsObjectiveComputation() {
        DmvObjectivePrm prm = new DmvObjectivePrm();
        DmvObjective dmvObj = new DmvObjective(prm, null);
        
        double[][] logProbs = new double[][]{{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
        int[][] featCounts = new int[][]{{1, 2, 3}, {4, 5, 6}};

        assertEquals(91.0, dmvObj.computeTrueObjective(logProbs, featCounts), 1e-13);
    }

    @Test
    public void testDoubleFeatCountsObjectiveComputation() {
        DmvObjectivePrm prm = new DmvObjectivePrm();
        DmvObjective dmvObj = new DmvObjective(prm, null);
        
        double[][] logProbs = new double[][]{{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
        double[][] featCounts = new double[][]{{1, 2, 3}, {4, 5, 6}};

        assertEquals(91.0, dmvObj.computeTrueObjective(logProbs, featCounts), 1e-13);
    }

    @Test
    public void testShinyEdgesPostCons() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the/NOUN man/NOUN ran/VERB");
        sentences.addSentenceFromString("cat/NOUN ate/VERB mouse/NOUN");
        sentences.addSentenceFromString("very/ADV red/ADJ sweaters/NOUN shine/VERB");   
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        IndexedDmvModel idm = new IndexedDmvModel(corpus);
        DmvModelFactory modelFactory = new RandomDmvModelFactory(0.1);
        DmvModel model = modelFactory.getInstance(sentences.getLabelAlphabet());

        DepTreebank projTrees;
        projTrees = LpDmvRelaxedParserTest.getPostConsTrees(sentences, model, true, 0.7);       

        DmvObjectivePrm prm = new DmvObjectivePrm();
        prm.universalPostCons = true;
        prm.universalMinProp = 1.0;
        prm.shinyEdges = LpDmvRelaxedParserTest.getUniversalSet(corpus.getLabelAlphabet());
        DmvObjective dmvObj = new DmvObjective(prm, idm);
        
        assertEquals(0.7, dmvObj.propShiny(idm.getTotFreqCm(projTrees)), 1e-13);
        assertEquals(Double.NEGATIVE_INFINITY, dmvObj.computeTrueObjective(model, projTrees), 1e-13);
        
        prm.universalMinProp = 0.6;
        assertEquals(-31.67, dmvObj.computeTrueObjective(model, projTrees), 1e-2);
    }
}
