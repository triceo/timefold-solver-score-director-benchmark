package ai.timefold.solver.jmh.scoredirector.problems;

public interface Problem {

    void setupTrial();

    void setupIteration();

    void setupInvocation();

    Object runInvocation();

    void tearDownInvocation();

    void tearDownIteration();

    void teardownTrial();

}
