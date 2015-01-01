package com.pinktwins.elephant.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.IOUtil;

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

	public List<String> resolveNames(List<String> tagNames) {
		ArrayList<String> ids = Factory.newArrayList();

		boolean newTags = false;

		for (final String tagName : tagNames) {
			Collection<Tag> t = CollectionUtils.select(flatList, new Predicate<Tag>() {
				@Override
				public boolean evaluate(Tag t) {
					return tagName.equals(t.name);
				}
			});

			if (t.isEmpty()) {
				Tag tag = new Tag(tagName);
				flatList.add(tag);
				newTags = true;
				ids.add(tag.id);
			} else {
				if (t.size() > 1) {
					// XXX more than one tag found by name. Ask user which tag
					// to use.
				}
				// XXX now using first found tag, resolve if multiple found
				for (Tag tag : t) {
					ids.add(tag.id);
					break;
				}
			}
		}

		if (newTags) {
			save();
		}

		return ids;
	}

	public List<String> resolveIds(List<String> tagIds) {
		ArrayList<String> names = Factory.newArrayList();

		for (Tag t : flatList) {
			if (tagIds.contains(t.id)) {
				names.add(t.name());
			}
		}

		return names;
	}

	public List<Tag> asList() {
		return flatList;
	}
}
