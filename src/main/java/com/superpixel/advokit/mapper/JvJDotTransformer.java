package com.superpixel.advokit.mapper;

import static com.superpixel.jdot.util.ScalaConverters.*;

import java.util.List;

import com.superpixel.jdot.JDotTransformer;


public class JvJDotTransformer {

	private JDotTransformer scTransformer;

	public JvJDotTransformer(JDotTransformer scTransformer) {
		this.scTransformer = scTransformer;
	}

	public JDotTransformer getScTransformer() {
		return scTransformer;
	}

	public String transform(String json) {
		return scTransformer.transform(json,
				scTransformer.transform$default$2(),
				scTransformer.transform$default$3(),
				scTransformer.transform$default$4());
	}

	public String transformList(List<String> jsonList) {
		return scTransformer.transformList(jvToScList(jsonList),
				scTransformer.transform$default$2(),
				scTransformer.transform$default$3(),
				scTransformer.transform$default$4());
	}


	public String transform(String json, JvJDotSettings settings) {
		return scTransformer.transform(json,
				settings.attachments,
				settings.mergingJson,
				settings.inclusions);
	}
	
	public String transformList(List<String> jsonList, JvJDotSettings settings) {
		return scTransformer.transformList(jvToScList(jsonList),
				settings.attachments,
				settings.mergingJson,
				settings.inclusions);
	}
}
