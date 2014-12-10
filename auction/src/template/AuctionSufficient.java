package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
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
public class AuctionSufficient implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;

	private int count = 1;

	private ArrayList<Task> tasks = new ArrayList<Task>();

	private ArrayList<Integer> id = new ArrayList<Integer>();
	private ArrayList<Task> supposedTasks = new ArrayList<Task>();


	private double currentCost = 0;
	List<VehiclePlan> plansList = new ArrayList<VehiclePlan>();
	List<VehiclePlan> newPlans = new ArrayList<VehiclePlan>();


	protected static double shortestEdge = 0;
	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);

		for (City city : topology.cities()) {
			for (City neighbor : city.neighbors()) {
				double edge = city.distanceTo(neighbor);
				shortestEdge = (edge > shortestEdge) ? edge : shortestEdge;
			}
		}
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {

		System.out.println("[Auction agent " + agent.id()+"][Results] Our bid: " + bids[agent.id()] + ", opponent bid:  " + bids[bids.length-1-agent.id()]);
		if (winner == agent.id()) {
			System.out.println("[Auction agent " + agent.id()+"][Winner] The winner is agent " + winner);
			actualizePlan(previous);

		}

	}

	@Override
	public Long askPrice(Task task) {
		double bid;

		// compute our bid with the marginal cost and an estimated bid of the opponent 
		// by computing his plan according to our agent because we don't know his starting cities
		supposedTasks.clear();
		supposedTasks.addAll(tasks);
		supposedTasks.add(task);

		TaskAssignment ta = new TaskAssignment(agent.vehicles(), supposedTasks);
		newPlans.clear();
		newPlans = ta.getPlans();

		double marginalCost = totalCost(newPlans) - currentCost; // marginal cost
		bid = marginalCost;

		System.out.printf("[Auction agent " + agent.id()+"][Normal bid] Bid for task: " + count + ", agent " + agent.id() + " bids %.3f\n",bid);


		count++;

		return (long) bid;
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
		ArrayList<Plan> astarPlans = new ArrayList<Plan>();
		double astarCost = Double.MAX_VALUE;
		ArrayList<Plan> centralizedPlans = new ArrayList<Plan>();
		double centralizedCost = Double.MAX_VALUE;

		if(tasks.size() <= 10) {

			Plan plan = new ASTAR(vehicle, taskList, null, Auction.shortestEdge).getPlan();
			double cost = plan.totalDistance()*vehicle.costPerKm();
			int index = 0;

			for(int i = 1;i<agent.vehicles().size();i++) {
				Plan otherPlan = new ASTAR(agent.vehicles().get(i), taskList, null, Auction.shortestEdge).getPlan();
				double otherCost = otherPlan.totalDistance()*agent.vehicles().get(i).costPerKm();
				if(otherCost < cost) {
					plan = otherPlan;
					cost = otherCost;
					index = i;
				}
			}

			for(int j = 0; j<agent.vehicles().size();j++) {
				if(j==index) {
					astarPlans.add(plan);
				} else {
					astarPlans.add(Plan.EMPTY);
				}
			}

			astarCost = cost;
		}

		List<VehiclePlan> vPlans = (new TaskAssignment(vehicles, taskList)).getPlans();
		centralizedCost = totalCost(vPlans);

		for (Vehicle vehicle : vehicles) {
			for (VehiclePlan vPlan : vPlans) {
				if (vPlan.getVehicle() == vehicle) {
					centralizedPlans.add(vPlan.getPlan());
				}
			}
		}

		if(centralizedCost <= astarCost || astarPlans.isEmpty()) {
			plans = centralizedPlans;
			System.out.println("[Auction agent " + agent.id()+"][End] Centralized plan built");
		} else {
			plans = astarPlans;
			System.out.println("[Auction agent " + agent.id()+"][End] A* plan built");
		}
		
		return plans;
	}


	public void actualizePlan(Task task) {
		//update task set
		tasks.add(task);
		id.add(task.id);
		plansList.clear();
		plansList = newPlans;
		// update the cost of our current plan
		currentCost = totalCost(plansList);

	}


	private double totalCost(List<VehiclePlan> vPlans) {
		double cost = 0;
		for (VehiclePlan plan : vPlans) {
			cost += plan.getCost();
		}
		return cost;
	}


}
