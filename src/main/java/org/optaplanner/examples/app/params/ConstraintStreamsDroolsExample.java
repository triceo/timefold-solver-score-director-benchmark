package org.optaplanner.examples.app.params;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.optaplanner.examples.app.directors.ScoreDirector;

@State(Scope.Benchmark)
public class ConstraintStreamsDroolsExample extends AbstractExample {

    @Param // All of them.
    public Example example;

    @Override
    protected ScoreDirector getScoreDirector() {
        return ScoreDirector.CONSTRAINT_STREAMS_DROOLS;
    }

    @Override
    protected Example getExample() {
        return example;
    }
}