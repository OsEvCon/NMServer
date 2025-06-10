package model;

public class UpdateResponse {
    private boolean updateNeeded;
    private String latestVersion;
    private String downloadUrl;
    private boolean forceUpdate;

    // Геттеры
    public boolean isUpdateNeeded() { return updateNeeded; }
    public String getLatestVersion() { return latestVersion; }
    public String getDownloadUrl() { return downloadUrl; }
    public boolean isForceUpdate() { return forceUpdate; }

    public void setUpdateNeeded(boolean updateNeeded) {
        this.updateNeeded = updateNeeded;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }
}
