package template;

import logist.task.Task;

public class TaskNode {
	private Task currTask;
	private TaskNode prevTaskNode;
	private TaskNode nextTaskNode;
	
	public TaskNode(Task _currTask, TaskNode _prevTaskNode, TaskNode _nextTaskNode) {
		currTask = _currTask;
		prevTaskNode = _prevTaskNode;
		nextTaskNode = _nextTaskNode;
	}
	
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
	
	
}
