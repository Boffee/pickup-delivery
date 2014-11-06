package template;

import logist.task.Task;

public class TaskNode {
	private Task currTask;
	private TaskNode prevTaskNode;
	private TaskNode nextTaskNode;
	private int time;
	
	public TaskNode(Task _currTask, TaskNode _prevTaskNode, TaskNode _nextTaskNode) {
		currTask = _currTask;
		prevTaskNode = _prevTaskNode;
		nextTaskNode = _nextTaskNode;
	}
	
	/**
	 * switch the position between two taskNodes
	 * @param otherTaskNode
	 */
	public void switchWith(TaskNode otherTaskNode) {
		TaskNode otherPrev = otherTaskNode.getPrevTaskNode();
		TaskNode otherNext = otherTaskNode.getNextTaskNode();
		
		TaskNode currPrev = prevTaskNode;
		TaskNode currNext = nextTaskNode;
		
		otherPrev.setNextTaskNode(this);
		otherNext.setPrevTaskNode(this);
		
		currPrev.setNextTaskNode(otherTaskNode);
		currNext.setPrevTaskNode(otherTaskNode);
		
		otherTaskNode.setNextTaskNode(currNext);
		otherTaskNode.setPrevTaskNode(currPrev);
		
		this.setNextTaskNode(otherNext);
		this.setPrevTaskNode(otherPrev);
		
		int otherTime = otherTaskNode.getTime();
		int currTime = time;
		otherTaskNode.setTime(currTime);
		time = otherTime;
	}

	public Task getCurrTask() {
		return currTask;
	}

	public TaskNode getPrevTaskNode() {
		return prevTaskNode;
	}

	public void setPrevTaskNode(TaskNode prevTaskNode) {
		this.prevTaskNode = prevTaskNode;
	}

	public TaskNode getNextTaskNode() {
		return nextTaskNode;
	}

	public void setNextTaskNode(TaskNode nextTaskNode) {
		this.nextTaskNode = nextTaskNode;
	}

	public Integer getTime() {
		return time;
	}

	public void setTime(Integer time) {
		this.time = time;
	}
	
	
}
