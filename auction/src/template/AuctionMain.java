package template;

//the list of imports
import java.awt.image.LookupTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionMain implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;

	private ArrayList<Task> tasks = new ArrayList<Task>();
	private ArrayList<Task> opponentTasks = new ArrayList<Task>();

	private ArrayList<Task> supposedTasks = new ArrayList<Task>();
	private ArrayList<Task> opSupposedTasks = new ArrayList<Task>();

	private double currentCost = 0;
	private double opCurrentCost = 0;
	
	List<Integer> id = new ArrayList<Integer>();
	List<VehiclePlan> plansList = new ArrayList<VehiclePlan>();
	List<VehiclePlan> opponentPlan = new ArrayList<VehiclePlan>();
	List<VehiclePlan> newPlans = new ArrayList<VehiclePlan>();
	
	private Map<Integer, Double> costLookup;
	private ArrayList<Long> opBids = new ArrayList<Long>();
	private ArrayList<Long> ourBids = new ArrayList<Long>();
	private ArrayList<Long> ourWinBids = new ArrayList<Long>();
	private ArrayList<Long> opWinBids = new ArrayList<Long>();
	
	private Random rand = new Random();
	
	private int expectTaskSize = 17;
	private int dummyTaskSize = 13;
	private int	expectNumTasks = 10;
	
	private int lossStreak = 0;
	private int winStreak = 0;
	
	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;

		costLookup = computeCostTable();

	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {

		// store our last bid and the opponent's last bid
		if (winner != agent.id()) {
			opWinBids.add(bids[winner]);
			lossStreak++;
			winStreak = 0;
		} else {
			ourWinBids.add(bids[winner]);
			lossStreak = 0;
			winStreak++;
		}
		int opId = (agent.id() == 1) ? 0 : 1;
		ourBids.add(bids[agent.id()]);
		opBids.add(bids[opId]);

		System.out.println("[Auction agent " + agent.id()+"][Results] Our bid: " + bids[agent.id()] + ", opponent bid:  " + bids[bids.length-1-agent.id()]);

		actualizePlan(previous, winner);
		
	}

	@Override
	public Long askPrice(Task task) {
		double bid;
		
		if (dummyTaskSize < 15) {
			dummyTaskSize+=2;
			computeCostPerTask(dummyTaskSize);
			double prevCost = costLookup.get(dummyTaskSize-2);
			double nextCost = costLookup.get(dummyTaskSize);
			costLookup.put(dummyTaskSize-1, (nextCost+prevCost)/2);
		}
		
		if (expectTaskSize < opSupposedTasks.size()+supposedTasks.size()+1) expectTaskSize+=6;
		
		supposedTasks.clear();
		supposedTasks.addAll(tasks);
		supposedTasks.add(task);
		double ourAvgMargCost = (tasks.size() > 0) ? currentCost/tasks.size():Double.MAX_VALUE;
		newPlans = (new TaskAssignment(agent.vehicles(), supposedTasks)).getPlans();
		double ourTotalCost = totalCost(newPlans);
		double ourMarginalCost = ourTotalCost - currentCost;
		ourMarginalCost = (ourMarginalCost <= 0) ? ourAvgMargCost : ourMarginalCost;
		
		opSupposedTasks.clear();
		opSupposedTasks.addAll(opponentTasks);
		opSupposedTasks.add(task);
		double opAvgMargCost = (opponentTasks.size() > 0) ? opCurrentCost/opponentTasks.size():Double.MAX_VALUE;
		opponentPlan = (new TaskAssignment(agent.vehicles(), opSupposedTasks)).getPlans();
		double opTotalCost = totalCost(opponentPlan);
		double opMarginalCost = opTotalCost - opCurrentCost;
		opMarginalCost = (opMarginalCost <= 0) ? opAvgMargCost : opMarginalCost;


		if (opBids.size() == 0) {
			// first bid
			return (long) (costLookup.get(12)*1.3);
		} else {
			
			long opPrevBid = opBids.get(opBids.size()-1);
			long ourPrevBid = ourBids.get(ourBids.size()-1);
			double minMarginalCost = Math.min(opMarginalCost, ourMarginalCost);
			if (totalBid(ourBids) > currentCost && plansList.size() > (int)(expectNumTasks/1.5)) {
				return (long) (Math.min(minMarginalCost, costLookup.get(expectNumTasks)));
			}
			if (ourPrevBid < opPrevBid) {
				double scale = 1 + rand.nextDouble()*.25;
				double discount = Math.min(Math.pow(.8, Math.pow(0.75, winStreak)),.95);
				return  (long) (Math.min(opPrevBid*discount , scale*costLookup.get(expectNumTasks)));
			} else {
				long ourAvgBid = avgBid(ourWinBids);
				long opAvgBid = avgBid(opWinBids);
				System.out.println(opAvgBid);
				int ourMinWins = minWins(ourAvgBid);
				int	opMinWins = minWins(opAvgBid);
				Map<Integer, Double> gainForSteals = new HashMap<Integer, Double>();
				
				for (int i = opMinWins-(opponentTasks.size()-1); i >= 1; i--) {
					int taskSteals = i;
					int opTaskSize = opMinWins-taskSteals;
					double opCost = costLookup.get(Math.min(opTaskSize, dummyTaskSize));
					double opLoss = opAvgBid - opCost;
					double opTotalLoss = opLoss*opTaskSize;
					
					if (taskSteals + ourMinWins > expectTaskSize) taskSteals = expectTaskSize - ourMinWins;
					int ourTaskSize = Math.min(ourMinWins+taskSteals, expectTaskSize-opponentTasks.size());
					double ourCost = costLookup.get(Math.min(ourTaskSize, dummyTaskSize));
					double ourLoss = opAvgBid*Math.pow(.9, lossStreak) - ourCost;
					double ourTotalLoss = ourLoss*(ourTaskSize);
					
					double netGain = ourTotalLoss - opTotalLoss;
					if (netGain > -500) return (long) (opAvgBid*Math.pow(.9, lossStreak));
					gainForSteals.put(taskSteals, netGain);
				}
				System.out.println(gainForSteals);		
				
			}
		}
		return (long) (costLookup.get(expectNumTasks)*(1+rand.nextDouble()*.2));
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

    	if (tasks.size() == 0) {
    		List<Plan> plans = new ArrayList<Plan>();
    		while (plans.size() < vehicles.size())
    			plans.add(Plan.EMPTY);
    		return plans;
    	}
    	
    	
		ArrayList<Plan> plans = new ArrayList<Plan>();
		
		ArrayList<Task> taskList = new ArrayList<Task>();
		for(Task task : tasks) {
			if(id.contains(task.id)) {
				taskList.add(task);
			}
		}

		ArrayList<Plan> centralizedPlans = new ArrayList<Plan>();

		List<VehiclePlan> vPlans = (new TaskAssignment(vehicles, taskList)).getPlans();
		double totalCost = totalCost(vPlans);
		double totalBid = 0;
		for (Long bid : ourBids) {
			totalBid += bid;
		}
		double totalProfit = totalBid - totalCost;
		System.out.println("total profit: " + totalProfit);
		for (Vehicle vehicle : vehicles) {
			for (VehiclePlan vPlan : vPlans) {
				if (vPlan.getVehicle() == vehicle) {
					centralizedPlans.add(vPlan.getPlan());
				}
			}
		}
		return centralizedPlans;
	}


	public void actualizePlan(Task task, int winner) {
		//update task set
		if(winner == agent.id()) {
			tasks.add(task);
			id.add(task.id);
			plansList.clear();
			plansList = newPlans;
			// update the cost of our current plan
			currentCost = totalCost(plansList);
		} else {
			opponentTasks.add(task);
			opCurrentCost = totalCost(opponentPlan);
		}
	}


	private double totalCost(List<VehiclePlan> vPlans) {
		double cost = 0;
		for (VehiclePlan plan : vPlans) {
			cost += plan.getCost();
		}
		return cost;
	}
	
	/**
	 * computes the cost per task for a given amount of randomly generated taskset
	 * @return
	 */
	private Map<Integer, Double> computeCostTable() {
		
	    if (costLookup == null) {
	    	costLookup = new HashMap<Integer, Double>();
	    }
		
		for (int i = 1; i <= dummyTaskSize; i+=2) {
			computeCostPerTask(i);
		}
		
		for (int i = 2; i <= dummyTaskSize-1; i+=2) {
			double prevCost = costLookup.get(i-1);
			double nextCost = costLookup.get(i+1);
			costLookup.put(i, (nextCost+prevCost)/2);
		}
		costLookup.put(0, 0.0);
		return costLookup;
	}
	
	private void computeCostPerTask(int taskSize) {
	    long startTime = System.currentTimeMillis();
		List<City> cities = topology.cities();
		int size = cities.size();
		double totalSampleCost = 0;
		int sampleSize;
		if (taskSize <= 5) {
			sampleSize = 7;
		} else if (taskSize <= 11) {
			sampleSize = 5;
		} else if (taskSize <= 19) {
			sampleSize = 3;
		} else if (taskSize <= 25) {
			sampleSize = 2;
		} else {
			sampleSize = 1;
		}
		for (int k = 0; k < sampleSize; k++) {
			ArrayList<Task> dummyTasks = new ArrayList<Task>();
			for (int j = 0; j < taskSize; j++) {
				int idx1 = 0;
				int idx2 = 0;
				while (idx1 == idx2) {
					idx1 = rand.nextInt(size);
					idx2 = rand.nextInt(size);
				}
				City city1 = cities.get(idx1);
				City city2 = cities.get(idx2);
				Task task = new Task(j, city1, city2, 0, distribution.weight(city1, city2));
				dummyTasks.add(task);
			}
			List<VehiclePlan> vPlans = (new TaskAssignment(agent.vehicles(), dummyTasks)).getPlans();
			double totalCost = totalCost(vPlans);
			totalSampleCost += totalCost/taskSize;
		}
		double costPerTask = totalSampleCost/sampleSize;
		System.out.println(taskSize + " : " + costPerTask);
		costLookup.put(taskSize, costPerTask);
		System.out.println("Running time: " + (System.currentTimeMillis()-startTime) + " ms");
	}
	
	/**
	 * finds the average of a list of bids
	 * @param bids
	 * @return average of all bids
	 */
	private long avgBid(List<Long> bids) {
		if (bids.size() == 0) {
			return (long) (costLookup.get(expectNumTasks)*1);
		}
		long totalBid = 0;
		for (Long bid : bids) {
			totalBid+=bid;
		}
		return totalBid/bids.size();
	}
	
	private long totalBid(List<Long> bids) {
		long totalBid = 0;
		for (Long bid : bids) {
			totalBid+=bid;
		}
		return totalBid;
	}
	
	/**
	 * finds the least number of wins required to not lose money if all future bids are
	 * are at the given price
	 * @param avgBid
	 * @return
	 */
	private int minWins(long avgBid) {
		int wins = 0;
		double closestCost = Double.MAX_VALUE;
		for (Map.Entry<Integer, Double> entry : costLookup.entrySet()) {
			if (entry.getValue() <= closestCost && entry.getValue() >= avgBid) {
				wins = entry.getKey();
				closestCost = entry.getValue();
			}
		}
		return wins;
	}

}
