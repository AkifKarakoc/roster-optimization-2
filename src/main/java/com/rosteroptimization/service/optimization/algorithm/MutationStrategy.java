package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.service.optimization.model.Chromosome;
import com.rosteroptimization.service.optimization.model.OptimizationRequest;

interface MutationStrategy {
    void mutate(Chromosome chromosome, double mutationRate, OptimizationRequest request);
}