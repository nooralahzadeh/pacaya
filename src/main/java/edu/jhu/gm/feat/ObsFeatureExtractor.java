package edu.jhu.gm.feat;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeExpFamFactor;

public interface ObsFeatureExtractor {

    /**
     * Initializes the feature extractor. This method must be called exactly
     * once before any calls to calcObsFeatureVector are made.
     * 
     * @param ex The factor graph example.
     * @param fts The templates.
     */
    void init(FgExample ex, FactorTemplateList fts);

    /**
     * Creates the observation function feature vector for the specified factor.
     * 
     * @param factorId The id of the factor.
     * @return The feature vector on the observations only.
     */
    FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor);
    
}
