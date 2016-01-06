package com.superpixel.advokit.mapper;

import com.superpixel.advokit.json.lift.JValueMerger;

public class JvContentMerger {

	public String leftMergeWithArraysAsValues(String leftJson, String rightJson) {
		return JValueMerger.leftMergeStrings(JValueMerger.mergeArraysAsValues(), leftJson, rightJson);
	}

	public String leftMergeWithArraysOnIndex(String leftJson, String rightJson) {
		return JValueMerger.leftMergeStrings(JValueMerger.mergeArraysOnIndex(), leftJson, rightJson);
	}
}
