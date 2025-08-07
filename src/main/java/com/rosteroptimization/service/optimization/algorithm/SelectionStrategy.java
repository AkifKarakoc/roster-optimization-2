package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.service.optimization.model.Chromosome;
import java.util.*;

public interface SelectionStrategy {
    Chromosome select(List<Chromosome> population, Random random);
}

class TournamentSelection implements SelectionStrategy {
    private final int tournamentSize;

    public TournamentSelection(int size) {
        this.tournamentSize = size;
    }

    @Override
    public Chromosome select(List<Chromosome> population, Random random) {
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

class RouletteWheelSelection implements SelectionStrategy {
    @Override
    public Chromosome select(List<Chromosome> population, Random random) {
        double totalFitness = population.stream()
                .mapToDouble(Chromosome::getFitnessScore)
                .sum();

        double randomValue = random.nextDouble() * totalFitness;
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

class RankSelection implements SelectionStrategy {
    @Override
    public Chromosome select(List<Chromosome> population, Random random) {
        int populationSize = population.size();
        int totalRank = (populationSize * (populationSize + 1)) / 2;
        int randomValue = random.nextInt(totalRank) + 1;

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