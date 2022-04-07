package org.optaplanner.sdb.params;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class ConstraintStreamsBavetExample extends AbstractExample {

    @Param ({"CLOUD_BALANCING", "NQUEENS", "ROCK_TOUR", "TASK_ASSIGNING", "VEHICLE_ROUTING"})
    public Example example;

    @Override
    protected ScoreDirectorType getScoreDirector() {
        return ScoreDirectorType.CONSTRAINT_STREAMS_BAVET;
    }

    @Override
    protected Example getExample() {
        return example;
    }
}
