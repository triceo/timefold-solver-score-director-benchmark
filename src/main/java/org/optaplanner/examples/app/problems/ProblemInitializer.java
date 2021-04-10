package org.optaplanner.examples.app.problems;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicType;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.impl.constructionheuristic.ConstructionHeuristicPhase;
import org.optaplanner.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.heuristic.HeuristicConfigPolicy;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleSupport;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;
import org.optaplanner.core.impl.solver.event.SolverEventSupport;
import org.optaplanner.core.impl.solver.recaller.BestSolutionRecaller;
import org.optaplanner.core.impl.solver.recaller.BestSolutionRecallerFactory;
import org.optaplanner.core.impl.solver.scope.SolverScope;
import org.optaplanner.core.impl.solver.termination.BasicPlumbingTermination;
import org.optaplanner.core.impl.solver.termination.Termination;
import org.optaplanner.core.impl.solver.termination.TerminationFactory;
import org.optaplanner.examples.app.params.Example;
import org.optaplanner.examples.app.params.ScoreDirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * XLS-based examples can not read initialized solution from the file.
 * Therefore we have to initialize the solution ourselves, by running the CH on the fastest possible score director.
 */
public final class ProblemInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProblemInitializer.class);

    private static final Map<Example, Object> SOLUTIONS = new EnumMap<>(Example.class);

    public static <Solution_> Solution_ getSolution(Example example, SolutionDescriptor<Solution_> solutionDescriptor,
                                                    Function<ScoreDirector, ScoreDirectorFactoryConfig> configFunction,
                                                    Supplier<Solution_> solutionSupplier) {
        final ScoreDirector fastestPossibleScoreDirector = Arrays.stream(ScoreDirector.values())
                .filter(example::isSupportedOn)
                .max(ScoreDirector::compareTo)
                .orElseThrow();
        final ScoreDirectorFactoryConfig config = configFunction.apply(fastestPossibleScoreDirector);
        final InnerScoreDirectorFactory<Solution_, ?> scoreDirectorFactory =
                ScoreDirector.buildScoreDirectorFactory(config, solutionDescriptor);
        return (Solution_) SOLUTIONS.computeIfAbsent(example,
                e -> initialize(example, solutionSupplier.get(), scoreDirectorFactory));
    }

    private static <Solution_> Solution_ initialize(Example example, Solution_ uninitializedSolution,
                                                    InnerScoreDirectorFactory<Solution_, ?> scoreDirectorFactory) {
        try (final InnerScoreDirector<Solution_, ?> scoreDirector =
                     scoreDirectorFactory.buildScoreDirector(false, false)) {
            scoreDirector.setWorkingSolution(uninitializedSolution);
            scoreDirector.triggerVariableListeners();
            Score<?> score = scoreDirector.calculateScore();
            if (score.isSolutionInitialized()) { // No need to do anything.
                LOGGER.info("Example {} already initialized.", example);
                return uninitializedSolution;
            }
            LOGGER.info("Initializing example {}.", example);
            // Configure the construction heuristic.
            ConstructionHeuristicPhaseConfig config = new ConstructionHeuristicPhaseConfig()
                    .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT);
            DefaultConstructionHeuristicPhaseFactory<Solution_> factory = new DefaultConstructionHeuristicPhaseFactory<>(config);

            HeuristicConfigPolicy<Solution_> policy = new HeuristicConfigPolicy<>(EnvironmentMode.REPRODUCIBLE, null, null, null, scoreDirectorFactory);
            BestSolutionRecaller<Solution_> bestSolutionRecaller =
                    BestSolutionRecallerFactory.create().buildBestSolutionRecaller(EnvironmentMode.REPRODUCIBLE);
            bestSolutionRecaller.setSolverEventSupport(new SolverEventSupport<>(null));
            Termination<Solution_> termination = TerminationFactory.<Solution_>create(new TerminationConfig())
                    .buildTermination(policy, new BasicPlumbingTermination<>(false));
            ConstructionHeuristicPhase<Solution_> constructionHeuristicPhase =
                    factory.buildPhase(0, policy, bestSolutionRecaller, termination);
            constructionHeuristicPhase.setSolverPhaseLifecycleSupport(new PhaseLifecycleSupport<>());

            // Start the construction heuristic.
            SolverScope<Solution_> solverScope = new SolverScope<>();
            solverScope.setBestSolution(uninitializedSolution);
            solverScope.setScoreDirector(scoreDirector);
            solverScope.startingNow();
            bestSolutionRecaller.solvingStarted(solverScope);
            constructionHeuristicPhase.solvingStarted(solverScope);
            constructionHeuristicPhase.solve(solverScope);
            constructionHeuristicPhase.solvingEnded(solverScope);
            bestSolutionRecaller.solvingEnded(solverScope);
            solverScope.endingNow();
            if (!scoreDirector.calculateScore().isSolutionInitialized()) {
                throw new IllegalStateException("Impossible state: uninitialized after the end of CH.");
            }
            LOGGER.info("Example {} initialized.", example);
            return solverScope.getBestSolution();
        }
    }

}