package hlmp.SubProtocol.FileTransfer;

import hlmp.SubProtocol.FileTransfer.Interfaces.ManageDirectory;


public class FileData {
	
    private int partSize;
    private int fileTimeOut;
    private int fileRiseUp;
    private int timeIntervalTimer;
    private int simulteneusUpload;
    private int simulteneusDownload;
    private String sharedDir;
    private String downloadDir;
    private FileInformationList fileList;

    
    public FileData(ManageDirectory md) {
        this.partSize = 1024 * 1024 / 8;
        this.fileTimeOut = 10;
        this.fileRiseUp = 5;
        this.timeIntervalTimer = 1000;
        this.simulteneusUpload = 1;
        this.simulteneusDownload = 1;
        this.fileList = new FileInformationList();
        this.downloadDir = md.createDownloadDir();
        this.sharedDir = md.createSharedDir();
        md.loadSharedFiles(this);
    }

    public int getPartSize() {
		return this.partSize;
	}

	public void setPartSize(int partSize) {
		this.partSize = partSize;
	}
	
	public String getSharedDir() {
		return sharedDir;
	}
	
	public String getDownloadDir() {
		return this.downloadDir;
	}

	public void setPartSize(String downloadDir) {
		this.downloadDir = downloadDir;
	}

	public int getFileTimeOut() {
		return this.fileTimeOut;
	}

	public void setFileTimeOut(int fileTimeOut) {
		this.fileTimeOut = fileTimeOut;
	}

	public int getFileRiseUp() {
		return this.fileRiseUp;
	}

	public void setFileRiseUp(int fileRiseUp) {
		this.fileRiseUp = fileRiseUp;
	}

	public int getTimeIntervalTimer() {
		return this.timeIntervalTimer;
	}

	public void setTimeIntervalTimer(int timeIntervalTimer) {
		this.timeIntervalTimer = timeIntervalTimer;
	}

	public int getSimulteneusUpload() {
		return this.simulteneusUpload;
	}

	public void setSimulteneusUpload(int simulteneusUpload) {
		this.simulteneusUpload = simulteneusUpload;
	}

	public int getSimulteneusDownload() {
		return this.simulteneusDownload;
	}

	public void setSimulteneusDownload(int simulteneusDownload) {
		this.simulteneusDownload = simulteneusDownload;
	}

	public FileInformationList getFileList() {
        return fileList;
    }
	
	public void addFile(FileInformation fileInformation) {
		fileList.add(fileInformation);
	}
}