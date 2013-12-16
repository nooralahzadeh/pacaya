package edu.jhu.gm.data;

import edu.jhu.gm.feat.FactorTemplateList;


/**
 * A collection of instances for a graphical model represented as factor graphs.
 * 
 * @author mgormley
 *
 */
public interface FgExampleList extends Iterable<FgExample> {

    /** Gets the i'th example. */
    public FgExample get(int i);
    
    /** Gets the number of examples. */
    public int size();

    /** Gets the feature templates for these examples. */
    public FactorTemplateList getTemplates();

    @Deprecated
    public void setSourceSentences(Object sents);
    @Deprecated
    public Object getSourceSentences();
    @Deprecated
    public int getNumFactors();
    @Deprecated
    public int getNumVars();
    
}
