package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.PriorityRules;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swap with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        void applyOn(ResourceOrder order) {
            Task task1 = order.tasksByMachine[machine][t1];
            Task task2 = order.tasksByMachine[machine][t2];
            order.tasksByMachine[machine][t1] = task2;
            order.tasksByMachine[machine][t2] = task1;
        }

        @Override
        public String toString() {
            return "Swap(m = " + this.machine + "; t1 = " + this.t1 + "; t2 = " + this.t2 + ")";
        }
    }


    @Override
    public Result solve(Instance instance, long deadline) {
        Solver greedy = new GreedySolver(PriorityRules.EST_LRPT);
        ResourceOrder order = new ResourceOrder(greedy.solve(instance, deadline).schedule);

        ResourceOrder orderTest;
        int testedMakespan, bestMakespan = order.toSchedule().makespan();

        boolean amelioration = true;
        while (amelioration &&  (deadline - System.currentTimeMillis() > 1)) {
            amelioration = false;
            for (Block block : blocksOfCriticalPath(order)) {
                for (Swap swap : neighbors(block)) {
                    orderTest = order.copy();
                    swap.applyOn(orderTest);
                    testedMakespan = orderTest.toSchedule().makespan();
                    if (testedMakespan < bestMakespan) {
                        order = orderTest.copy();
                        bestMakespan = testedMakespan;
                        amelioration = true; // s'il n'en trouve pas, on ne passe pas ici i.e. on reste à false : on sort.
                    }
                }
            }
        }
        if (deadline - System.currentTimeMillis() < 1) {
            return new Result(instance, order.toSchedule(), Result.ExitCause.Timeout);
        }else {
            return new Result(instance, order.toSchedule(), Result.ExitCause.ProvedOptimal);
        }
    }

    private List<Block> blocksOfCriticalPath(ResourceOrder order) {
        List<Block> blocklist = new ArrayList<>();
        List<Task> tasklist = order.toSchedule().criticalPath();
        int first_task, orderPointeur, criticalPathPointer ;
        for (int machine=0; machine <order.toSchedule().pb.numMachines;machine++) {
            orderPointeur=0;
            while (orderPointeur < (order.toSchedule().pb.numJobs-1)) {
                if (tasklist.contains(order.tasksByMachine[machine][orderPointeur])) {
                    criticalPathPointer= tasklist.indexOf(order.tasksByMachine[machine][orderPointeur]);
                    if(machine==order.toSchedule().pb.machine(tasklist.get(criticalPathPointer+1))) {
                        first_task = orderPointeur;
                        while (criticalPathPointer<(tasklist.size()-1) && machine==order.toSchedule().pb.machine(tasklist.get(criticalPathPointer + 1))) {
                            criticalPathPointer++;
                            orderPointeur++;
                        }
                        blocklist.add(new Block(machine, first_task, orderPointeur));
                    }
                }
                orderPointeur++;
            }
        }
        return blocklist;
    }



    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    private List<Swap> neighbors(Block block) {
        List<Swap> swapList = new ArrayList<>();
        int machine = block.machine;
        int firstTask = block.firstTask, lastTask = block.lastTask;
        int secondTask = firstTask +1, beforelastTask = lastTask -1; //On considère que les Taches sont dans l'ordre
        if (secondTask == lastTask){
            swapList.add(new Swap(machine,firstTask,lastTask));
        }else {
            swapList.add(new Swap(machine, firstTask, secondTask));
            swapList.add(new Swap(machine, beforelastTask, lastTask));
        }
        return swapList;
    }

}