package olfbrower;

import java.util.Date;

public class BroswerData {
	private String userModuleId="null";
	private String updatePaths="null";
	private String paths="null";
	private boolean defineModule = false;
	private long lastLoadTime=new Date().getTime();
	public static int LOADING=0;
	public static int CONPLETED=1;
	public static int FAIL=2;
	public static int NO_URL=3;
	private int completedStatus=BroswerData.NO_URL;
	public void setUserModuleId(String userModuleId) {
		this.userModuleId = userModuleId;
	}
	public String getUserModuleId() {
		return userModuleId;
	}
	public void setUpdatePaths(String updatePaths) {
		this.updatePaths = updatePaths;
	}
	public String getUpdatePaths() {
		return updatePaths;
	}
	public void setPaths(String paths) {
		this.paths = paths;
	}
	public String getPaths() {
		return paths;
	}
	public void setDefineModule(boolean defineModule) {
		this.defineModule = defineModule;
	}
	public boolean isDefineModule() {
		return defineModule;
	}
	public void setLastLoadTime(long lastLoadTime) {
		this.lastLoadTime = lastLoadTime;
	}
	public long getLastLoadTime() {
		return lastLoadTime;
	}
	public void setCompletedStatus(int completedStatus) {
		this.completedStatus = completedStatus;
	}
	public int getCompletedStatus() {
		return completedStatus;
	}

}
