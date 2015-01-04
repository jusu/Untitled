package com.pinktwins.elephant.data;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

public class Tag implements Comparable<Tag> {

	String name;
	String id;
	String parentId;

	public Tag(String name) {
		id = Long.toString(System.currentTimeMillis(), 36) + "_" + (long) (Math.random() * 1000.0f);
		this.name = name;
		parentId = "";
	}

	@Override
	public int compareTo(Tag t) {
		return name().toLowerCase().compareTo(t.name().toLowerCase());
	}

	public String name() {
		return name;
	}
	
	public String id() {
		return id;
	}

	public Tag(JSONObject o) {
		id = o.optString("id");
		name = o.optString("name");
		try {
			name = URLDecoder.decode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		parentId = o.optString("parentId");
	}

	public Object toJSON() {
		JSONObject o = new JSONObject();
		try {
			o.put("id", id);
			try {
				o.put("name", URLEncoder.encode(name, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				o.put("name", name);
			}
			o.put("parentId", parentId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return o;
	}
}
