package edu.jhu.gm.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FactorTemplate;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.util.IntIter;
import edu.jhu.prim.arrays.BoolArrays;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.prim.map.IntDoubleMap;
import edu.jhu.prim.util.Lambda.FnIntDoubleToDouble;
import edu.jhu.prim.util.Lambda.LambdaUnaryOpDouble;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleHashVector;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.Alphabet;
import edu.jhu.util.dist.Gaussian;

/**
 * A model in the exponential family for a factor graph .
 * 
 * @author mgormley
 *
 */
// TODO: rename to CrfModel.
// TODO: Internally we could store the parameters of a particular feature
// template/config as a SparseVector so that the footprint of this object
// (particularly when serialized) is smaller.
public class FgModel implements Serializable, IFgModel {

    private static final Logger log = Logger.getLogger(FgModel.class);

    private static final long serialVersionUID = 4477788767217412525L;
    /** The model parameters. */
    private final IntDoubleVector params;
    /**
     * The model parameters indices. Indexed by feature template index, variable
     * assignment config index, and observation function feature index.
     */
    private final int[][][] indices;
    /**
     * Whether or not the correspondingly indexed model parameter is included in
     * this model.
     */
    private final boolean[][][] included;
    /** The number of feature templates. */
    private int numTemplates;
    /** The number of parameters in the model. */
    private int numParams;
    /** The feature templates. */
    private FactorTemplateList templates;
    
    public FgModel(FgExampleList data, boolean includeUnsupportedFeatures) {
        this(data, data.getTemplates(), includeUnsupportedFeatures);
    }
    
    public FgModel(FactorTemplateList templates) {
        this(null, templates, true);
    }
    
    private FgModel(FgExampleList data, FactorTemplateList templates, boolean includeUnsupportedFeatures) {
        this.templates = templates;
        numTemplates = templates.size();
        
        this.indices = new int[numTemplates][][];
        this.included = new boolean[numTemplates][][];
        for (int t=0; t<numTemplates; t++) {
            FactorTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            Alphabet<Feature> alphabet = template.getAlphabet();
            indices[t] = new int[numConfigs][alphabet.size()];
            included[t] = new boolean[numConfigs][alphabet.size()];
        }
        
        if (!includeUnsupportedFeatures) {
            includeSupportedFeatures(data, templates);
        } else {
            BoolArrays.fill(included, true);
        }
      
        // Always include the bias features.
        for (int t=0; t<indices.length; t++) {
            FactorTemplate template = templates.get(t);
            Alphabet<Feature> alphabet = template.getAlphabet();            
            for (int k = 0; k < alphabet.size(); k++) {
                if (alphabet.lookupObject(k).isBiasFeature()) {
                    for (int c = 0; c < indices[t].length; c++) {
                        included[t][c][k] = true;
                    }        
                }
            }
        }
        
        // Set the indices to track only the included parameters.
        // All other entries are set to -1.
        // Also: Count the number of parameters, accounting for excluded params.
        for (int t=0; t<indices.length; t++) {
            for (int c = 0; c < indices[t].length; c++) {
                for (int k = 0; k < indices[t][c].length; k++) {
                    indices[t][c][k] = included[t][c][k] ? numParams++ : -1;
                }
            }
        }
        
        this.params = new IntDoubleDenseVector(numParams);
        for (int i=0; i<numParams; i++) {
            params.set(i, 0.0);
        }
    }
    
    /** Shallow copy constructor which also sets params. */
    private FgModel(FgModel other, IntDoubleVector params) {
        this.params = params;
        this.indices = other.indices;
        this.included = other.included;
        this.numParams = other.numParams;
        this.numTemplates = other.numTemplates;
        this.templates = other.templates;
    }
    
    /** Copy constructor. */
    public FgModel getDenseCopy() {
        return new FgModel(this, new IntDoubleDenseVector(params));
    }
    
    /** Copy constructor, which initializes the parameter vector to all zeros. */
    public FgModel getSparseZeroedCopy() {
        return new FgModel(this, new IntDoubleHashVector(10000));
    }

    /**
     * For each factor in the data, lookup its configId. Set all the
     * observed features for that configuration to true.
     */
    private void includeSupportedFeatures(FgExampleList data, FactorTemplateList templates) {
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            for (int a=0; a<ex.getOriginalFactorGraph().getNumFactors(); a++) {
                Factor f = ex.getFgLat().getFactor(a);
                if (f instanceof GlobalFactor) {
                    continue;
                } else if (f instanceof ExpFamFactor) {
                    int t = templates.getTemplateId(f);
                    if (t != -1) {
                        FeatureVector fv = ex.getObservationFeatures(a);
                        if (f.getVars().size() == 0) {
                            int predConfig = ex.getGoldConfigIdxPred(a);
                            for (IntDoubleEntry entry : fv) {
                                included[t][predConfig][entry.index()] = true;
                            }
                        } else {
                            // We must clamp the predicted variables and loop over the latent ones.
                            VarConfig predVc = ex.getGoldConfigPred(a);
                            IntIter iter = IndexForVc.getConfigIter(ex.getFgLatPred().getFactor(a).getVars(), predVc);
                            
                            int numConfigs = f.getVars().calcNumConfigs();
                            for (int c=0; c<numConfigs; c++) {            
                                // The configuration of all the latent/predicted variables,
                                // where the predicted variables have been clamped.
                                int config = iter.next();
                                for (IntDoubleEntry entry : fv) {
                                    included[t][config][entry.index()] = true;
                                }
                            }
                        }
                    }
                } else {
                    throw new UnsupportedFactorTypeException(f);
                }
            }
        }
    }
    
    public void updateModelFromDoubles(double[] inParams) {
        assert numParams == inParams.length : String.format("numParams=%d inParams.length=%d", numParams, inParams.length);
        for (int i=0; i<numParams; i++) {
            this.params.set(i, inParams[i]);
        }
    }
    
    public void updateDoublesFromModel(double[] outParams) {
        assert (numParams == outParams.length);
        for (int i=0; i<numParams; i++) {
            outParams[i] = this.params.get(i);
        }
    }
    
    public void add(int ft, int config, int feat, double addend) {
      if (!included[ft][config][feat]) {
          throw new IllegalArgumentException("The specified parameter is not included in this model");
      }
      params.add(indices[ft][config][feat], addend);
    }

    public void addIfParamExists(int ft, int config, int feat, double addend) {
        if (included[ft][config][feat]) {
            params.add(indices[ft][config][feat], addend);
        }
    }

    public void addIfParamExists(int t, int c, FeatureVector fv, double multiplier) {
        int[] fvInd = fv.getInternalIndices();
        double[] fvVal = fv.getInternalValues();
        for (int i=0; i<fvInd.length; i++) {
            int f = fvInd[i];
            if (included[t][c][f]) {
                params.add(indices[t][c][f], multiplier * fvVal[i]);
            }
        }
    }
    
    private boolean shouldLogNumExplicitParams = true;
    
    public void add(FgModel other) {
        if (other.indices != this.indices) {
            throw new IllegalStateException("Only copies of this model can be added to it.");
        }
        if (shouldLogNumExplicitParams && other.params instanceof IntDoubleMap) {
            // Only log this once for posterity.
            log.debug(String.format("Adding %d explicit params to model from a %s.",
                    ((IntDoubleMap) other.params).size(), other.params.getClass()));
            shouldLogNumExplicitParams = false;
        }
        this.params.add(other.params);
    }
    
    public double dot(int t, int c, FeatureVector fv) {        
        double dot = 0.0;
        int[] fvInd = fv.getInternalIndices();
        double[] fvVal = fv.getInternalValues();
        for (int i=0; i<fvInd.length; i++) {
            int f = fvInd[i];
            if (f < included[t][c].length && included[t][c][f]) {
                dot += fvVal[i] * params.get(indices[t][c][f]);
            }
        }
        return dot;
    }
    
    public int getNumParams() {
        return numParams;
    }

    public int getNumTemplates() {
        return indices.length;
    }

    public int getNumConfigs(int ft) {
        return indices[ft].length;
    }

    public int getNumFeats(int ft, int c) {
        return indices[ft][c].length;
    }

    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            printModel(new OutputStreamWriter(baos));
            return baos.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void printModel(Writer writer) throws IOException {
        for (int t=0; t<numTemplates; t++) {
            FactorTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            Alphabet<Feature> alphabet = template.getAlphabet();
            for (int c = 0; c < numConfigs; c++) {
                //VarConfig vc = vars.getVarConfig(c);
                for (int k = 0; k < indices[t][c].length; k++) {
                    if (included[t][c][k]) {
                        writer.write(template.getKey().toString());
                        writer.write("_");
                        writer.write(template.getStateNamesStr(c));
                        writer.write("_");
                        writer.write(alphabet.lookupObject(k).toString());
                        writer.write("\t");
                        writer.write(String.format("%.13g", params.get(indices[t][c][k])));
                        writer.write("\n");
                    }
                }
            }
        }
        writer.flush();
    }

    public FactorTemplateList getTemplates() {
        return templates;
    }
    
    public void apply(FnIntDoubleToDouble lambda) {
        params.apply(lambda);
    }

    /** ONLY FOR TESTING. */
    void apply(final LambdaUnaryOpDouble lambda) {
        params.apply(new FnIntDoubleToDouble() {
            @Override
            public double call(int idx, double val) {
                return lambda.call(val);
            }
        });
    }

    /**
     * Fill the model parameters with values randomly drawn from ~ Normal(0, 1).
     */
    public void setRandomStandardNormal() {
        FnIntDoubleToDouble lambda = new FnIntDoubleToDouble() {
            @Override
            public double call(int idx, double val) {
                return Gaussian.nextDouble(0.0, 1.0);
            }
        };
        apply(lambda);
    }

    public void fill(final double value) {
        apply(new FnIntDoubleToDouble() {
            @Override
            public double call(int idx, double ignored) {
                return value;
            }
        });
    }
    
    public void zero() {
        fill(0.0);
    }

    public void scale(final double multiplier) {
        apply(new FnIntDoubleToDouble() {
            @Override
            public double call(int idx, double val) {
                return multiplier * val;
            }
        });
    }
        
}
