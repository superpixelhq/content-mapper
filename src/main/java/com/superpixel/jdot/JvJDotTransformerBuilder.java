package com.superpixel.jdot;

import java.util.Map;
import java.util.Optional;

import com.superpixel.jdot.FixedInclusions;
import com.superpixel.jdot.Inclusions;
import com.superpixel.jdot.JDotTransformer;
import com.superpixel.jdot.JDotTransformer$;
import com.superpixel.jdot.MergingJson;
import com.superpixel.jdot.MergingJsonPost;
import com.superpixel.jdot.MergingJsonPre;
import com.superpixel.jdot.MergingJsonPrePost;
import com.superpixel.jdot.json4s.JValueTransformer;
import com.superpixel.jdot.pathing.JPathPair;

import static com.superpixel.jdot.util.ScalaConverters.*;

public class JvJDotTransformerBuilder {

  private Optional<Map<String, String>> inclusionsMap = Optional.empty();
  
  private Optional<String[]> preMergingJson = Optional.empty();
  private Optional<String[]> postMergingJson = Optional.empty();
  
  
  /***
   * Format Destination -> Source.
   * Destinations paths are keys with source paths as values.
   */
  private Map<String, String> pathMapping;
  
  /***
   * Declares the paths for the json transformation.
   * Format is Destination -> Source.
   * Destinations paths are keys with source paths as values.
   * 
   * For example, if the target class has a String field called 'name'.
   * Then mapping:
   *  'name' -> 'person.forename'
   * Will assign the 'name' attribute with the value in 'forename' field in the 'person' object in the json.
   * 
   * @param pathMapping
   * @return
   */
  public JvJDotTransformerBuilder withPathMapping(Map<String, String> pathMapping) {
    this.pathMapping = pathMapping;
    return this;
  }
  
  
  /***
   * Inclusions can be used by specifying a link in the path.
   * The path 'person.id>.friendCount' will get the 'id' from the 'person' object in the json.
   * It will look for this 'id' in the inclusions map, and then use the resulting json value for the rest of the path.
   * 
   * @param inclusionsMap
   * @return
   */
  public JvJDotTransformerBuilder withInclusionsMap(Map<String, String> inclusionsMap) {
    this.inclusionsMap = Optional.of(inclusionsMap);
    return this;
  }
  
  /***
   * This json is merged with the passed in json BEFORE each transformation.
   * The passed in json is favoured, any missed fields are taken from the default
   * (i.e. this json should be of the same format as those passed IN for transformation)
   * @param defaultInJson
   * @return
   */
  public JvJDotTransformerBuilder withPreJsonMerging(String... preMergingJson) {
    this.preMergingJson = Optional.of(preMergingJson);
    return this;
  }
  
  /***
   * This json is merged with the resulting json AFTER each transformation.
   * The result json is favoured, any missed fields are taken from the default
   * (i.e. this json should be of the same format as the json that comes OUT of the transformation)
   * @param defaultOutJson
   * @return
   */
  public JvJDotTransformerBuilder withPostJsonMerging(String... postMergingJson) {
    this.postMergingJson = Optional.of(postMergingJson);
    return this;
  }
  
  /***
   * Extracts from the settings the inclusions, preJsonMerging and postMergingJson.
   * Ignore any attachment settings.
   * 
   * 
   * 
   * @param settings
   * @return
   */
  public JvJDotTransformer buildWithSettings(JvJDotSettings settings) {
	scala.collection.immutable.Set<JPathPair> scPathMapping = jvStringMapToJPathPairSet(pathMapping);
	    
    Inclusions scIncMap;
    if (inclusionsMap.isPresent()) {
      scIncMap = MappingParameters.combineInclusions(new FixedInclusions(jvToScMap(inclusionsMap.get())), settings.inclusions);
    } else {
      scIncMap = settings.inclusions;
    }
    MergingJson scMergJson;
    if (preMergingJson.isPresent() && postMergingJson.isPresent()) {
      scMergJson = MappingParameters.combineMergingJson(new MergingJsonPrePost(jvArrayToScSeq(preMergingJson.get()), jvArrayToScSeq(postMergingJson.get())), settings.mergingJson);
    } else if (preMergingJson.isPresent()) {
      scMergJson = MappingParameters.combineMergingJson(new MergingJsonPre(jvArrayToScSeq(preMergingJson.get())), settings.mergingJson);
    } else if (postMergingJson.isPresent()) {
      scMergJson = MappingParameters.combineMergingJson(new MergingJsonPost(jvArrayToScSeq(postMergingJson.get())), settings.mergingJson);
    } else {
      scMergJson = settings.mergingJson;
    }
    
    JDotTransformer scTransformer = JDotTransformer$.MODULE$.apply(scPathMapping, scMergJson, scIncMap);
    return new JvJDotTransformer(scTransformer);
  }
  
  public JvJDotTransformer build() {

    scala.collection.immutable.Set<JPathPair> scPathMapping = jvStringMapToJPathPairSet(pathMapping);
    
    Inclusions scIncMap;
    if (inclusionsMap.isPresent()) {
      scIncMap = new FixedInclusions(jvToScMap(inclusionsMap.get()));
    } else {
      scIncMap = JValueTransformer.apply$default$3();
    }
    MergingJson scMergJson;
    if (preMergingJson.isPresent() && postMergingJson.isPresent()) {
      scMergJson = new MergingJsonPrePost(jvArrayToScSeq(preMergingJson.get()), jvArrayToScSeq(postMergingJson.get()));
    } else if (preMergingJson.isPresent()) {
      scMergJson = new MergingJsonPre(jvArrayToScSeq(preMergingJson.get()));
    } else if (postMergingJson.isPresent()) {
      scMergJson = new MergingJsonPost(jvArrayToScSeq(postMergingJson.get()));
    } else {
      scMergJson = JValueTransformer.apply$default$2();
    }
    
    JDotTransformer scTransformer = JDotTransformer$.MODULE$.apply(scPathMapping, scMergJson, scIncMap);
    return new JvJDotTransformer(scTransformer);
  }
}
