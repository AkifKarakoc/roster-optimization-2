package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.service.optimization.model.Chromosome;
import com.rosteroptimization.service.optimization.model.OptimizationRequest;

// Crossover Strategy Interface
interface CrossoverStrategy {
    Chromosome crossover(Chromosome parent1, Chromosome parent2, OptimizationRequest request);
}