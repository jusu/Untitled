package com.pinktwins.elephant.data;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class Tag implements Comparable<Tag> {

	private static final Logger LOG = Logger.getLogger(Tag.class.getName());

	final String name;
	final String id;
	final String parentId;

	public Tag(String name) {
		id = Long.toString(System.currentTimeMillis(), 36) + "_" + (long) (Math.random() * 1000.0f);
		this.name = name;
		parentId = "";
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}

	@Override
	public int compareTo(Tag t) {
		return name().toLowerCase().compareTo(t.name().toLowerCase());
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public String name() {
		return name;
	}

	public String id() {
		return id;
	}

	public Tag(JSONObject o) {
		id = o.optString("id");

		String _name = o.optString("name");
		try {
			_name = URLDecoder.decode(_name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.severe("Fail: " + e);
		}

		name = _name;
		parentId = o.optString("parentId");
	}

	public Object toJSON() {
		JSONObject o = new JSONObject();
		try {
			o.put("id", id);
			try {
				o.put("name", URLEncoder.encode(name, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				LOG.severe("Fail: " + e);
				o.put("name", name);
			}
			o.put("parentId", parentId);
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		}
		return o;
	}
}
