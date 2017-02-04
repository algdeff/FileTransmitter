package FileTransmitter.Logic.DistributedComputing;

import java.util.concurrent.Callable;

public interface IRemoteTaskEntity {

    public void setTaskName(String taskName);
    public String getTaskName();

    public void setAssignedClientName(String assignedClientName);
    public String getAssignedClientName();

    public void setTaskUnit(Callable<Object> taskUnit);
    public Callable<Object> getTaskUnit();

    public void setCompletedTaskResult(Object result);
    public Object getCompletedTaskResult();

}
