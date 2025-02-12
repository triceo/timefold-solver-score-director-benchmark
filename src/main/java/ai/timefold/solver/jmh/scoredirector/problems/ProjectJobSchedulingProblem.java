package ai.timefold.solver.jmh.scoredirector.problems;

import java.io.File;

import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.examples.projectjobscheduling.domain.Allocation;
import ai.timefold.solver.examples.projectjobscheduling.domain.Schedule;
import ai.timefold.solver.examples.projectjobscheduling.optional.score.ProjectJobSchedulingIncrementalScoreCalculator;
import ai.timefold.solver.examples.projectjobscheduling.persistence.ProjectJobSchedulingSolutionFileIO;
import ai.timefold.solver.examples.projectjobscheduling.score.ProjectJobSchedulingConstraintProvider;
import ai.timefold.solver.jmh.scoredirector.Example;
import ai.timefold.solver.jmh.scoredirector.ScoreDirectorType;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public final class ProjectJobSchedulingProblem extends AbstractProblem<Schedule> {

    public ProjectJobSchedulingProblem(ScoreDirectorType scoreDirectorType) {
        super(Example.PROJECT_JOB_SCHEDULING, scoreDirectorType);
    }

    @Override
    protected ScoreDirectorFactoryConfig buildScoreDirectorFactoryConfig(ScoreDirectorType scoreDirectorType) {
        var scoreDirectorFactoryConfig = buildInitialScoreDirectorFactoryConfig();
        return switch (scoreDirectorType) {
            case CONSTRAINT_STREAMS, CONSTRAINT_STREAMS_JUSTIFIED -> scoreDirectorFactoryConfig
                    .withConstraintProviderClass(ProjectJobSchedulingConstraintProvider.class)
                    .withConstraintStreamImplType(ConstraintStreamImplType.BAVET);
            case INCREMENTAL -> scoreDirectorFactoryConfig
                    .withIncrementalScoreCalculatorClass(ProjectJobSchedulingIncrementalScoreCalculator.class);
            default -> throw new UnsupportedOperationException("Score director: " + scoreDirectorType);
        };
    }

    @Override
    protected SolutionDescriptor<Schedule> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(Schedule.class, Allocation.class);
    }

    @Override
    protected Schedule readOriginalSolution() {
        final SolutionFileIO<Schedule> solutionFileIO = new ProjectJobSchedulingSolutionFileIO();
        return solutionFileIO.read(new File("data/projectjobscheduling-B-7.json"));
    }

}
