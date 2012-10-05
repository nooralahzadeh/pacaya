package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import util.Alphabet;
import edu.jhu.hltcoe.data.DepTree.HeadFinderException;
import edu.stanford.nlp.ling.CategoryWordTag;
import edu.stanford.nlp.trees.DiskTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Treebank;

public class DepTreebank implements Iterable<DepTree> {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(DepTreebank.class);

    private SentenceCollection sentences = null;
    private int maxSentenceLength;
    private int maxNumSentences;
    private TreeFilter filter = null;
    private Alphabet<Label> alphabet;
    private ArrayList<DepTree> trees;
        
    public DepTreebank(Alphabet<Label> alphabet) {
        this(Integer.MAX_VALUE, Integer.MAX_VALUE, alphabet);
    }

    public DepTreebank(final int maxSentenceLength, final int maxNumSentences, Alphabet<Label> alphabet) {
        this(maxSentenceLength, maxNumSentences, null, alphabet);
    }
    
    private DepTreebank(final int maxSentenceLength, final int maxNumSentences, TreeFilter filter, Alphabet<Label> alphabet) {
        this.maxSentenceLength = maxSentenceLength;
        this.maxNumSentences = maxNumSentences;
        this.filter = filter;
        this.alphabet = alphabet;
        this.trees = new ArrayList<DepTree>();
    }
    
    public void setTreeFilter(TreeFilter filter) {
        this.filter = filter;
    }
    
    public void loadPath(String trainPath) {
        Treebank stanfordTreebank = new DiskTreebank();
        CategoryWordTag.suppressTerminalDetails = true;
        stanfordTreebank.loadPath(trainPath);
        for (Tree stanfordTree : stanfordTreebank) {
            try {
                if (this.size() >= maxNumSentences) {
                    break;
                }
                DepTree tree = new DepTree(stanfordTree);
                int len = tree.getNumTokens();
                if (len <= maxSentenceLength) {
                    if (filter == null || filter.accept(tree)) {
                        this.add(tree);
                    }
                }
            } catch (HeadFinderException e) {
                log.warn("Skipping tree due to HeadFinderException: " + e.getMessage());
            }
        }
    }

    public SentenceCollection getSentences() {
        if (sentences == null) {
            sentences = new SentenceCollection(this);
        }
        return sentences;
    }

    public int getNumTokens() {
        int numWords = 0;
        for (DepTree tree : this) {
            numWords += tree.getNumTokens();
        }
        return numWords;
    }
    
    public Set<Label> getTypes() {
        Set<Label> types = new HashSet<Label>();
        for (DepTree tree : this) {
            for (DepTreeNode node : tree) {
                types.add(node.getLabel());
            }
        }
        types.remove(WallDepTreeNode.WALL_LABEL);
        return types;
    }
    
    public int getNumTypes() {
        return getTypes().size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DepTree tree : this) {
            sb.append(tree);
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public void add(DepTree tree) {
        addTreeToAlphabet(tree);
        trees.add(tree);
    }

    private void addTreeToAlphabet(DepTree tree) {
        for (DepTreeNode node : tree) {
            if (node.getLabel() != WallDepTreeNode.WALL_LABEL) {
                alphabet.lookupObject(node.getLabel());
            }
        }
    }
    
    public void rebuildAlphabet() {
        alphabet.reset();
        for (DepTree tree : trees) {
            addTreeToAlphabet(tree);
        }
    }
    
    public DepTree get(int i) {
        return trees.get(i);
    }
    
    public int size() {
        return trees.size();
    }

    public Alphabet<Label> getAlphabet() {
        return alphabet;
    }

    @Override
    public Iterator<DepTree> iterator() {
        return trees.iterator();
    }
    
}
