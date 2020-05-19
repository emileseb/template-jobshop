package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.PriorityRules;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;

public class GreedySolver implements Solver {

    private PriorityRules rule;

    public GreedySolver(PriorityRules rule){
        super();
        this.rule = rule;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        ResourceOrder order = new ResourceOrder(instance);
        int[] nextStartigTimeForMachine = new int[order.instance.numMachines];
        int[] nextStartigTimeForJob = new int[order.instance.numJobs];
        //Initialisation-------------------------------
        for (int m = 0; m < instance.numMachines;m++){
            nextStartigTimeForMachine[m]=0;
        }
        for (int j = 0; j < instance.numJobs;j++){
            nextStartigTimeForJob[j]=0;
        }
        ArrayList<Task> realisableTaskList = new ArrayList<>();
        for (int j =0; j < instance.numJobs; j++){
            realisableTaskList.add(new Task(j,0));
        }
        //Boucle---------------------------------------
        int m, t_startingTime;
        Task t;
        while (realisableTaskList.size()>0 &&  (deadline - System.currentTimeMillis() > 1)){
            //Choose rule for the greedy filling :
            switch (this.rule){
                case SPT:
                    t = this.getSPT(order, realisableTaskList);
                    break;
                case LPT:
                    t = this.getLPT(order, realisableTaskList);
                    break;
                case SRPT:
                    t = this.getSRPT(order, realisableTaskList);
                    break;
                case LRPT:
                    t= this.getLRPT(order,realisableTaskList);
                    break;
                case EST_SPT:
                    t = this.getEST(PriorityRules.SPT, order, realisableTaskList, nextStartigTimeForMachine, nextStartigTimeForJob);
                    break;
                case EST_LPT:
                    t = this.getEST(PriorityRules.LPT, order, realisableTaskList, nextStartigTimeForMachine, nextStartigTimeForJob);
                    break;
                case EST_SRPT:
                    t = this.getEST(PriorityRules.SRPT, order, realisableTaskList, nextStartigTimeForMachine, nextStartigTimeForJob);
                    break;
                case EST_LRPT:
                    t = this.getEST(PriorityRules.LRPT, order, realisableTaskList, nextStartigTimeForMachine, nextStartigTimeForJob);
                    break;
                default:
                    System.out.println("ERROR : Invalid Priority chosen (Idk the problem bro, sorry");
                    t = realisableTaskList.get(0);
            }
            //------------------------------------
            realisableTaskList.remove(t);
            t_startingTime = Math.max(nextStartigTimeForJob[t.job], nextStartigTimeForMachine[instance.machine(t)]);
            m = instance.machine(t);
            order.tasksByMachine[m][order.nextFreeSlot[m]] = t;
            order.nextFreeSlot[m] ++;
            nextStartigTimeForMachine[m] = t_startingTime + instance.duration(t);
            nextStartigTimeForJob[t.job] = t_startingTime + instance.duration(t);
            if (t.task+1 < instance.numTasks) {
                realisableTaskList.add(new Task(t.job, t.task + 1));
            }
        }
        //----------------------------------------------        }
        Schedule schedule = order.toSchedule();
        if (deadline - System.currentTimeMillis() <= 1) {
            return new Result(instance, order.toSchedule(), Result.ExitCause.Timeout);
        }else {
            return new Result(instance, order.toSchedule(), Result.ExitCause.ProvedOptimal);
        }
    }

    //(Shortest Processing Time)
    private Task getSPT(ResourceOrder order, ArrayList<Task> realisableTaskList){
        Task t = realisableTaskList.get(0);
        for (Task i : realisableTaskList){
            if (order.instance.duration(i) < order.instance.duration(t)){
                    t=i;
            }
        }
        return t;
    }

    //(Longest Processing Time)
    private Task getLPT(ResourceOrder order, ArrayList<Task> realisableTaskList){
        Task t = realisableTaskList.get(0);
        for (Task i : realisableTaskList){
            if (order.instance.duration(i) > order.instance.duration(t)){
                    t=i;
            }
        }
        return t;
    }

    //(Shortest Remaining Processing Time) V2
    private Task getSRPT(ResourceOrder order, ArrayList<Task> realisableTaskList){
        Task t = realisableTaskList.get(0);
        int bestJob =-1;
        int remainingTimeByJob;
        //On calcul le temps restant de chaque Job
        for (int i =0; i < realisableTaskList.size() ; i++){
            remainingTimeByJob = 0;
            for (int tacheRestante = realisableTaskList.get(i).task ; tacheRestante < order.instance.numTasks ; tacheRestante++){
                remainingTimeByJob += order.instance.duration(realisableTaskList.get(i).job,tacheRestante);
            }
            if (bestJob==-1)
                bestJob = remainingTimeByJob;
            else if (remainingTimeByJob < bestJob){
                    t=realisableTaskList.get(i);
            }
        }
        return t;
    }

    //(Longest Remaining Processing Time)
    private Task getLRPT(ResourceOrder order, ArrayList<Task> realisableTaskList){
        Task t = realisableTaskList.get(0);
        int bestJob =-1;
        int remainingTimeByJob;
        //On calcul le temps restant de chaque Job
        for (int i =0; i < realisableTaskList.size() ; i++){
            remainingTimeByJob=0;
            for (int tacheRestante = realisableTaskList.get(i).task ; tacheRestante < order.instance.numTasks ; tacheRestante++){
                remainingTimeByJob += order.instance.duration(realisableTaskList.get(i).job,tacheRestante);
            }
            if (bestJob==-1)
                bestJob = remainingTimeByJob;
            else if (remainingTimeByJob > bestJob)
                t=realisableTaskList.get(i);
        }
        return t;
    }

    //(EST)
    private Task getEST(PriorityRules rule, ResourceOrder order, ArrayList<Task> realisableTaskList, int[] nextStartigTimeForMachine, int[] nextStartigTimeForJob){
        ArrayList<Task> ESTTaskList = new ArrayList<>();
        //On cherche la date la plus courte à laquelle une tache peut être réalisée:
            //Initialisation -------------------------------------------------------
        int earliestdate = -1;
            //Boucle ---------------------------------------------------------------
        int t_startingtime;
        for (Task t : realisableTaskList){
            t_startingtime = Math.max(nextStartigTimeForJob[t.job], nextStartigTimeForMachine[order.instance.machine(t)]);
            if ((earliestdate ==-1) || (t_startingtime < earliestdate)){
                earliestdate = t_startingtime;
                ESTTaskList.clear();
                ESTTaskList.add(t);
            } else if (t_startingtime == earliestdate){
                ESTTaskList.add(t);
            }
        }
        //on gloutonne sur cette nouvelle liste selon les règles précédentes.
        switch (rule){
            case SPT:
                return(getSPT(order, ESTTaskList));
            case LPT:
                return(getLPT(order, ESTTaskList));
            case SRPT:
                return(getSRPT(order, ESTTaskList));
            case LRPT:
                return(getLRPT(order,ESTTaskList));
            default:
                System.out.println("ERROR : Invalid Priority chosen (don't worry mate, it's the dev fault (Emile)");
                return getLPT(order, ESTTaskList);
        }
    }
}
