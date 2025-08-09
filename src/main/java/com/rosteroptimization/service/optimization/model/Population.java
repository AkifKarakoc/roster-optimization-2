package com.rosteroptimization.service.optimization.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Population {

    private List<Chromosome> chromosomes;
    private boolean sorted = false;

    public Population(List<Chromosome> chromosomes) {
        this.chromosomes = new ArrayList<>(chromosomes);
        this.sorted = false;
    }

    public void sortByFitness() {
        if (!sorted) {
            chromosomes.sort((c1, c2) -> Double.compare(c2.getFitnessScore(), c1.getFitnessScore()));
            sorted = true;
        }
    }

    public Chromosome getBest() {
        sortByFitness();
        return chromosomes.isEmpty() ? null : chromosomes.get(0);
    }

    public Chromosome getWorst() {
        sortByFitness();
        return chromosomes.isEmpty() ? null : chromosomes.get(chromosomes.size() - 1);
    }

    public List<Chromosome> getBestN(int n) {
        sortByFitness();
        return chromosomes.stream()
                .limit(n)
                .map(Chromosome::copy)
                .collect(Collectors.toList());
    }

    public List<Chromosome> getWorstN(int n) {
        sortByFitness();
        return chromosomes.stream()
                .skip(Math.max(0, chromosomes.size() - n))
                .map(Chromosome::copy)
                .collect(Collectors.toList());
    }

    public void replaceWorstWith(List<Chromosome> newChromosomes) {
        if (newChromosomes.isEmpty()) return;

        sortByFitness();
        int replaceCount = Math.min(newChromosomes.size(), chromosomes.size());

        for (int i = 0; i < replaceCount; i++) {
            chromosomes.set(chromosomes.size() - 1 - i, newChromosomes.get(i).copy());
        }

        invalidateCache();
    }

    public void addChromosome(Chromosome chromosome) {
        chromosomes.add(chromosome);
        invalidateCache();
    }

    public boolean removeChromosome(Chromosome chromosome) {
        boolean removed = chromosomes.remove(chromosome);
        if (removed) {
            invalidateCache();
        }
        return removed;
    }

    public int size() {
        return chromosomes.size();
    }

    public boolean isEmpty() {
        return chromosomes.isEmpty();
    }

    public double getAverageFitness() {
        return chromosomes.stream()
                .mapToDouble(Chromosome::getFitnessScore)
                .average()
                .orElse(0.0);
    }

    public double getFitnessStandardDeviation() {
        double average = getAverageFitness();
        double variance = chromosomes.stream()
                .mapToDouble(c -> Math.pow(c.getFitnessScore() - average, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    public void invalidateCache() {
        sorted = false;
    }

    public Chromosome getRandomChromosome() {
        if (chromosomes.isEmpty()) return null;
        return chromosomes.get(new Random().nextInt(chromosomes.size()));
    }

    public Population copy() {
        List<Chromosome> copiedChromosomes = chromosomes.stream()
                .map(Chromosome::copy)
                .collect(Collectors.toList());
        return new Population(copiedChromosomes);
    }

    @Override
    public String toString() {
        return String.format("Population{size=%d, avgFitness=%.2f, bestFitness=%.2f}",
                size(), getAverageFitness(), getBest() != null ? getBest().getFitnessScore() : 0.0);
    }
}