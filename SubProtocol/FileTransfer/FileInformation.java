package hlmp.SubProtocol.FileTransfer;

import java.util.UUID;


public class FileInformation {

	private String name;
	private String path;
	private long size;
	private UUID id;


	public FileInformation() {
		this.id = UUID.randomUUID();
	}

	public FileInformation(String name, long size, String path) {
		this();
		this.name = name;
		this.size = size;
		this.path = path;
	}

	public FileInformation(String name, long size, UUID id) {
		this.name = name;
		this.size = size;
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return this.size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID id) {
		this.id = id;
	}
}