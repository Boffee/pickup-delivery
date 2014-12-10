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
public class Auction implements AuctionBehavior {
 
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;
    private Vehicle vehicle;
    private City currentCity;
 
    private int count = 1;
 
    private ArrayList<Task> tasks = new ArrayList<Task>();
    private ArrayList<Task> opponentTasks = new ArrayList<Task>();
 
    private ArrayList<Integer> id = new ArrayList<Integer>();
    private ArrayList<Task> supposedTasks = new ArrayList<Task>();
    private ArrayList<Task> opSupposedTasks = new ArrayList<Task>();
 
 
    private double averageRatio = 1;
    private double difference = 0;
    private double ratio = 0;
    private double averageDifference = 0;
    private double totalBids = 0;
    private int auctionCount = 0;
    private double opponentBid = 0;
    private double opponentDifference = 0;
    private double averageOpponentDifference = 0;
 
    private double minimalBid = Double.MAX_VALUE;
    private final double BID_THRESHOLD = 1000;
 
    private boolean bidAgainstOpponent = false;
    private int riskyLoose = 0;
 
    private double currentCost = 0;
    private double opCurrentCost = 0;
     
    List<VehiclePlan> plansList = new ArrayList<VehiclePlan>();
    List<VehiclePlan> opponentPlan = new ArrayList<VehiclePlan>();
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
            totalBids += bids[agent.id()];
 
        }
        // if we loose by trying to outsmart opponent
        if(winner != agent.id() && bidAgainstOpponent) {
            riskyLoose++;
        } else if(winner == agent.id() && bidAgainstOpponent){ // if we win with the outsmarting strategy
            riskyLoose = 0;
        }
 
            actualizePlan(previous, winner);
 
 
        // then we update the bid history of our vehicles
        updateHistory(bids[agent.id()], bids[bids.length-1-agent.id()]);
 
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
        opSupposedTasks.clear();
        opSupposedTasks.addAll(opponentTasks);
        opSupposedTasks.add(task);
 
        opponentPlan = (new TaskAssignment(agent.vehicles(), opSupposedTasks)).getPlans();
        opponentBid = totalCost(opponentPlan) - opCurrentCost;
 
        if(opponentBid <=0) {
            opponentBid = 650;
        }
 
        if(bid <=0) {
            if(minimalBid <= BID_THRESHOLD)
                minimalBid = BID_THRESHOLD;
            bid = minimalBid;
        }
 
        double opponentEstimation = opponentBid + averageOpponentDifference;
 
        bid = modifyBid(bid);
        System.out.printf("[Auction agent " + agent.id()+"][Comparator] Marginal cost: %.3f, calculated bid: %.3f, opponent estimated bid: %.3f\n",marginalCost,bid,opponentEstimation);
 
        if(bid > opponentEstimation && opponentEstimation > marginalCost) { // if your bid is lower than the estimated bid of the opponent
            if(riskyLoose > 1) { // if this strategy failed more than two times in a row
                bid = (bid + opponentBid + averageOpponentDifference)/2; // we reduce the bid by computing an average between the 2 possible bids we have
            } else {
                bid = opponentBid + averageOpponentDifference; // we try to outsmart him by bidding something lower than his bid but higher than our computed bid
            }
            bidAgainstOpponent = true;
            System.out.printf("[Auction agent " + agent.id()+"][Bid against opponent bid] Bid for task: " + count + ", agent " + agent.id() + " bids %.3f\n",bid);
 
        } else {
            System.out.printf("[Auction agent " + agent.id()+"][Normal bid] Bid for task: " + count + ", agent " + agent.id() + " bids %.3f\n",bid);
            bidAgainstOpponent = false;
 
        }
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
        }
 
        else{
            plans = astarPlans;
            System.out.println("[Auction agent " + agent.id()+"][End] A* plan built");
 
        }
        //  }
        return plans;
    }
 
 
    /**
     * Update the bid history by adding the ratio between the bids to a list
     * @param bid
     * @param opponentBid
     */
    public void updateHistory(double bid, double opponentBid) {
        if(opponentBid < minimalBid) {
            minimalBid = opponentBid;
        }
 
        ratio += (opponentBid/bid); // ratio to see how much we can improve/decrease our bid and stay under the opponentBid
        difference += (opponentBid-bid);
        auctionCount++;
        opponentDifference += (opponentBid - this.opponentBid);
        averageOpponentDifference = opponentDifference/auctionCount;
 
        averageDifference = difference/auctionCount; 
        averageRatio = ratio/auctionCount;
        //System.out.println("[Auction][History] History updated, average ratio: " + averageRatio + ", average difference: " + averageDifference);
    }
 
    /**
     * Add or remove value to our bid
     * @param bid
     * @return
     */
    private double modifyBid(double bid) {
        System.out.printf("[Auction agent " + agent.id()+"][Bid alteration] Bid: " + bid + ", average ratio: %.3f, average difference: %.3f\n",averageRatio,averageDifference);
        double strategy1 = bid*averageRatio;
        double strategy2 = bid + averageDifference;
        double modifiedBid;
        if(strategy1 > strategy2) {
            modifiedBid = strategy1;
        } else modifiedBid = strategy2;
         
        if(modifiedBid < bid)
            return bid;
        else return modifiedBid;
         
         
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
 
 
}