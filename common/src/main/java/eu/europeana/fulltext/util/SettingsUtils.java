package eu.europeana.fulltext.util;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class SettingsUtils {


  public static void validateValues(Map<String, String> map, List<String> missingProps) {
    for (Map.Entry<String,  String> entry : map.entrySet()) {
      if (StringUtils.isEmpty(entry.getKey())) {
        missingProps.add(entry.getValue());
      }
    }
  }
}
