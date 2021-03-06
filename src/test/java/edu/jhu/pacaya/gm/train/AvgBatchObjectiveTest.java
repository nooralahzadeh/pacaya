package edu.jhu.pacaya.gm.train;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.train.AvgBatchObjective.ExampleObjective;

public class AvgBatchObjectiveTest {

    private static class MockExObj implements ExampleObjective {

        public int numChecks = 0;
        
        @Override
        public void accum(FgModel model, int i, Accumulator vg) {
            assertEquals(77, vg.curIter);
            assertEquals(88, vg.maxIter);
            numChecks++;
        }

        @Override
        public int getNumExamples() {
            return 6;
        }

        @Override
        public void report() {
            // no-op
        }
        
    }
    
    @Test
    public void testNonstationaryFunction() {        
        FgModel model = new FgModel(10);
        MockExObj exObj = new MockExObj();
        AvgBatchObjective avg = new AvgBatchObjective(exObj, model);
        
        avg.updatateIterAndMax(77, 88);
        avg.getValue(model.getParams());
        avg.getGradient(model.getParams());
        avg.getValueGradient(model.getParams());
        
        assertEquals(3*6, exObj.numChecks);
    }

}
