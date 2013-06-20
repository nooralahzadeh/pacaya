package edu.jhu.model.dmv;

import edu.jhu.util.Alphabet;
import edu.jhu.data.Label;
import edu.jhu.model.Model;
import edu.jhu.train.TrainCorpus;

public abstract class AbstractDmvModelFactory implements DmvModelFactory {

    public AbstractDmvModelFactory() {
        super();
    }

    @Override
    public Model getInstance(TrainCorpus corpus) {
        return getInstance(corpus.getLabelAlphabet());
    }
    
    public abstract DmvModel getInstance(Alphabet<Label> alphabet);
    
}