package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.service.optimization.model.Chromosome;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class SelectionStrategy {

    public abstract Chromosome select(List<Chromosome> population);

    public static class TournamentSelection extends SelectionStrategy {
        private final int tournamentSize;

        public TournamentSelection(int tournamentSize) {
            this.tournamentSize = tournamentSize;
        }

        @Override
        public Chromosome select(List<Chromosome> population) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Chromosome best = null;

            for (int i = 0; i < tournamentSize; i++) {
                Chromosome candidate = population.get(random.nextInt(population.size()));
                if (best == null || candidate.getFitnessScore() > best.getFitnessScore()) {
                    best = candidate;
                }
            }

            return best;
        }
    }

    public static class RouletteWheelSelection extends SelectionStrategy {
        @Override
        public Chromosome select(List<Chromosome> population) {
            double totalFitness = population.stream()
                    .mapToDouble(Chromosome::getFitnessScore)
                    .sum();

            if (totalFitness <= 0) {
                return population.get(ThreadLocalRandom.current().nextInt(population.size()));
            }

            double randomValue = ThreadLocalRandom.current().nextDouble() * totalFitness;
            double currentSum = 0;

            for (Chromosome chromosome : population) {
                currentSum += chromosome.getFitnessScore();
                if (currentSum >= randomValue) {
                    return chromosome;
                }
            }

            return population.get(population.size() - 1);
        }
    }

    public static class RankSelection extends SelectionStrategy {
        @Override
        public Chromosome select(List<Chromosome> population) {
            int populationSize = population.size();
            int totalRank = (populationSize * (populationSize + 1)) / 2;
            int randomValue = ThreadLocalRandom.current().nextInt(totalRank) + 1;

            int currentSum = 0;
            for (int i = 0; i < populationSize; i++) {
                currentSum += (populationSize - i);
                if (currentSum >= randomValue) {
                    return population.get(i);
                }
            }

            return population.get(populationSize - 1);
        }
    }
}