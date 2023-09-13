package com.milky.trackerWeb.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "ebay")
@TypeAlias("")
public class Competitor {
	@Id
	private ObjectId _id;
	@Field("source_name")
	@Indexed(unique = true)
	private String sourcename;
	@Field("converted_url")
	private String convertedUrl;
	@Field("url")
	private String url;
	@Field("status")
	private Status status;
	
	public enum Status {
	    FAILED,
	    SUCCESS
	}

	public ObjectId get_id() {
		return _id;
	}

	public void set_id(ObjectId _id) {
		this._id = _id;
	}

	public String getSourcename() {
		return sourcename;
	}

	public void setSourcename(String sourcename) {
		this.sourcename = sourcename;
	}

	public String getConvertedUrl() {
		return convertedUrl;
	}

	public void setConvertedUrl(String convertedUrl) {
		this.convertedUrl = convertedUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

}
