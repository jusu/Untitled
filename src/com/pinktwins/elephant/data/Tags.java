package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Tags {
	private ArrayList<Tag> flatList = Factory.newArrayList();

	private String fileLoaded;

	public void reload(String path) {

		JSONObject o = IOUtil.loadJson(new File(path));

		if (o.has("tags")) {
			try {
				JSONArray arr = o.getJSONArray("tags");
				for (int n = 0, len = arr.length(); n < len; n++) {
					JSONObject t = arr.getJSONObject(n);
					Tag tag = new Tag(t);
					flatList.add(tag);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Loaded " + flatList.size() + " tags.");
		fileLoaded = path;
	}

	public void save() {
		if (fileLoaded == null) {
			throw new IllegalStateException();
		}

		JSONArray arr = new JSONArray();
		for (Tag t : flatList) {
			arr.put(t.toJSON());
		}

		JSONObject o = new JSONObject();
		try {
			o.put("tags", arr);
			IOUtil.writeFile(new File(fileLoaded), o.toString(4));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class Tag {
	String id;
	String name;
	String parentId;

	public Tag(JSONObject o) {
		id = o.optString("id");
		name = o.optString("name");
		parentId = o.optString("parentId");
	}

	public Object toJSON() {
		JSONObject o = new JSONObject();
		try {
			o.put("id", id);
			o.put("name", name);
			o.put("parentId", parentId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return o;
	}
}
